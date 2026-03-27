package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.ColorHelper;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;

public class TextureApplier implements Processor {

    @SerializedName("multiplier")
    public double extraMultiplier = 1.2;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage grayImage = parameters.load(0, BufferedImage.class);
        BufferedImage textureImage = parameters.load(1, BufferedImage.class);

        int texWidth = textureImage.getWidth();
        int texHeight = textureImage.getHeight();
        int[] temp = new int[3];
        for (int x = 0; x < grayImage.getWidth(); x++) {
            for (int y = 0; y < grayImage.getHeight(); y++) {
                int grayColor = grayImage.getRGB(x, y);
                int alpha = grayColor & 0xFF000000;
                int gray = ColorHelper.getGrayOrN1(grayColor);
                if (gray == -1) continue;
                ColorHelper.rgbToHsv(textureImage.getRGB(x % texWidth, y % texHeight), temp);
                temp[2] = (int) (temp[2] * (gray / 255.0) * extraMultiplier);
                grayImage.setRGB(x, y, alpha | (ColorHelper.hsvToRgb(temp[0], temp[1], temp[2])));
            }
        }

        result.push(0, grayImage);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.texture", TextureApplier.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class, BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes(List<Class<?>> currentStack) {
        return List.of(BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Texture Applier";
    }

    @Override
    public String getProcessorTitle() {
        return "Texture Applier (" + this.extraMultiplier + "x)";
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider.Editor<TextureApplier>, TerminalProcessorEditorProvider {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.texture", this);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

        @Override
        public void editorLoop(TextureApplier processor, Consumer<TextureApplier> setter, TerminalMenuContext context) {
            new StringInput("Enter new multiplier: (Leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.extraMultiplier)))
                .allowEmpty()
                .scanAndRun(context, (resultString, error) -> {
                    if (resultString.isBlank()) return;
                    try {
                        processor.extraMultiplier = Double.parseDouble(resultString);
                    } catch (NumberFormatException e) {
                        error.accept(ANSIHelper.red("Invalid number."));
                    }
                });
        }

    }

}
