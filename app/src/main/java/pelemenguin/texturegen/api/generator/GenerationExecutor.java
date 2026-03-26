package pelemenguin.texturegen.api.generator;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import pelemenguin.texturegen.api.client.ProgressReporter;
import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.PlainTextProgressReporter;
import pelemenguin.texturegen.api.client.terminal.TerminalProgressReporter;

public class GenerationExecutor {

    public static record ExecutionResult(
        int totalImages,
        int successfulImages,
        int failedImages,
        int unfoundImages,
        int skippedImages,
        List<GenerationError> exceptions
    ) {}

    public static ExecutionResult run(Path assetsFolder, Path outputFolder, List<TextureInfo> textureInfos, GeneratorInfo generatorInfo) {
        return run(assetsFolder, outputFolder, textureInfos, generatorInfo, null);
    }

    public static ExecutionResult run(Path assetsFolder, Path outputFolder, List<TextureInfo> textureInfos, GeneratorInfo generatorInfo, PrintStream out) {
        List<String> fallbacks = generatorInfo.fallbacks;
        List<String> types = generatorInfo.types;
        List<Processor> processors = generatorInfo.processors;
        String resultSuffix = generatorInfo.suffix;

        List<GenerationError> exceptions = Collections.synchronizedList(new ArrayList<>());

        int totalImages = textureInfos.size();

        // Initialization
        for (Processor processor : processors) {
            try {
                processor.processorInit();
            } catch (Throwable t) {
                if (out != null) {
                    out.println(ANSIHelper.blue("Processor initialization failed!"));
                }
                exceptions.add(new GenerationError(null, new GeneratorException("Initialization failed for processor " + processor + ": " + t.getMessage(), t)));
                return new ExecutionResult(
                    totalImages,
                    0,
                    0,
                    0,
                    totalImages,
                    exceptions
                );
            }
        }

        // Type check
        try {
            typeCheck(processors);
        } catch (Throwable t) {
            exceptions.add(new GenerationError(null, new GeneratorException("Processor type check failed: " + t.getMessage(), t)));
            if (out != null) {
                out.println(ANSIHelper.red("Type check failed!"));
            }
            return new ExecutionResult(
                totalImages,
                0,
                0,
                0,
                totalImages,
                exceptions
            );
        }

        final ProgressReporter reporter;
        if (out != null) {
            if (ANSIHelper.ansiEnabled()) {
                ANSIHelper.clear(out);
                TerminalProgressReporter terminalProgressReporter = new TerminalProgressReporter(out);
                terminalProgressReporter.registerCategory("Succeeded", i -> ANSIHelper.green(String.valueOf(i)), '#');
                terminalProgressReporter.registerCategory("Failed", i -> ANSIHelper.red(String.valueOf(i)), '!');
                terminalProgressReporter.registerCategory("Unfound", i -> ANSIHelper.yellow(String.valueOf(i)), '?');
                terminalProgressReporter.registerCategory("Skipped", i -> ANSIHelper.blue(String.valueOf(i)), '/');
                terminalProgressReporter.updateTotal(totalImages);

                terminalProgressReporter.loop(out);
                reporter = terminalProgressReporter;
            } else {
                PlainTextProgressReporter plainTextProgressReporter = new PlainTextProgressReporter();
                plainTextProgressReporter.registerCategory("Succeeded");
                plainTextProgressReporter.registerCategory("Failed");
                plainTextProgressReporter.registerCategory("Unfound");
                plainTextProgressReporter.registerCategory("Skipped");

                plainTextProgressReporter.updateTotal(totalImages);
                plainTextProgressReporter.loop(out);

                reporter = plainTextProgressReporter;
            }
        } else {
            // TODO: maybe we should change to another reporter after window client is implemented
            reporter = ProgressReporter.newNoOutputReporter();
        }

        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

        for (TextureInfo textureInfo : textureInfos) {
            if (!textureInfo.supportTypes(types)) {
                reporter.increase("Skipped");
                continue;
            }

            service.submit(() -> {
                Path path = textureInfo.path;

                Path fallbackPath = null;
                String fallbackUsed = null;
                File imageFile = null;
                for (String fallback : fallbacks) {
                    fallbackPath = assetsFolder.resolve(path.resolveSibling(path.getFileName().toString().replaceFirst("(\\.\\w+)$", "_" + fallback + "$1")));
                    try {
                        File currentFile = fallbackPath.toFile();
                        if (currentFile.exists()) {
                            fallbackUsed = fallback;
                            imageFile = currentFile;
                            break;
                        }
                    } catch (Exception e) {
                        exceptions.add(new GenerationError(textureInfo, new GeneratorException("Exception occured while trying fallbacks. current fallback path: " + fallbackPath, e)));
                    }
                }
                if (fallbackUsed == null) {
                    try {
                        File currentFile = assetsFolder.resolve(path).toFile();
                        if (currentFile.exists()) {
                            fallbackPath = path;
                            imageFile = currentFile;
                        } else {
                            fallbackPath = null;
                        }
                    } catch (Exception e) {
                        fallbackPath = null;
                    }
                }

                if (fallbackPath == null) {
                    exceptions.add(new GenerationError(textureInfo, new FileNotFoundException("Could not find texture at " + path + " or any of the fallbacks")));
                    reporter.increase("Unfound");
                    return;
                }

                GenerationContext context = new GenerationContext(
                    path,
                    fallbackPath,
                    fallbackUsed
                );

                File resultFile = null;
                try {
                    if (!imageFile.canRead()) {
                        throw new IOException("Cannot read file: " + imageFile);
                    }
                    BufferedImage image = ImageIO.read(imageFile);
                    image = toARGB(image);

                    BufferedImage result = runSingle(image, context, processors);
                    Path resultPath = outputFolder.resolve(path.resolveSibling(
                        path.getFileName().toString().replaceFirst("(\\.\\w+)$", resultSuffix == null ? "$1" : ("_" + resultSuffix + "$1"))
                    ));
                    resultFile = resultPath.toFile();

                    if (!resultFile.exists()) {
                        File parentFile = resultFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        resultFile.createNewFile();
                    }

                    ImageIO.write(result, "png", resultFile);
                } catch (Throwable e) {
                    exceptions.add(new GenerationError(textureInfo, new GeneratorException("Exception while processing " + imageFile + ", with result file " + resultFile, e)));
                    reporter.increase("Failed");
                    return;
                }

                reporter.increase("Succeeded");
            });
        }

        boolean hasFinializationExceptions = false;
        for (Processor processor : processors) {
            try {
                processor.processorFinalize();
            } catch (Throwable t) {
                exceptions.add(new GenerationError(null, new GeneratorException("Finalization failed for processor " + processor, t)));
                hasFinializationExceptions = true;
            }
        }

        try {
            service.shutdown();
            service.awaitTermination(1, TimeUnit.HOURS);
            reporter.shutdown();

            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int successfulImages = reporter.getData("Succeeded");
        int failedImages = reporter.getData("Failed");
        int unfoundImages = reporter.getData("Unfound");
        int skippedImages = reporter.getData("Skipped");

        if (out != null) {
            ANSIHelper.clear(out);
            out.println(ANSIHelper.blue("Processing complete!"));
            out.println("Succeeded | " + ANSIHelper.green(String.valueOf(successfulImages)));
            out.println("Failed    | " + ANSIHelper.red(String.valueOf(failedImages)));
            out.println("Unfound   | " + ANSIHelper.yellow(String.valueOf(unfoundImages)));
            out.println("Skipped   | " + ANSIHelper.blue(String.valueOf(skippedImages)));

            if (hasFinializationExceptions) {
                out.println();
                out.println(ANSIHelper.yellow("WARNING: There were exceptions thrown during processor finalization!"));
            }
        }

        return new ExecutionResult(
            totalImages,
            successfulImages,
            failedImages,
            unfoundImages,
            skippedImages,
            exceptions
        );
    }

    private static BufferedImage toARGB(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage converted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // FIXME: too few types
        switch (image.getType()) {
            case BufferedImage.TYPE_CUSTOM: {
                Raster originalData = image.getRaster();
                Raster alphaData = image.getAlphaRaster();
                WritableRaster newData = converted.getRaster();
                int[] pixel = new int[originalData.getNumBands()];
                int[] result = new int[4];
                if (pixel.length == 4) {
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            originalData.getPixel(x, y, pixel);
                            result[0] = pixel[0];
                            result[1] = pixel[1];
                            result[2] = pixel[2];
                            result[3] = alphaData == null ? 255 : alphaData.getSample(x, y, 0);
                            newData.setPixel(x, y, result);
                        }
                    }
                } else {
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            originalData.getPixel(x, y, pixel);
                            int grey = pixel[0];
                            result[0] = grey;
                            result[1] = grey;
                            result[2] = grey;
                            result[3] = alphaData == null ? 255 : alphaData.getSample(x, y, 0);
                            newData.setPixel(x, y, result);
                        }
                    }
                }
                break;
            }
            case BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE: {
                converted.getGraphics().drawImage(image, 0, 0, null);
                break;
            }
            case BufferedImage.TYPE_3BYTE_BGR: {
                WritableRaster originalData = image.getRaster();
                WritableRaster newData = converted.getRaster();
                int[] pixel = new int[3];
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        originalData.getPixel(x, y, pixel);
                        // BGR to ARGB
                        int b = pixel[0];
                        int g = pixel[1];
                        int r = pixel[2];
                        newData.setPixel(x, y, new int[] {r, g, b, 255});
                    }
                }
                break;
            }
            case BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_4BYTE_ABGR_PRE: {
                WritableRaster originalData = image.getRaster();
                WritableRaster newData = converted.getRaster();
                int[] pixel = new int[4];
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        originalData.getPixel(x, y, pixel);
                        // ABGR to ARGB
                        int b = pixel[0];
                        int g = pixel[1];
                        int r = pixel[2];
                        int a = pixel[3];
                        newData.setPixel(x, y, new int[] {r, g, b, a});
                    }
                }
                break;
            }
            case BufferedImage.TYPE_BYTE_GRAY: {
                WritableRaster originalData = image.getRaster();
                WritableRaster originalAlpha = image.getAlphaRaster();
                WritableRaster newData = converted.getRaster();
                int[] newBands = new int[] {0, 0, 0, 0};
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int grey = originalData.getSample(x, y, 0);
                        newBands[0] = grey;
                        newBands[1] = grey;
                        newBands[2] = grey;
                        newBands[3] = originalAlpha == null ? 255 : originalAlpha.getSample(x, y, 0);
                        newData.setPixel(x, y, newBands);
                    }
                }
                break;
            }
            case BufferedImage.TYPE_BYTE_INDEXED: {
                assert image.getColorModel() instanceof IndexColorModel;
                IndexColorModel colorModel = (IndexColorModel) image.getColorModel();

                WritableRaster originalData = image.getRaster();
                
                int[] indices = originalData.getPixels(0, 0, width, height, (int[]) null);
                int[] newPixels = new int[indices.length * 4];
                for (int i = 0; i < indices.length; i++) {
                    int color = colorModel.getRGB(indices[i]);
                    newPixels[i * 4] = (color >> 16) & 0xFF;
                    newPixels[i * 4 + 1] = (color >> 8) & 0xFF;
                    newPixels[i * 4 + 2] = color & 0xFF;
                    newPixels[i * 4 + 3] = (color >> 24) & 0xFF;
                }
                converted.getRaster().setPixels(0, 0, width, height, newPixels);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported image type: " + image.getType());
            }
        }

        return converted;
    }

    public static BufferedImage runSingle(BufferedImage image, GenerationContext context, List<Processor> processors) throws IllegalStateException {
        if (processors.isEmpty()) return image;

        ArrayDeque<Object> stack = new ArrayDeque<>();
        ArrayDeque<Class<?>> types = new ArrayDeque<>();

        // Push input image
        stack.addFirst(image);
        types.addFirst(BufferedImage.class);

        // Iterate through processors and process
        for (Processor processor : processors) {
            List<Class<?>> typesView = List.copyOf(types);
            List<Class<?>> inputTypes = processor.getInputTypes(typesView);
            List<Class<?>> outputTypes = processor.getOutputTypes(typesView);

            if (types.size() < inputTypes.size()) {
                throw new IllegalStateException("Not enough parameters for processor " + processor
                    + "\nCurrent stack: " + types + "\nRequired types" + inputTypes);
            }

            Object[] inputs = new Object[inputTypes.size()];
            for (int i = inputs.length - 1; i >= 0; i--) {
                Class<?> expectedType = inputTypes.get(i);
                Object actualObject = stack.pollFirst();
                Class<?> actualType = types.pollFirst();

                if (!expectedType.isAssignableFrom(actualType)) {
                    throw new IllegalStateException("Expected type " + expectedType.getName() + " but got " + actualType.getName() + " for processor " + processor.getClass().getName());
                }

                inputs[i] = actualObject;
            }

            Parameter parameter = new Parameter(inputs);
            Result result = new Result(outputTypes.toArray(new Class<?>[0]));
            try {
                processor.process(context, parameter, result);
            } catch (Exception e) {
                throw new IllegalStateException("Processor " + processor.getProcessorTitle() + " threw an exception: " + e.getMessage(), e);
            }

            // Push output types and objects
            for (int i = 0; i < outputTypes.size(); i++) {
                stack.addFirst(result.data()[i]);
                types.addFirst(outputTypes.get(i));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one output after processing, but got " + stack.size());
        }
        Object finalResult = stack.pollFirst();
        if (finalResult instanceof BufferedImage r) {
            return r;
        } else {
            throw new IllegalStateException("Expected final output to be a BufferedImage, but got " + finalResult.getClass().getName());
        }
    }

    private static void typeCheck(List<Processor> processors) {
        ArrayDeque<Class<?>> types = new ArrayDeque<>();
        types.addFirst(BufferedImage.class);

        for (Processor processor : processors) {
            List<Class<?>> typesView = List.copyOf(types);
            List<Class<?>> inputTypes = processor.getInputTypes(typesView);
            List<Class<?>> outputTypes = processor.getOutputTypes(typesView);

            if (types.size() < inputTypes.size()) {
                throw new IllegalArgumentException("Not enough parameters for processor " + processor
                    + "\nCurrent stack: " + types + "\nRequired types" + inputTypes);
            }

            for (int i = 0; i < inputTypes.size(); i++) {
                Class<?> expectedType = inputTypes.get(i);
                Class<?> actualType = types.pollFirst();

                if (!expectedType.isAssignableFrom(actualType)) {
                    throw new IllegalArgumentException("Expected type " + expectedType.getName() + " but got " + actualType.getName() + " for processor " + processor.getClass().getName());
                }
            }

            // Push output types
            for (Class<?> outputType : outputTypes) {
                types.addFirst(outputType);
            }
        }

        // Finally, there should be exactly one BufferedImage on the stack
        if (types.size() != 1 || !BufferedImage.class.isAssignableFrom(types.peekFirst())) {
            throw new IllegalStateException("Expected exactly one output of type BufferedImage after processing, but got " + types);
        }
    }

    public static record Parameter(Object[] data) {
        public <T> T load(int index, Class<T> type) throws IllegalArgumentException {
            try {
                return type.cast(data[index]);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Parameter at index " + index + " is not of type " + type.getName() + ". Parameters: " + Arrays.toString(data));
            }
        }
    }

    public static record GenerationError(TextureInfo textureInfo, Exception exception) {
        public void printStackTrace(PrintStream out) {
            out.println("Generating: " + textureInfo);
            this.exception.printStackTrace(out);
        }
    }

    public static record Result(Class<?>[] types, Object[] data) {
        public Result(Class<?>[] types) {
            this(types, new Object[types.length]);
        }
        public <T> void push(int index, T object) throws IllegalArgumentException {
            try {
                data[index] = types[index].cast(object);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Result at index " + index + " is not of type " + types[index].getName());
            }
        }
    }

    public static class GeneratorException extends Exception {
        public GeneratorException(String message) {
            super(message);
        }

        public GeneratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
