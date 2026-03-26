package pelemenguin.texturegen.api.util;

import java.util.function.IntPredicate;

import com.google.gson.annotations.SerializedName;

/**
 * A util class to record a range of integers.
 */
public class IntegerRange implements IntPredicate {

    @SerializedName("lower_bound")
    public int lowerBound = 0;
    @SerializedName("upper_bound")
    public int upperBound = 0;
    @SerializedName("lower_bound_inclusive")
    public boolean lowerBoundInclusive = true;
    @SerializedName("upper_bound_inclusive")
    public boolean upperBoundInclusive = true;
    public boolean invert = false;

    public IntegerRange() {
        this(0, 100, true, true, false);
    }

    public IntegerRange(int lowerBound, int upperBound, boolean lowerBoundInclusive, boolean upperBoundInclusive,
            boolean invert) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerBoundInclusive = lowerBoundInclusive;
        this.upperBoundInclusive = upperBoundInclusive;
        this.invert = invert;
    }

    @Override
    public boolean test(int value) {
        boolean lowerPass = (invert ^ lowerBoundInclusive) ? value >= lowerBound : value > lowerBound;
        boolean upperPass = (invert ^ upperBoundInclusive) ? value <= upperBound : value < upperBound;
        return invert ^ (lowerPass && upperPass);
    }

    public String getInequationRepresentation(String variableName) {
        if (this.invert) {
            return new StringBuilder()
                .append(variableName)
                .append(' ').append(this.lowerBoundInclusive ? "\u2264" : "<")
                .append(' ').append(this.lowerBound)
                .append(" or ").append(variableName)
                .append(' ').append(this.upperBoundInclusive ? "\u2265" : ">")
                .append(' ').append(this.upperBound)
                .toString();
        } else {
            return new StringBuilder().append(this.lowerBound)
                .append(' ').append(this.lowerBoundInclusive ? "\u2264" : "<")
                .append(' ').append(variableName)
                .append(' ').append(this.upperBoundInclusive ? "\u2264" : "<")
                .append(' ').append(this.upperBound)
                .toString();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + lowerBound;
        result = prime * result + upperBound;
        result = prime * result + (lowerBoundInclusive ? 1231 : 1237);
        result = prime * result + (upperBoundInclusive ? 1231 : 1237);
        result = prime * result + (invert ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IntegerRange other = (IntegerRange) obj;
        if (lowerBound != other.lowerBound)
            return false;
        if (upperBound != other.upperBound)
            return false;
        if (lowerBoundInclusive != other.lowerBoundInclusive)
            return false;
        if (upperBoundInclusive != other.upperBoundInclusive)
            return false;
        if (invert != other.invert)
            return false;
        return true;
    }

    @Override
    public IntegerRange negate() {
        IntegerRange result = new IntegerRange();
        result.invert = !this.invert;
        result.lowerBoundInclusive = !this.lowerBoundInclusive;
        result.upperBoundInclusive = !this.upperBoundInclusive;
        result.lowerBound = this.lowerBound;
        result.upperBound = this.upperBound;
        return result;
    }

}
