package pelemenguin.texturegen.api.generator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import pelemenguin.texturegen.client.terminal.ANSIHelper;
import pelemenguin.texturegen.client.terminal.TerminalProgressReporter;

public class GenerationExecutor {

    public static record ExecutionResult(
        int totalImages,
        int successfulImages,
        int failedImages,
        int unfoundImages,
        List<GenerationError> exceptions
    ) {}

    public static ExecutionResult run(Path assetsFolder, Path outputFolder, List<TextureInfo> textureInfos, GeneratorInfo generatorInfo) {
        return run(assetsFolder, outputFolder, textureInfos, generatorInfo, null);
    }

    public static ExecutionResult run(Path assetsFolder, Path outputFolder, List<TextureInfo> textureInfos, GeneratorInfo generatorInfo, PrintStream out) {
        String[] fallbacks = generatorInfo.fallbacks;
        Processor[] processors = generatorInfo.processors;
        String resultSuffix = generatorInfo.suffix;

        List<GenerationError> exceptions = Collections.synchronizedList(new ArrayList<>());

        int totalImages = textureInfos.size();

        final TerminalProgressReporter reporter;
        if (out != null && ANSIHelper.ansiEnabled()) {
            ANSIHelper.clear(out);
            reporter = new TerminalProgressReporter();
            reporter.registerCategory("Succeeded", i -> ANSIHelper.green(String.valueOf(i)), '#');
            reporter.registerCategory("Failed", i -> ANSIHelper.red(String.valueOf(i)), '!');
            reporter.registerCategory("Unfound", i -> ANSIHelper.yellow(String.valueOf(i)), '?');
            reporter.updateTotal(totalImages);

            reporter.loop(out);
        } else {
            reporter = null;
        }

        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

        for (TextureInfo textureInfo : textureInfos) {
            service.submit(() -> {
                Path path = textureInfo.path;

                Path fallbackPath = null;
                String fallbackUsed = null;
                BufferedImage image = null;
                for (String fallback : fallbacks) {
                    fallbackPath = assetsFolder.resolve(path.resolveSibling(path.getFileName().toString().replaceFirst("(\\.\\w+)$", "_" + fallback + "$1")));
                    try {
                        File currentFile = fallbackPath.toFile();
                        if (currentFile.exists()) {
                            image = ImageIO.read(currentFile);
                            fallbackUsed = fallback;
                            break;
                        }
                    } catch (Exception e) {
                        exceptions.add(new GenerationError(textureInfo, e));
                    }
                }
                if (image == null) {
                    try {
                        File currentFile = assetsFolder.resolve(path).toFile();
                        if (currentFile.exists()) {
                            image = ImageIO.read(currentFile);
                            fallbackUsed = null;
                            fallbackPath = path;
                        }
                    } catch (Exception e) {
                        exceptions.add(new GenerationError(textureInfo, e));
                    }
                }

                if (image == null) {
                    exceptions.add(new GenerationError(textureInfo, new FileNotFoundException("Could not find texture at " + path + " or any of the fallbacks")));
                    reporter.increase("Unfound");
                    return;
                }

                GenerationContext context = new GenerationContext(
                    path,
                    fallbackPath,
                    fallbackUsed
                );

                try {
                    BufferedImage result = runSingle(image, context, List.of(processors));
                    Path resultPath = outputFolder.resolve(path.resolveSibling(
                        path.getFileName().toString().replaceFirst("(\\.\\w+)$", resultSuffix == null ? "$1" : ("_" + resultSuffix + "$1"))
                    ));
                    File resultFile = resultPath.toFile();

                    if (!resultFile.exists()) {
                        File parentFile = resultFile.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        resultFile.createNewFile();
                    }

                    ImageIO.write(result, "png", resultFile);
                } catch (Exception e) {
                    exceptions.add(new GenerationError(textureInfo, e));
                    reporter.increase("Failed");
                }

                reporter.increase("Succeeded");
            });
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

        if (out != null && ANSIHelper.ansiEnabled()) {
            ANSIHelper.clear(out);
            out.println(ANSIHelper.blue("Processing complete!"));
            out.println("Succeeded | " + ANSIHelper.green(String.valueOf(successfulImages)));
            out.println("Failed    | " + ANSIHelper.red(String.valueOf(failedImages)));
            out.println("Unfound   | " + ANSIHelper.yellow(String.valueOf(unfoundImages)));
        }

        return new ExecutionResult(
            totalImages,
            successfulImages,
            failedImages,
            unfoundImages,
            exceptions
        );
    }

    public static BufferedImage runSingle(BufferedImage image, GenerationContext context, List<Processor> processors) throws IllegalArgumentException, IllegalStateException {
        ArrayDeque<List<?>> stack = new ArrayDeque<>();
        ArrayDeque<Class<?>> types = new ArrayDeque<>();

        // Push input image
        stack.add(List.of(image));
        types.add(BufferedImage.class);

        // Type check
        for (Processor processor : processors) {
            List<Class<?>> inputTypes = processor.getInputTypes();
            List<Class<?>> outputTypes = processor.getOutputTypes();

            if (stack.size() < inputTypes.size()) {
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
                stack.add(new ArrayList<>());
                types.add(outputType);
            }
        }

        // Iterate processors and process
        for (Processor processor : processors) {
            List<Class<?>> inputTypes = processor.getInputTypes();
            List<Class<?>> outputTypes = processor.getOutputTypes();

            // Run with parameters iter through each Cartesian product of argument lists
            Parameter parameter = new Parameter(new Object[inputTypes.size()]);
            List<?>[] inputLists = new List[inputTypes.size()];
            for (int i = inputLists.length - 1; i > 0; i--) {
                inputLists[i] = stack.pop();
            }
            // Implement loop equivalent to Cartesian product inline
            int[] indices = new int[inputLists.length];
            for (List<?> list : inputLists) {
                if (list.isEmpty()) {
                    continue;
                }
            }
            Result result = new Result(outputTypes.toArray(new Class[0]));
            while (true) {
                processor.process(context, parameter, result);
                int pos = indices.length - 1;
                while (pos >= 0) {
                    indices[pos] ++;
                    if (indices[pos] < inputLists[pos].size()) {
                        parameter.updateData(pos, inputLists[pos].get(indices[pos]));
                        break;
                    } else {
                        indices[pos] = 0;
                        parameter.updateData(pos, inputLists[pos].get(0));
                        pos --;
                    }
                }
                if (pos < 0) {
                    break;
                }
            }

            // Push results to stack
            for (int i = outputTypes.size() - 1; i >= 0; i--) {
                stack.add(result.data[i]);
                types.add(result.types[i]);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one output but got " + stack.size());
        }
        if (!BufferedImage.class.isAssignableFrom(types.peek())) {
            throw new IllegalStateException("Expected output type " + BufferedImage.class.getName() + " but got " + types.peek().getName());
        }
        List<?> finalResult = stack.pop();
        if (finalResult.size() != 1) {
            throw new IllegalStateException("Expected exactly one output but got " + finalResult.size());
        }
        return (BufferedImage) finalResult.get(0);
    }

    public static record Parameter(Object[] data) {
        public <T> T load(int index, Class<T> type) throws IllegalArgumentException {
            try {
                return type.cast(data[index]);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Parameter at index " + index + " is not of type " + type.getName());
            }
        }
        private void updateData(int index, Object data) {
            this.data[index] = data;
        }
    }

    public static record GenerationError(TextureInfo textureInfo, Exception exception) {
        public void printStackTrace(PrintStream out) {
            out.println("Generating: " + textureInfo);
            this.exception.printStackTrace(out);
        }
    }

    public static record Result(Class<?>[] types, List<Object>[] data) {
        public Result(Class<?>[] types) {
            this(types, new List[types.length]);
            for (int i = 0; i < types.length; i++) {
                data[i] = new ArrayList<>();
            }
        }
        public <T> void push(int index, T object) throws IllegalArgumentException {
            if (object == null) return;
            try {
                if (object == data[data.length - 1]) return;
                data[index].add(types[index].cast(object));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Result at index " + index + " is not of type " + types[index].getName());
            }
        }
    }

}
