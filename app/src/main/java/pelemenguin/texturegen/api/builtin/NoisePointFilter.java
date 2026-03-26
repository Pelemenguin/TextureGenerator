package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.TerminalDoubleRangeEditor;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalNoiseProviderEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.DoubleRange;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.NoiseProvider;
import pelemenguin.texturegen.api.util.PointFilter;

public class NoisePointFilter implements PointFilter {

    public NoiseProvider noise = NoiseProvider.constant(0);
    public DoubleRange range = new DoubleRange();

    @Override
    public void register(JsonRegistry<PointFilter> registry) {
        registry.register("texturegen.noise", NoisePointFilter.class);
    }

    @Override
    public void filter(GenerationContext context, BufferedImage image, BufferedImage maskResult) {
        WritableRaster raster = maskResult.getRaster();
        for (int x = 0; x < maskResult.getWidth(); x++) {
            for (int y = 0; y < maskResult.getHeight(); y++) {
                float noiseValue = this.noise.getNoiseValueAt(context, image, x, y);
                raster.setSample(x, y, 0, range.test(noiseValue) ? 1 : 0);
            }
        }
    }

    @Override
    public String getPointFilterName() {
        return "Noise Filter";
    }

    @Override
    public String getPointFilterTitle() {
        return "Noise Filter (" + this.noise.getNoiseProviderTitle() + ")";
    }

    public static class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<NoisePointFilter> {

        @Override
        public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
            registry.register("texturegen.noise", this);
        }

        @Override
        public void editorLoop(NoisePointFilter pointFilter, Consumer<NoisePointFilter> setter,
                TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu("Noise Filter").autoUppercase();
            menu.addKey('-', "Back");
            menu.addKey('N', TerminalNoiseProviderEditorProvider.getEditorLooop(() -> pointFilter.noise, n -> pointFilter.noise = n, context));
            menu.addKey('M', "Replace Noise Provider", TerminalNoiseProviderEditorProvider.getSelectionList(n -> pointFilter.noise = n, context));
            menu.addKey('R', () -> {
                DoubleRange range = new TerminalDoubleRangeEditor(pointFilter.range).loop(context);
                pointFilter.range = range;
            });
            while (true) {
                menu.updateKeyDescription('N', "Noise Provider: " + ANSIHelper.blue(pointFilter.noise.getNoiseProviderTitle()));
                menu.updateKeyDescription('R', "Range: " + ANSIHelper.blue(pointFilter.range.getInequationRepresentation("Value")));
                if (menu.scan(context) == '-') break;
            }
            setter.accept(pointFilter);
        }

        @Override
        public Editor<? extends PointFilter> getEditor() {
            return this;
        }

    }

}
