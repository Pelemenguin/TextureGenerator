package pelemenguin.texturegen.api.builtin;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;

/**
 * {@code ImageMerger} merges to images together using {@link Graphics2D}.
 * Note that there is <b>always one image to be modified</b>, depending on the value of {@link #writeSecondOnFirst}.
 */
public class ImageMerger implements Processor {

    @SerializedName("write_second_on_first")
    public boolean writeSecondOnFirst = true;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage first = parameters.load(0, BufferedImage.class);
        BufferedImage second = parameters.load(1, BufferedImage.class);

        BufferedImage base;
        BufferedImage overlay;
        if (writeSecondOnFirst) {
            base = first;
            overlay = second;
        } else {
            base = second;
            overlay = first;
        }
        Graphics2D g = base.createGraphics();
        try {
            g.drawImage(overlay, 0, 0, null);
        } finally {
            g.dispose();
        }

        result.push(0, base);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.merger", ImageMerger.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class, BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Image Merger";
    }

    @Override
    public String getProcessorTitle() {
        return this.writeSecondOnFirst ? "Draw second image on first image" : "Draw first image on second image";
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<ImageMerger> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.merger", this);
        }

        @Override
        public void editorLoop(ImageMerger processor, Consumer<ImageMerger> setter, TerminalMenuContext context) {
            processor.writeSecondOnFirst = !processor.writeSecondOnFirst;
            setter.accept(processor);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

    }

}
