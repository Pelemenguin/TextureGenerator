package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalNoiseProviderEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.NoiseProvider;

public class NoiseRenderer implements Processor {

    public NoiseProvider noise = NoiseProvider.constant(0);

    @SerializedName("value_for_black")
    public float valueForBlack = -1;
    @SerializedName("value_for_white")
    public float valueForWhite = 1;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage original = parameters.load(0, BufferedImage.class);
        BufferedImage resultImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                float noiseValue = this.noise.getNoiseValueAt(context, original, x, y);
                float grey = (noiseValue - this.valueForBlack) / (this.valueForWhite - this.valueForBlack);
                if (Float.isNaN(grey) || Float.isInfinite(grey)) {
                    throw new IllegalStateException("Calculated grey value is invalid: " + grey + " (valueForBlack: " + this.valueForBlack + ", valueForWhite: " + this.valueForWhite + ")");
                }
                int greyInt = Math.max(0, Math.min(255, (int) (grey * 255)));
                int argb = (0xFF << 24) | (greyInt << 16) | (greyInt << 8) | greyInt;
                resultImage.setRGB(x, y, argb);
            }
        }

        result.push(0, original);
        result.push(1, resultImage);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.noise_renderer", NoiseRenderer.class);
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
        return "Noise Renderer";
    }

    @Override
    public String getProcessorTitle() {
        return "Noise Renderer (" + this.valueForBlack + " -> " + this.valueForWhite + ")";
    }

    @Override
    public void processorInit() {
        this.noise.noiseProviderInit();
    }

    @Override
    public void processorFinalize() {
        this.noise.noiseProviderFinalize();
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<NoiseRenderer> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.noise_renderer", this);
        }

        @Override
        public void editorLoop(NoiseRenderer processor, Consumer<NoiseRenderer> setter, TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu("Noise Renderer").autoUppercase();
            menu.addKey('-', "Back");
            menu.addKey('L', () -> {
                new StringInput("Enter new value: (Leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.valueForBlack)) + ")")
                    .allowEmpty().scanAndRun(context, (resultString, error) -> {
                        if (resultString.isBlank()) return;
                        try {
                            float r = Float.parseFloat(resultString);
                            processor.valueForBlack = r;
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid number."));
                        }
                    });
            });
            menu.addKey('H', () -> {
                new StringInput("Enter new value: (Leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.valueForWhite)) + ")")
                    .allowEmpty().scanAndRun(context, (resultString, error) -> {
                        if (resultString.isBlank()) return;
                        try {
                            float r = Float.parseFloat(resultString);
                            processor.valueForWhite = r;
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid number."));
                        }
                    });
            });
            menu.addKey('N', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> processor.noise, noise -> processor.noise = noise, context));
            menu.addKey('M', "Replace Noise Provider", TerminalNoiseProviderEditorProvider.getSelectionList(noise -> processor.noise = noise, context));
            while (true) {
                menu.updateKeyDescription('L', "Noise value for black pixel: " + ANSIHelper.blue(String.valueOf(processor.valueForBlack)));
                menu.updateKeyDescription('H', "Noise value for white pixel: " + ANSIHelper.blue(String.valueOf(processor.valueForWhite)));
                menu.updateKeyDescription('N', "Noise Provider: " + ANSIHelper.blue(processor.noise.getNoiseProviderTitle()));
                if (menu.scan(context) == '-') {
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
