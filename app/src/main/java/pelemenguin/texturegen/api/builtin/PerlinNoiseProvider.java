package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.FieldEditorMenu;
import pelemenguin.texturegen.api.client.terminal.ListEditorMenu;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalNoiseProviderEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.ImageUtils;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.NoiseProvider;
import pelemenguin.texturegen.api.util.PerlinNoise;

public class PerlinNoiseProvider implements NoiseProvider {

    private static final Random INITIAL_SEED_RANDOM = new Random();

    public long seed = INITIAL_SEED_RANDOM.nextLong();
    public List<PerlinNoise.Octave> octaves = new ArrayList<>();
    private transient ConcurrentHashMap<BufferedImage, PerlinNoise> cachedPerlinNoises = new ConcurrentHashMap<>();

    @Override
    public void register(JsonRegistry<NoiseProvider> registry) {
        registry.register("texturegen.perlin_noise", PerlinNoiseProvider.class);
    }

    @Override
    public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
        PerlinNoise noise = cachedPerlinNoises.get(inputImage);
        if (noise == null) {
            noise = new PerlinNoise(this.seed * 31L + ImageUtils.hashCode(inputImage));
            cachedPerlinNoises.put(inputImage, noise);
        }
        return noise.getNoiseValueAt(x, y, this.octaves);
    }

    @Override
    public String getNoiseProviderName() {
        return "Perlin Noise";
    }

    @Override
    public String getNoiseProviderTitle() {
        return "Perlin Noise (" + this.octaves.size() + " octaves)";
    }

    public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<PerlinNoiseProvider> {

        @Override
        public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
            registry.register("texturegen.perlin_noise", this);
        }

        @Override
        public void editorLoop(PerlinNoiseProvider noiseProvider, Consumer<PerlinNoiseProvider> setter,
                TerminalMenuContext context) {
            new ListEditorMenu<>(noiseProvider.octaves, (value, noiseSetter) -> {
                value = value == null ? new PerlinNoise.Octave(1, 1) : value;
                new FieldEditorMenu<>(value).loop(context);
                noiseSetter.accept(value);
            }).extraKeys(m -> m.addKey('S', "Seed: " + ANSIHelper.blue(String.valueOf(noiseProvider.seed)), () -> {
                new StringInput("Enter new seed: (Leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(noiseProvider.seed)) + ")")
                    .allowEmpty()
                    .scanAndRun(context, (result, error) -> {
                        if (result.isBlank()) return;
                        try {
                            long r = Long.parseLong(result);
                            noiseProvider.seed = r;
                            for (var entry : noiseProvider.cachedPerlinNoises.entrySet()) {
                                BufferedImage image = entry.getKey();
                                entry.getValue().seed(r * 31L + ImageUtils.hashCode(image));
                            }
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid integer."));
                        }
                    });
            })).loop(context);
            setter.accept(noiseProvider);
        }

        @Override
        public Editor<? extends NoiseProvider> getEditor() {
            return this;
        }

    }

}
