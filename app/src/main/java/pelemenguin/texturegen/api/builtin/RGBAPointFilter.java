package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.TerminalIntegerRangeEditor;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;
import pelemenguin.texturegen.api.util.IntegerRange;

public sealed abstract class RGBAPointFilter implements PointFilter
    permits RGBAPointFilter.Red, RGBAPointFilter.Green, RGBAPointFilter.Blue, RGBAPointFilter.Alpha {
        
    public IntegerRange range = new IntegerRange();

    protected transient int rgbaBand;
    protected transient String rgbaName;

    protected transient final int MAX = 255;
    protected transient final int MIN = 0;

    public RGBAPointFilter() {
        this.range.lowerBound = this.MIN;
        this.range.upperBound = this.MAX;
    }

    protected boolean testValue(int value) {
        return this.range.test(value);
    }
    
    @Override
    public void filter(GenerationContext context, BufferedImage image, BufferedImage maskResult) {
        Raster raster = image.getRaster();
        WritableRaster result = maskResult.getRaster();
        for (int y = 0; y < raster.getHeight(); y++) {
            for (int x = 0; x < raster.getWidth(); x++) {
                int value = raster.getSample(x, y, rgbaBand);
                int maskValue = testValue(value) ? 1 : 0;
                result.setSample(x, y, 0, maskValue);
            }
        }
    }

    @Override
    public String getPointFilterName() {
        return "RGBA " + this.rgbaName + " Filter";
    }

    @Override
    public String getPointFilterTitle() {
        return "RGBA %1$s (%2$s)".formatted(this.rgbaName, this.range.getInequationRepresentation(rgbaName));
    }

    public static sealed abstract class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<RGBAPointFilter>
        permits Red.TerminalEditor, Green.TerminalEditor, Blue.TerminalEditor, Alpha.TerminalEditor {

        @Override
        public Editor<? extends PointFilter> getEditor() {
            return this;
        }

        @Override
        public void editorLoop(RGBAPointFilter pointFilter, Consumer<RGBAPointFilter> setter,
                TerminalMenuContext context) {
            IntegerRange range = new TerminalIntegerRangeEditor(pointFilter.range)
                .fixRange(pointFilter.MIN, pointFilter.MAX).loop(context);
            pointFilter.range = range;
            setter.accept(pointFilter);
        }

    }

    public static final class Red extends RGBAPointFilter {

        public Red() {
            super();
            this.rgbaBand = 0;
            this.rgbaName = "Red";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.rgba.red", Red.class);
        }

        public static final class TerminalEditor extends RGBAPointFilter.TerminalEditor {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.rgba.red", this);
            }

        }

    }

    public static final class Green extends RGBAPointFilter {

        public Green() {
            super();
            this.rgbaBand = 1;
            this.rgbaName = "Green";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.rgba.green", Green.class);
        }

        public static final class TerminalEditor extends RGBAPointFilter.TerminalEditor {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.rgba.green", this);
            }

        }

    }

    public static final class Blue extends RGBAPointFilter {

        public Blue() {
            super();
            this.rgbaBand = 2;
            this.rgbaName = "Blue";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.rgba.blue", Blue.class);
        }

        public static final class TerminalEditor extends RGBAPointFilter.TerminalEditor {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.rgba.blue", this);
            }

        }

    }

    public static final class Alpha extends RGBAPointFilter {

        public Alpha() {
            super();
            this.rgbaBand = 3;
            this.rgbaName = "Alpha";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.rgba.alpha", Alpha.class);
        }

        public static final class TerminalEditor extends RGBAPointFilter.TerminalEditor {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.rgba.alpha", this);
            }

        }

    }

}
