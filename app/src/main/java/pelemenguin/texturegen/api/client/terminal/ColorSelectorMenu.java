package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.IntConsumer;

public class ColorSelectorMenu {

    private int[] rgba = new int[4];
    private int[] hsva = new int[4];
    // TODO: More modes and implement hsva editing
    private byte mode = MODE_RGBA;
    private int originalColor = 0xFFFFFFFF;
    private int currentColor = 0xFFFFFFFF;

    public static final byte MODE_RGBA = 0;
    public static final byte MODE_HSVA = 1;

    public ColorSelectorMenu(int original) {
        this.rgba[0] = (original >> 16) & 0xFF;
        this.rgba[1] = (original >> 8) & 0xFF;
        this.rgba[2] = original & 0xFF;
        this.rgba[3] = (original >> 24) & 0xFF;
        this.originalColor = original;
        this.currentColor = original;

        this.syncColors(MODE_RGBA);
    }

    public ColorSelectorMenu() {
        this(0xFFFFFFFF);
    }

    public ColorSelectorMenu loop(TerminalMenuContext context) {
        return loop(context.outStream(), context.scanner());
    }

    public ColorSelectorMenu loop(PrintStream out, Scanner scanner) {
        TerminalMenu menu = new TerminalMenu().autoUppercase();

        while (true) {
            StringBuilder sb = new StringBuilder("Color Selector:\n\n");
            this.addColorPreview(sb);
            this.addColorInfo(sb);
            this.addResetKey(menu);
            menu.addKey('-', "Back");
            this.addRGBAKeys(menu, out, scanner);
            menu.updateDescription(sb.toString());

            char c = menu.scan(out, scanner);
            if (c == '-') {
                break;
            }
        }

        return this;
    }

    private void addResetKey(TerminalMenu menu) {
        menu.addKey('!', "Reset to old color", () -> {
            this.rgba[0] = (this.originalColor >> 16) & 0xFF;
            this.rgba[1] = (this.originalColor >> 8) & 0xFF;
            this.rgba[2] = this.originalColor & 0xFF;
            this.rgba[3] = (this.originalColor >> 24) & 0xFF;
            this.syncColors(MODE_RGBA);
        });
    }

    private void addColorPreview(StringBuilder sb) {
        sb.append("Preview: ")
            .append("Old ")
            .append(ANSIHelper.rgbBackground("   ", this.originalColor))
            .append(ANSIHelper.rgbBackground("   ", this.currentColor))
            .append(" New\n");
    }

    private Runnable editNumber(TerminalMenu menu, PrintStream out, Scanner scanner,
        int original, IntConsumer setter, int min, int max,
        Character key, String normalDescription, String selectedDescription,
        int syncMode
    ) {
        return () -> {
            menu.updateKeyDescription(key, selectedDescription);
            String newV = new StringInput(menu.getDisplayContent()).allowEmpty().scan(out, scanner);
            if (!newV.isBlank()) {
                try {
                    int v = Integer.parseInt(newV);
                    if (v < min || v > max) {
                        out.println(ANSIHelper.red("Invalid value. Must be between " + min + " and " + max + "."));
                    } else {
                        setter.accept(v);
                        this.syncColors(syncMode);
                    }
                } catch (NumberFormatException e) {
                    out.println(ANSIHelper.red("Invalid input. Please enter a number between 0 and 255."));
                }
            }
            menu.updateKeyDescription(key, normalDescription);
        };
    }

    // Add a "|" at current progress
    private String rgbColorBar(char rOrGOrB, int currentValue) {
        StringBuilder sb = new StringBuilder();
        boolean metCurrent = false;
        for (int i = 0; i < 51; i++) {
            int threshold = i * 255 / 50;
            int backgroundColor = switch (rOrGOrB) {
                case 'R' -> (currentColor & 0x00FFFF);
                case 'G' -> (currentColor & 0xFF00FF);
                case 'B' -> (currentColor & 0xFFFF00);
                default -> 0x000000;
            };
            backgroundColor |= switch (rOrGOrB) {
                case 'R' -> threshold << 16;
                case 'G' -> threshold << 8;
                case 'B' -> threshold;
                default -> 0x000000;
            };
            if (!metCurrent && currentValue <= threshold) {
                sb.append(ANSIHelper.rgbBackground("|", backgroundColor));
                metCurrent = true;
            } else {
                sb.append(ANSIHelper.rgbBackground(" ", backgroundColor));
            }
        }
        return sb.toString();
    }

    private String grayColorBar(int currentGray) {
        StringBuilder sb = new StringBuilder();
        boolean metCurrent = false;
        for (int i = 0; i < 51; i++) {
            int threshold = i * 255 / 50;
            int grayValue = (threshold << 16) | (threshold << 8) | threshold;
            if (!metCurrent && currentGray <= threshold) {
                sb.append(ANSIHelper.rgbBackground("|", grayValue));
                metCurrent = true;
            } else {
                sb.append(ANSIHelper.rgbBackground(" ", grayValue));
            }
        }
        return sb.toString();
    }

