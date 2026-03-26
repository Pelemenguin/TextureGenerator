package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.TerminalIntegerRangeEditor;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.util.ColorHelper;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;
import pelemenguin.texturegen.api.util.IntegerRange;

public sealed abstract class HSVPointFilter implements PredicatePointFilter
    permits HSVPointFilter.Value, HSVPointFilter.Saturation, HSVPointFilter.Hue {

    public IntegerRange range = new IntegerRange();

    protected transient int hsvBand;
    protected transient String hsvName;

    protected transient int MAX;
    protected transient int MIN;

    protected boolean testValue(int value) {
        return range.test(value);
    }

    public static sealed abstract class TerminalEditor<F extends HSVPointFilter> implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<F>
        permits Hue.TerminalEditor, Saturation.TerminalEditor, Value.TerminalEditor {

        @Override
        public void editorLoop(F pointFilter, Consumer<F> setter, TerminalMenuContext context) {
            IntegerRange range = new TerminalIntegerRangeEditor(pointFilter.range)
                .fixRange(pointFilter.MIN, pointFilter.MAX).loop(context);
            pointFilter.range = range;
            setter.accept(pointFilter);
        }

        @Override
        public Editor<? extends PointFilter> getEditor() {
            return this;
        }

    }

    @Override
    public boolean testPoint(GenerationContext context, BufferedImage image, int x, int y) {
        int[] temp = new int[3];
        int rgba = image.getRGB(x, y);
        ColorHelper.rgbToHsv(
            (rgba >> 16) & 0xFF,
            (rgba >> 8) & 0xFF,
            rgba & 0xFF,
            temp
        );
        return testValue(temp[this.hsvBand]);
    }

    @Override
    public String getPointFilterName() {
        return "HSV " + this.hsvName + " Filter";
    }

    @Override
    public String getPointFilterTitle() {
        return "HSV %1$s (%2$s)".formatted(this.hsvName, this.range.getInequationRepresentation(hsvName));
    }

    public static final class Hue extends HSVPointFilter {

        public Hue() {
            this.MIN = 0;
            this.MAX = 360;
            this.range.lowerBound = this.MIN;
            this.range.upperBound = this.MAX;
            this.hsvBand = 0;
            this.hsvName = "Hue";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.hue", Hue.class);
        }

        public static final class TerminalEditor extends HSVPointFilter.TerminalEditor<Hue> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.hsv.hue", this);
            }

        }

    }

    public static final class Saturation extends HSVPointFilter {

        public Saturation() {
            this.MIN = 0;
            this.MAX = 100;
            this.range.lowerBound = this.MIN;
            this.range.upperBound = this.MAX;
            this.hsvBand = 1;
            this.hsvName = "Saturation";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.saturation", Saturation.class);
        }

        public static final class TerminalEditor extends HSVPointFilter.TerminalEditor<Saturation> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.hsv.saturation", this);
            }

        }

    }

    public static final class Value extends HSVPointFilter {

        public Value() {
            this.MIN = 0;
            this.MAX = 100;
            this.range.lowerBound = this.MIN;
            this.range.upperBound = this.MAX;
            this.hsvBand = 2;
            this.hsvName = "Value";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.value", Value.class);
        }

        public static final class TerminalEditor extends HSVPointFilter.TerminalEditor<Value> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.hsv.value", this);
            }

        }

    }

}
