package pelemenguin.texturegen.api.util;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ListEditorMenu;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalNoiseProviderEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;

/**
 * A {@code NoiseGenerator} is an interface that accepts a {@link BufferedImage} and returns another {@link BufferedImage} with type {@link BufferedImage#TYPE_BYTE_GRAY}.
 */
public interface NoiseProvider extends JsonRegistry.Registrable<NoiseProvider> {

    public static final JsonRegistry<NoiseProvider> REGISTRY = new JsonRegistry<>(NoiseProvider.class, (registry, generator) -> {
        PrivateObjectHolder.ID_TO_NAMES.put(registry.getIdOf(generator.getClass()), generator.getNoiseProviderName());
    });
    public static final TypeAdapterFactory TYPE_ADAPTER = REGISTRY.createTypeAdapterFactory();

    public static class PrivateObjectHolder {
        private static final HashMap<String, String> ID_TO_NAMES = new HashMap<>();
    }

    public static String getNameFor(String noiseProviderId) {
        return PrivateObjectHolder.ID_TO_NAMES.get(noiseProviderId);
    }

    public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y);

    public default String getNoiseProviderName() {
        return this.getClass().getName();
    }

    public default String getNoiseProviderTitle() {
        return this.toString();
    }

    public default void noiseProviderInit() {}
    public default void noiseProviderFinalize() {}

    public static Constant constant(float value) {
        Constant res = new Constant();
        res.value = value;
        return res;
    }

    public static class Constant implements NoiseProvider {

        public float value;

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.constant", Constant.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            return this.value;
        }

        @Override
        public String getNoiseProviderName() {
            return "Constant";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Constant (" + this.value + ")";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Constant> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.constant", this);
            }

            @Override
            public void editorLoop(Constant noiseProvider, Consumer<Constant> setter, TerminalMenuContext context) {
                new StringInput("Enter new constant value: (Leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(noiseProvider.value)))
                    .allowEmpty().scanAndRun(context, (result, error) -> {
                        if (result.isBlank()) return;
                        try {
                            float newValue = Float.parseFloat(result);
                            noiseProvider.value = newValue;
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid number."));
                        }
                    });
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

    public static class Add implements NoiseProvider {

        public List<NoiseProvider> arguments = new ArrayList<>();

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.add", Add.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            float sum = 0;
            for (NoiseProvider addend : arguments) {
                sum += addend.getNoiseValueAt(context, inputImage, x, y);
            }
            return sum;
        }

        @Override
        public String getNoiseProviderName() {
            return "Add";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Add (" + this.arguments.size() + " noise)";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Add> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.add", this);
            }

            @Override
            public void editorLoop(Add noiseProvider, Consumer<Add> setter, TerminalMenuContext context) {
                new ListEditorMenu<>(noiseProvider.arguments, (original, noiseSetter) -> {
                    if (original == null) {
                        TerminalNoiseProviderEditorProvider.getSelectionList(noiseSetter, context).run();
                    } else {
                        TerminalNoiseProviderEditorProvider.getEditorLooop(() -> original, noiseSetter, context).run();
                    }
                }).description("Edit Noise to add:")
                    .strigifier(NoiseProvider::getNoiseProviderTitle)
                    .loop(context);
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

    public static class Multiply implements NoiseProvider {

        public List<NoiseProvider> arguments = new ArrayList<>();

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.multiply", Multiply.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            float product = 1;
            for (NoiseProvider multiplier : arguments) {
                product *= multiplier.getNoiseValueAt(context, inputImage, x, y);
            }
            return product;
        }

        @Override
        public String getNoiseProviderName() {
            return "Multiply";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Multiply (" + this.arguments.size() + " noise)";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Multiply> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.multiply", this);
            }

            @Override
            public void editorLoop(Multiply noiseProvider, Consumer<Multiply> setter, TerminalMenuContext context) {
                new ListEditorMenu<>(noiseProvider.arguments, (original, noiseSetter) -> {
                    if (original == null) {
                        TerminalNoiseProviderEditorProvider.getSelectionList(noiseSetter, context).run();
                    } else {
                        TerminalNoiseProviderEditorProvider.getEditorLooop(() -> original, noiseSetter, context).run();
                    }
                }).description("Edit noise to multiply:")
                    .strigifier(NoiseProvider::getNoiseProviderTitle)
                    .loop(context);
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

    public static class Minimum implements NoiseProvider {

        public List<NoiseProvider> arguments = new ArrayList<>();

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.min", Minimum.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            float min = Float.POSITIVE_INFINITY;
            for (NoiseProvider argument : arguments) {
                float value = argument.getNoiseValueAt(context, inputImage, x, y);
                if (value < min) {
                    min = value;
                }
            }
            return min;
        }

        @Override
        public String getNoiseProviderName() {
            return "Minimum";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Minimum (" + this.arguments.size() + " noise)";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Minimum> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.min", this);
            }

            @Override
            public void editorLoop(Minimum noiseProvider, Consumer<Minimum> setter, TerminalMenuContext context) {
                new ListEditorMenu<>(noiseProvider.arguments, (original, noiseSetter) -> {
                    if (original == null) {
                        TerminalNoiseProviderEditorProvider.getSelectionList(noiseSetter, context).run();
                    } else {
                        TerminalNoiseProviderEditorProvider.getEditorLooop(() -> original, noiseSetter, context).run();
                    }
                }).description("Edit noise to find minimum:")
                    .strigifier(NoiseProvider::getNoiseProviderTitle)
                    .loop(context);
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

    public static class Maximum implements NoiseProvider {

        public List<NoiseProvider> arguments = new ArrayList<>();

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.max", Maximum.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            float max = Float.NEGATIVE_INFINITY;
            for (NoiseProvider argument : arguments) {
                float value = argument.getNoiseValueAt(context, inputImage, x, y);
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }

        @Override
        public String getNoiseProviderName() {
            return "Maximum";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Maximum (" + this.arguments.size() + " noise)";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Maximum> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.max", this);
            }

            @Override
            public void editorLoop(Maximum noiseProvider, Consumer<Maximum> setter, TerminalMenuContext context) {
                new ListEditorMenu<>(noiseProvider.arguments, (original, noiseSetter) -> {
                    if (original == null) {
                        TerminalNoiseProviderEditorProvider.getSelectionList(noiseSetter, context).run();
                    } else {
                        TerminalNoiseProviderEditorProvider.getEditorLooop(() -> original, noiseSetter, context).run();
                    }
                }).description("Edit noise to find maximum:")
                    .strigifier(NoiseProvider::getNoiseProviderTitle)
                    .loop(context);
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

    public static class Threshold implements NoiseProvider {

        public NoiseProvider source = NoiseProvider.constant(0);
        public NoiseProvider threshold = NoiseProvider.constant(0);
        @SerializedName("if_greater_than")
        public NoiseProvider ifGreaterThan = NoiseProvider.constant(0);
        @SerializedName("if_less_than")
        public NoiseProvider ifLessThan = NoiseProvider.constant(0);
        @SerializedName("use_greater_than_when_equals")
        public boolean useGreaterThanWhenEquals = false;

        @Override
        public void register(JsonRegistry<NoiseProvider> registry) {
            registry.register("texturegen.arithmatic.threshold", Threshold.class);
        }

        @Override
        public float getNoiseValueAt(GenerationContext context, BufferedImage inputImage, float x, float y) {
            float sourceValue = source.getNoiseValueAt(context, inputImage, x, y);
            float thresholdValue = threshold.getNoiseValueAt(context, inputImage, x, y);
            if (sourceValue > thresholdValue || (useGreaterThanWhenEquals && (sourceValue == thresholdValue))) {
                return ifGreaterThan.getNoiseValueAt(context, inputImage, x, y);
            } else {
                return ifLessThan.getNoiseValueAt(context, inputImage, x, y);
            }
        }

        @Override
        public String getNoiseProviderName() {
            return "Threshold";
        }

        @Override
        public String getNoiseProviderTitle() {
            return "Threshold";
        }

        public static class TerminalEditor implements TerminalNoiseProviderEditorProvider, TerminalNoiseProviderEditorProvider.Editor<Threshold> {

            @Override
            public void register(CommonRegistry<TerminalNoiseProviderEditorProvider> registry) {
                registry.register("texturegen.arithmatic.threshold", this);
            }

            @Override
            public void editorLoop(Threshold noiseProvider, Consumer<Threshold> setter, TerminalMenuContext context) {
                TerminalMenu menu = new TerminalMenu("Threshold Noise Provider");
                menu.addKey('-', "Back");
                menu.addKey('s', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> noiseProvider.source, s -> noiseProvider.source = s, context));
                menu.addKey('S', TerminalNoiseProviderEditorProvider.getSelectionList(s -> noiseProvider.source = s, context));
                menu.addKey('t', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> noiseProvider.threshold, s -> noiseProvider.threshold = s, context));
                menu.addKey('T', TerminalNoiseProviderEditorProvider.getSelectionList(s -> noiseProvider.threshold = s, context));
                menu.addKey('l', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> noiseProvider.ifLessThan, s -> noiseProvider.ifLessThan = s, context));
                menu.addKey('L', TerminalNoiseProviderEditorProvider.getSelectionList(s -> noiseProvider.ifLessThan = s, context));
                menu.addKey('g', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> noiseProvider.ifGreaterThan, s -> noiseProvider.ifGreaterThan = s, context));
                menu.addKey('G', TerminalNoiseProviderEditorProvider.getSelectionList(s -> noiseProvider.ifGreaterThan = s, context));
                menu.addKey('e', () -> noiseProvider.useGreaterThanWhenEquals = !noiseProvider.useGreaterThanWhenEquals);
                while (true) {
                    menu.updateKeyDescription('s', "Source Noise: " + ANSIHelper.blue(noiseProvider.source.getNoiseProviderTitle()));
                    menu.updateKeyDescription('S', "Replace Source Noise");
                    menu.updateKeyDescription('t', "Threshold Noise: " + ANSIHelper.blue(noiseProvider.threshold.getNoiseProviderTitle()));
                    menu.updateKeyDescription('T', "Replace Threshold Noise");
                    menu.updateKeyDescription('l', "When Less than Threshold: " + ANSIHelper.blue(noiseProvider.ifLessThan.getNoiseProviderTitle()));
                    menu.updateKeyDescription('L', "Replace Less-than Noise");
                    menu.updateKeyDescription('g', "When Greater than Threshold: " + ANSIHelper.blue(noiseProvider.ifGreaterThan.getNoiseProviderTitle()));
                    menu.updateKeyDescription('G', "Replace Greater-than Noise");
                    menu.updateKeyDescription('e', "Use " + ANSIHelper.blue(noiseProvider.useGreaterThanWhenEquals ? "Greater-Than": "Less-Than") + " Noise when Equals to Threshold");
                    if (menu.scan(context) == '-') {
                        break;
                    }
                }
            }

            @Override
            public Editor<? extends NoiseProvider> getEditor() {
                return this;
            }

        }

    }

}
