package pelemenguin.texturegen.api.client.terminal;

import pelemenguin.texturegen.api.util.IntegerRange;

public class TerminalIntegerRangeEditor {
    
    private IntegerRange range;
    private boolean fixedRange = false;
    private int minRange = 0;
    private int maxRange = 100;

    public TerminalIntegerRangeEditor(IntegerRange range) {
        this.range = range;
    }

    public TerminalIntegerRangeEditor fixRange(int min, int max) {
        this.fixedRange = true;
        this.maxRange = min;
        this.maxRange = max;
        return this;
    }

    private String getBar(int min, int max) {
        int barLength = 50;
        int charIndexOfLowerBound = (int) ((range.lowerBound - min) / (double) (max - min) * barLength);
        int charIndexOfUpperBound = (int) ((range.upperBound - min) / (double) (max - min) * barLength);
        String partBar1 = (range.invert ? "=" : "-").repeat(Math.max(0, charIndexOfLowerBound));
        String partBar2 = (range.invert ? "-" : "=").repeat(Math.max(0, charIndexOfUpperBound - charIndexOfLowerBound - 1));
        String partBar3 = (range.invert ? "=" : "-").repeat(Math.max(0, barLength - charIndexOfUpperBound - 1));
        String split1 = (range.lowerBoundInclusive ? "|" : (range.invert ? ")" : "("));
        String split2 = (range.upperBoundInclusive ? "|" : (range.invert ? "(" : ")"));
        if (range.invert) {
            return ANSIHelper.green(partBar1 + split1) + ANSIHelper.red(partBar2) + ANSIHelper.green(split2 + partBar3);
        } else {
            return ANSIHelper.red(partBar1) + ANSIHelper.green(split1 + partBar2 + split2) + ANSIHelper.red(partBar3);
        }
    }

    public IntegerRange loop(TerminalMenuContext context) {
        TerminalMenu menu = new TerminalMenu().autoUppercase();
        menu.addKey('-', "Back");
        menu.addKey('L', "Lower Bound:   " + ANSIHelper.blue(String.valueOf(range.lowerBound)), () -> {
            menu.updateKeyDescription('L', "Lower Bound: " + ANSIHelper.bold(ANSIHelper.blue("> " + range.lowerBound + " <")));
            new StringInput(menu.getDisplayContent())
                .allowEmpty().scanAndRun(context, (result, error) -> {
                    if (result.isBlank()) return;
                    try {
                        int newValue = Integer.parseInt(result);
                        if (newValue > range.upperBound) {
                            error.accept(ANSIHelper.red("New value should be less than " + range.upperBound));
                            return;
                        }
                        if (this.fixedRange && newValue < this.minRange) {
                            error.accept(ANSIHelper.red("New value should be greater than " + this.minRange));
                            return;
                        }
                        range.lowerBound = newValue;
                    } catch (NumberFormatException e) {
                        error.accept(ANSIHelper.red("Invalid number."));
                    }
                });
            menu.updateKeyDescription('L', "Lower Bound:   " + ANSIHelper.blue(String.valueOf(range.lowerBound)));
        });
        menu.addKey('U', "Upper Bound:   " + ANSIHelper.blue(String.valueOf(range.upperBound)), () -> {
            menu.updateKeyDescription('U', "Upper Bound: " + ANSIHelper.bold(ANSIHelper.blue("> " + range.upperBound + " <")));
            new StringInput(menu.getDisplayContent())
                .allowEmpty().scanAndRun(context, (result, error) -> {
                    if (result.isBlank()) return;
                    try {
                        int newValue = Integer.parseInt(result);
                        if (newValue < range.lowerBound || newValue > this.maxRange) {
                            error.accept(ANSIHelper.red("New value should be greater than " + range.lowerBound));
                            return;
                        }
                        if (this.fixedRange && newValue > this.maxRange) {
                            error.accept(ANSIHelper.red("New value should be less than " + this.minRange));
                            return;
                        }
                        range.upperBound = newValue;
                    } catch (NumberFormatException e) {
                        error.accept(ANSIHelper.red("Invalid number."));
                    }
                });
            menu.updateKeyDescription('U', "Upper Bound:   " + ANSIHelper.blue(String.valueOf(range.upperBound)));
        });
        menu.addKey('Z', "Lower Bound: " + ANSIHelper.blue(range.lowerBoundInclusive ? "Inclusive" : "Exclusive"), () -> {
            range.lowerBoundInclusive = !range.lowerBoundInclusive;
            menu.updateKeyDescription('Z', "Lower Bound: " + ANSIHelper.blue(range.lowerBoundInclusive ? "Inclusive" : "Exclusive"));
        });
        menu.addKey('X', "Upper Bound: " + ANSIHelper.blue(range.upperBoundInclusive ? "Inclusive" : "Exclusive"), () -> {
            range.upperBoundInclusive = !range.upperBoundInclusive;
            menu.updateKeyDescription('X', "Upper Bound: " + ANSIHelper.blue(range.upperBoundInclusive ? "Inclusive" : "Exclusive"));
        });
        menu.addKey('I', "Invert:      " + ANSIHelper.blue(String.valueOf(range.invert)), () -> {
            range.invert = !range.invert;
            menu.updateKeyDescription('I', "Invert:      " + ANSIHelper.blue(String.valueOf(range.invert)));
        });
        while (true) {
            if (this.fixedRange) {
                menu.updateDescription(getBar(this.minRange, this.maxRange));
            } else {
                int d = this.range.upperBound - this.range.lowerBound;
                menu.updateDescription(getBar(this.range.lowerBound - d, this.range.upperBound + d));
            }

            if (menu.scan(context) == '-') {
                break;
            }
        }

        return this.range;
    }

}