    private void addRGBAKeys(TerminalMenu menu, PrintStream out,Scanner scanner) {
        String redColorBar   = this.rgbColorBar('R', this.rgba[0]);
        String greenColorBar = this.rgbColorBar('G', this.rgba[1]);
        String blueColorBar  = this.rgbColorBar('B', this.rgba[2]);
        String alphaColorBar = this.grayColorBar(this.rgba[3]);
        String redNum      =                 ANSIHelper.red  ("  %3d  ".formatted(this.rgba[0]))  + " " + redColorBar  ;
        String greenNum    =                 ANSIHelper.green("  %3d  ".formatted(this.rgba[1]))  + " " + greenColorBar;
        String blueNum     =                 ANSIHelper.blue ("  %3d  ".formatted(this.rgba[2]))  + " " + blueColorBar ;
        String alphaNum    =                                 ("  %3d  ".formatted(this.rgba[3]))  + " " + alphaColorBar;
        String redSelect   = ANSIHelper.bold(ANSIHelper.red  ("> %3d <".formatted(this.rgba[0]))) + " " + redColorBar  ;
        String greenSelect = ANSIHelper.bold(ANSIHelper.green("> %3d <".formatted(this.rgba[1]))) + " " + greenColorBar;
        String blueSelect  = ANSIHelper.bold(ANSIHelper.blue ("> %3d <".formatted(this.rgba[2]))) + " " + blueColorBar ;
        String alphaSelect = ANSIHelper.bold(                ("> %3d <".formatted(this.rgba[3]))) + " " + alphaColorBar;
        menu.addKey('R', "Red   | " + redNum, this.editNumber(
            menu, out, scanner,
            this.rgba[0], (r) -> this.rgba[0] = r, 0, 255,
            'R',
            "Red   | " + redNum,
            "Red   | " + redSelect,
            MODE_RGBA
        ));
        menu.addKey('G', "Green | " + greenNum, this.editNumber(
            menu, out, scanner,
            this.rgba[1], (g) -> this.rgba[1] = g, 0, 255,
            'G',
            "Green | " + greenNum,
            "Green | " + greenSelect,
            MODE_RGBA
        ));
        menu.addKey('B', "Blue  | " + blueNum, this.editNumber(
            menu, out, scanner,
            this.rgba[2], (b) -> this.rgba[2] = b, 0, 255,
            'B',
            "Blue  | " + blueNum,
            "Blue  | " + blueSelect,
            MODE_RGBA
        ));
        menu.addKey('A', "Alpha | " + alphaNum, this.editNumber(
            menu, out, scanner,
            this.rgba[3], (a) -> this.rgba[3] = a, 0, 255,
            'A',
            "Alpha | " + alphaNum,
            "Alpha | " + alphaSelect,
            MODE_RGBA
        ));
    }

    private String getModeString() {
        return switch (this.mode) {
            case MODE_RGBA -> ANSIHelper.cyan("RGBA");
            case MODE_HSVA -> ANSIHelper.magenta("HSVA");
            default -> ANSIHelper.red("Unknown");
        };
    }

    private void addColorInfo(StringBuilder sb) {
        sb.append("Mode: ").append(this.getModeString()).append("\n");
        sb.append("RGBA: ").append(String.format("%d, %d, %d, %d", rgba[0], rgba[1], rgba[2], rgba[3])).append("\n");
        sb.append("HSVA: ").append(String.format("%d, %d%%, %d%%, %d", hsva[0], hsva[1], hsva[2], hsva[3])).append("\n");
    }

    public int getIntARGB() {
        return (rgba[3] << 24) | (rgba[0] << 16) | (rgba[1] << 8) | rgba[2];
    }

    public int[] getArrayRGBA() {
        return rgba.clone();
    }

    public int[] getArrayHSVA() {
        return hsva.clone();
    }

    private void syncColors(int sourceMode) {
        switch (sourceMode) {
            case MODE_RGBA:
                // Update this.hsva from this.rgba
                this.hsva[3] = this.rgba[3];
                this.hsva[0] = (int) (Math.atan2(Math.sqrt(3) * (this.rgba[1] - this.rgba[2]), 2 * this.rgba[0] - this.rgba[1] - this.rgba[2]) / Math.PI * 180);
                int cMax = Math.max(this.rgba[0], Math.max(this.rgba[1], this.rgba[2]));
                int cMin = Math.min(this.rgba[0], Math.min(this.rgba[1], this.rgba[2]));
                this.hsva[1] = cMax == 0 ? 0 : (cMax - cMin) * 100 / cMax;
                this.hsva[2] = cMax * 100 / 255;
                break;
            case MODE_HSVA:
                // Update this.rgba from this.hsva
                int c = this.hsva[2] * this.hsva[1] / 100;
                int x = c * (100 - Math.abs((this.hsva[0] / 60) % 2 - 1)) / 100;
                int m = this.hsva[2] - c;
                int r1 = 0, g1 = 0, b1 = 0;
                if (this.hsva[0] < 60) {
                    r1 = c;
                    g1 = x;
                } else if (this.hsva[0] < 120) {
                    r1 = x;
                    g1 = c;
                } else if (this.hsva[0] < 180) {
                    g1 = c;
                    b1 = x;
                } else if (this.hsva[0] < 240) {
                    g1 = x;
                    b1 = c;
                } else if (this.hsva[0] < 300) {
                    r1 = x;
                    b1 = c;
                } else {
                    r1 = c;
                    b1 = x;
                }
                this.rgba[0] = (r1 + m) * 255 / 100;
                this.rgba[1] = (g1 + m) * 255 / 100;
                this.rgba[2] = (b1 + m) * 255 / 100;
                this.rgba[3] = this.hsva[3];
                break;
        }
        // Must sync rgba to current color
        this.currentColor = (rgba[3] << 24) | (rgba[0] << 16) | (rgba[1] << 8) | rgba[2];
    }
}
