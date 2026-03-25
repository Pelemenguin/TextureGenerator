package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ColorPickerMenu;
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

/**
 * {@code FillColorProcessor} fills a certain area specified by a {@link PointFilter} of a {@link BufferedImage} with the specified color.
 */
public class FillColorProcessor implements Processor {
    
    public PointFilter filter = PointFilter.alwaysPass();
    public int color = 0xFFFFFFFF;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage original = parameters.load(0, BufferedImage.class);

        WritableRaster raster = original.getRaster();
        BufferedImage mask = this.filter.filter(original);
        Raster maskRaster = mask.getRaster();
        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                if (maskRaster.getSample(x, y, 0) > 0) {
                    raster.setPixel(x, y, new int[] {
                        (color >> 16) & 0xFF,
                        (color >> 8) & 0xFF,
                        color & 0xFF,
                        (color >> 24) & 0xFF
                    });
                }
            }
        }

        result.push(0, original);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.fill_color", FillColorProcessor.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Fill Color";
    }

    @Override
    public String getProcessorTitle() {
        return "Fill Color #%08X".formatted(color);
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<FillColorProcessor> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.fill_color", this);
        }

        @Override
        public void editorLoop(FillColorProcessor processor, Consumer<FillColorProcessor> setter,
                TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu(processor.getProcessorName()).autoUppercase();
            menu.addKey('-', "Back")
                .addKey('C', "", () -> {
                    processor.color = new ColorPickerMenu(processor.color)
                        .loop(context)
                        .getIntARGB();
                }).addKey('F', "", () -> {
                    TerminalPointFilterEditorProvider.getEditorLoop(processor.filter, filter -> processor.filter = filter, context).run();
                }).addKey('G', "Replace Point Filter", () -> {
                    TerminalPointFilterEditorProvider.getSelectionList(filter -> processor.filter = filter, context).run();;
                });
            while (true) {
                menu.updateKeyDescription('C', "Color: #%08X".formatted(processor.color) + (ANSIHelper.ansiEnabled()
                    ? " " + ANSIHelper.rgbBackground("      ", processor.color)
                    : ""
                )).updateKeyDescription('F', "Point Filter: " + ANSIHelper.blue(processor.filter.getPointFilterTitle()));
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
