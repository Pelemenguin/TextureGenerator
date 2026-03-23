package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public sealed abstract class RGBAPointFilter implements PointFilter
    permits RGBAPointFilter.Red, RGBAPointFilter.Green, RGBAPointFilter.Blue, RGBAPointFilter.Alpha {

    @SerializedName("lower_bound")
    protected int lowerBound;
    @SerializedName("upper_bound")
    protected int upperBound;
    @SerializedName("lower_bound_inclusive")
    protected boolean lowerBoundInclusive = true;
    @SerializedName("upper_bound_inclusive")
    protected boolean upperBoundInclusive = true;
    @SerializedName("invert")
    protected boolean invert = false;

    protected transient int rgbaBand;
    protected transient String rgbaName;

    protected transient final int MAX = 255;
    protected transient final int MIN = 0;

    public RGBAPointFilter() {
        this.lowerBound = this.MIN;
        this.upperBound = this.MAX;
    }

    protected boolean testValue(int value) {
        boolean lowerPass = (invert ^ lowerBoundInclusive) ? value >= lowerBound : value > lowerBound;
        boolean upperPass = (invert ^ upperBoundInclusive) ? value <= upperBound : value < upperBound;
        return invert ^ (lowerPass && upperPass);
    }
    
    @Override
    public void filter(BufferedImage image, BufferedImage maskResult) {
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
        String lowerBoundSymbol = lowerBoundInclusive ? "\u2264" : "<";
        String upperBoundSymbol = invert ? (upperBoundInclusive ? "\u2265" : ">") : (upperBoundInclusive ? "\u2264" : "<");
        if (invert) {
            return String.format("RGBA %1$s (%1$s %2$s %3$d or %1$s %4$s %5$d)", this.rgbaName, lowerBoundSymbol, lowerBound, upperBoundSymbol, upperBound);
        } else {
            return String.format("RGBA %1$s (%3$d %2$s %1$s %4$s %5$d)", this.rgbaName, lowerBoundSymbol, lowerBound, upperBoundSymbol, upperBound);
        }
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
            TerminalMenu menu = new TerminalMenu("")
                .autoUppercase();
            menu.addKey('-', "Back")
                .addKey('L', "Lower Bound: " + ANSIHelper.blue(String.valueOf(pointFilter.lowerBound)), () -> {
                    menu.updateKeyDescription('L', "Lower Bound: " + ANSIHelper.bold(ANSIHelper.blue("> " + pointFilter.lowerBound + " <")));
                    String result = new StringInput(menu.getDisplayContent()).allowEmpty().scan(context);
                    try {
                        int newLowerBound = Integer.parseInt(result);
                        if (newLowerBound < pointFilter.MIN || newLowerBound > pointFilter.upperBound) {
                            throw new NumberFormatException();
                        }
                        pointFilter.lowerBound = newLowerBound;
                        setter.accept(pointFilter);
                    } catch (NumberFormatException e) {
                        context.outStream().println(ANSIHelper.red("Invalid input. Please enter an integer within %d ~ %d".formatted(pointFilter.MIN, pointFilter.upperBound)));
                    }
                    menu.updateKeyDescription('L', "Lower Bound: " + ANSIHelper.blue(String.valueOf(pointFilter.lowerBound)));
                })
                .addKey('U', "Upper Bound: " + ANSIHelper.blue(String.valueOf(pointFilter.upperBound)), () -> {
                    menu.updateKeyDescription('U', "Upper Bound: " + ANSIHelper.bold(ANSIHelper.blue("> " + pointFilter.upperBound + " <")));
                    String result = new StringInput(menu.getDisplayContent()).allowEmpty().scan(context);
                    try {
                        int newUpperBound = Integer.parseInt(result);
                        if (newUpperBound > pointFilter.MAX || newUpperBound < pointFilter.lowerBound) {
                            throw new NumberFormatException();
                        }
                        pointFilter.upperBound = newUpperBound;
                        setter.accept(pointFilter);
                    } catch (NumberFormatException e) {
                        context.outStream().println(ANSIHelper.red("Invalid input. Please enter an integer within %d ~ %d".formatted(pointFilter.lowerBound, pointFilter.MAX)));
                    }
                    menu.updateKeyDescription('U', "Upper Bound: " + ANSIHelper.blue(String.valueOf(pointFilter.upperBound)));
                })
                .addKey('W', "Lower Bound Inclusive: " + ANSIHelper.blue(String.valueOf(pointFilter.lowerBoundInclusive)), () -> {
                    pointFilter.lowerBoundInclusive = !pointFilter.lowerBoundInclusive;
                    menu.updateKeyDescription('W', "Lower Bound Inclusive: " + ANSIHelper.blue(String.valueOf(pointFilter.lowerBoundInclusive)));
                    setter.accept(pointFilter);
                })
                .addKey('P', "Upper Bound Inclusive: " + ANSIHelper.blue(String.valueOf(pointFilter.upperBoundInclusive)), () -> {
                    pointFilter.upperBoundInclusive = !pointFilter.upperBoundInclusive;
                    menu.updateKeyDescription('P', "Upper Bound Inclusive: " + ANSIHelper.blue(String.valueOf(pointFilter.upperBoundInclusive)));
                    setter.accept(pointFilter);
                })
                .addKey('I', "Invert: " + ANSIHelper.blue(String.valueOf(pointFilter.invert)), () -> {
                    pointFilter.invert = !pointFilter.invert;
                    menu.updateKeyDescription('I', "Invert: " + ANSIHelper.blue(String.valueOf(pointFilter.invert)));
                    setter.accept(pointFilter);
                });
            while (true) {
                int barLength = 50;
                int charIndexOfLowerBound = (int) ((pointFilter.lowerBound - pointFilter.MIN) / (double) (pointFilter.MAX - pointFilter.MIN) * barLength);
                int charIndexOfUpperBound = (int) ((pointFilter.upperBound - pointFilter.MIN) / (double) (pointFilter.MAX - pointFilter.MIN) * barLength);
                String partBar1 = (pointFilter.invert ? "=" : "-").repeat(Math.max(0, charIndexOfLowerBound));
                String partBar2 = (pointFilter.invert ? "-" : "=").repeat(Math.max(0, charIndexOfUpperBound - charIndexOfLowerBound - 1));
                String partBar3 = (pointFilter.invert ? "=" : "-").repeat(Math.max(0, barLength - charIndexOfUpperBound - 1));
                String split1 = (pointFilter.lowerBoundInclusive ? "|" : (pointFilter.invert ? ")" : "("));
                String split2 = (pointFilter.upperBoundInclusive ? "|" : (pointFilter.invert ? "(" : ")"));
                String bar;
                if (pointFilter.invert) {
                    bar = ANSIHelper.green(partBar1 + split1) + ANSIHelper.red(partBar2) + ANSIHelper.green(split2 + partBar3);
                } else {
                    bar = ANSIHelper.red(partBar1) + ANSIHelper.green(split1 + partBar2 + split2) + ANSIHelper.red(partBar3);
                }
                menu.updateDescription(pointFilter.getPointFilterTitle() + "\n\n" + bar + "\n");

                char choice = menu.scan(context);
                if (choice == '-') {
                    break;
                }
            }
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
