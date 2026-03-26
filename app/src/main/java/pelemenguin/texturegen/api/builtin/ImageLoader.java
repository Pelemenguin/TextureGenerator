package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;

public class ImageLoader implements Processor {

    Path path = Path.of(".");

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage resultImage;
        try {
            resultImage = GenerationExecutor.toARGB(ImageIO.read(context.assetsRootFolder().resolve(this.path).toFile()));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to open image: " + this.path, t);
        }
        result.push(0, resultImage);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.loader", ImageLoader.class);
    }

    @Override
    public List<Class<?>> getOutputTypes(List<Class<?>> currentStack) {
        return List.of(BufferedImage.class);
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<ImageLoader> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.loader", this);
        }

        @Override
        public void editorLoop(ImageLoader processor, Consumer<ImageLoader> setter, TerminalMenuContext context) {
            new StringInput("Enter new image path. (Leave empty to cancel. Original: " + ANSIHelper.blue(processor.path.toString()) + ")")
                .allowEmpty()
                .scanAndRun(context, (resString, error) -> {
                    if (resString.isBlank()) return;
                    try {
                        Path path = Path.of(resString);
                        processor.path = path;
                    } catch (Throwable t) {
                        error.accept(ANSIHelper.red("Failed to set path: " + t.getMessage()));
                    }
                });
            setter.accept(processor);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

    }

}
