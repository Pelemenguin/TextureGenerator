package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public class ImageSplitter implements Processor {

    public PointFilter filter = PointFilter.alwaysPass();
    @SerializedName("put_image_for_passed_on_top")
    public boolean putImageForPassedOnTop = true;
    @SerializedName("keep_passed_in_original")
    public boolean keepPassedInOriginal = true;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage original = parameters.load(0, BufferedImage.class);
        BufferedImage biProduct = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        BufferedImage maskImage = filter.filter(context, original);

        Raster maskRaster = maskImage.getRaster();
        for (int x = 0; x < maskImage.getWidth(); x++) {
            for (int y = 0; y < maskImage.getHeight(); y++) {
                boolean succeeded = maskRaster.getSample(x, y, 0) > 0;
                if (keepPassedInOriginal ^ succeeded) {
                    biProduct.setRGB(x, y, original.getRGB(x, y));
                    original.setRGB(x, y, 0);
                }
            }
        }

        if (putImageForPassedOnTop ^ keepPassedInOriginal) {
            result.push(0, original);
            result.push(1, biProduct);
        } else {
            result.push(0, biProduct);
            result.push(1, original);
        }
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.splitter", ImageSplitter.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of(BufferedImage.class, BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Image Splitter";
    }

    @Override
    public String getProcessorTitle() {
        return "Split Image by " + filter.getPointFilterTitle();
    }

    @Override
    public void processorInit() {
        this.filter.pointFilterInit();
    }

    @Override
    public void processorFinalize() {
        this.filter.pointFilterFinalize();
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<ImageSplitter> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.splitter", this);
        }

        @Override
        public void editorLoop(ImageSplitter processor, Consumer<ImageSplitter> setter, TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu(processor.getProcessorName()).autoUppercase();
            menu.addKey('-', "Back");
            menu.addKey('F', "", () -> {
                TerminalPointFilterEditorProvider.getEditorLoop(processor.filter, f -> processor.filter = f, context).run();
            });
            menu.addKey('G', "Replace Point Filter", () -> {
                TerminalPointFilterEditorProvider.getSelectionList(f -> processor.filter = f, context).run();
            });
            menu.addKey('T', "", () -> processor.putImageForPassedOnTop = !processor.putImageForPassedOnTop);
            menu.addKey('K', "", () -> processor.keepPassedInOriginal = !processor.keepPassedInOriginal);
            while (true) {
                menu.updateKeyDescription('F', "Point Filter: " + ANSIHelper.blue(processor.filter.getPointFilterTitle()));
                menu.updateKeyDescription('T', "Put image for " + ANSIHelper.blue(processor.putImageForPassedOnTop ? "passed" : "failed") + " pixels on stack top");
                menu.updateKeyDescription('K', "Keep " + ANSIHelper.blue(processor.keepPassedInOriginal ? "passed" : "failed") + " pixels in original image");
                char c = menu.scan(context);
                if (c == '-') {
                    break;
                }
            }
            setter.accept(processor);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

    }

}
