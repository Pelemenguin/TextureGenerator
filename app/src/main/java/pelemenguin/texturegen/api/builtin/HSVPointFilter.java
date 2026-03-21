package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.util.ColorHelper;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public sealed abstract class HSVPointFilter implements PredicatePointFilter
    permits HSVPointFilter.Value, HSVPointFilter.Saturation, HSVPointFilter.Hue {

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

    protected transient int hsvBand;
    protected transient String hsvName;

    protected boolean testValue(int value) {
        boolean lowerPass = lowerBoundInclusive ? value >= lowerBound : value > lowerBound;
        boolean upperPass = upperBoundInclusive ? value <= upperBound : value < upperBound;
        return invert ^ (lowerPass && upperPass);
    }
        
    @Override
    public boolean testPoint(BufferedImage image, int x, int y) {
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
        String lowerBoundSymbol = lowerBoundInclusive ? "\u2264" : "<";
        String upperBoundSymbol = invert ? (upperBoundInclusive ? "\u2265" : ">") : (upperBoundInclusive ? "\u2264" : "<");
        if (invert) {
            return String.format("HSV %1$s (%1$s %2$s %3$d or %1$s %4$s %5$d)", this.hsvName, lowerBoundSymbol, lowerBound, upperBoundSymbol, upperBound);
        } else {
            return String.format("HSV %1$s (%3$d %2$s %1$s %4$s %5$d)", this.hsvName, lowerBoundSymbol, lowerBound, upperBoundSymbol, upperBound);
        }
    }

    public static final class Hue extends HSVPointFilter {

        public Hue() {
            this.lowerBound = 0;
            this.upperBound = 360;
            this.hsvBand = 0;
            this.hsvName = "Hue";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.hue", Hue.class);
        }

    }

    public static final class Saturation extends HSVPointFilter {

        public Saturation() {
            this.lowerBound = 0;
            this.upperBound = 100;
            this.hsvBand = 1;
            this.hsvName = "Saturation";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.saturation", Saturation.class);
        }

    }

    public static final class Value extends HSVPointFilter {

        public Value() {
            this.lowerBound = 0;
            this.upperBound = 100;
            this.hsvBand = 2;
            this.hsvName = "Value";
        }

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.hsv.value", Value.class);
        }

    }

}
