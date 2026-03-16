package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.IntConsumer;

public class ColorPickerMenu {

    private int[] rgba = new int[4];
    private int[] hsva = new int[4];
    // TODO: More modes and implement hsva editing
    private byte mode = MODE_RGBA;
    private int originalColor = 0xFFFFFFFF;
    private int currentColor = 0xFFFFFFFF;

    public static final byte MODE_RGBA = 0;
    public static final byte MODE_HSVA = 1;

    public ColorPickerMenu(int original) {
        this.rgba[0] = (original >> 16) & 0xFF;
        this.rgba[1] = (original >> 8) & 0xFF;
        this.rgba[2] = original & 0xFF;
        this.rgba[3] = (original >> 24) & 0xFF;
        this.originalColor = original;
        this.currentColor = original;

        this.syncColors(MODE_RGBA);
    }

    public ColorPickerMenu() {
        this(0xFFFFFFFF);
    }

    public ColorPickerMenu loop(TerminalMenuContext context) {
        return loop(context.outStream(), context.scanner());
    }

    public ColorPickerMenu loop(PrintStream out, Scanner scanner) {
        TerminalMenu menu = new TerminalMenu().autoUppercase();

        while (true) {
            menu.clearKeys();
            StringBuilder sb = new StringBuilder("Color Picker\n\n");
            this.addColorPreview(sb);
            this.addColorInfo(sb);
            String curHex = ANSIHelper.bold(Integer.toHexString(this.currentColor).toUpperCase());
            menu.addKey('M', "Switch color mode", () -> {
                this.mode = switch (this.mode) {
                    case MODE_RGBA -> MODE_HSVA;
                    case MODE_HSVA -> MODE_RGBA;
                    default -> MODE_RGBA;
                };
            });
            menu.addKey('X', "ARGB Hex:   " + curHex, () -> {
                menu.updateKeyDescription('X', "ARGB Hex: > " + curHex + " <");
                String newHex = new StringInput(menu.getDisplayContent()).allowEmpty().scan(out, scanner);
                if (!newHex.isBlank()) {
                    try {
                        int newColor = Integer.parseUnsignedInt(newHex, 16);
                        this.rgba[0] = (newColor >> 16) & 0xFF;
                        this.rgba[1] = (newColor >> 8) & 0xFF;
                        this.rgba[2] = newColor & 0xFF;
                        this.rgba[3] = (newColor >> 24) & 0xFF;
                        this.syncColors(MODE_RGBA);
                    } catch (NumberFormatException e) {
                        out.println(ANSIHelper.red(
                                "Invalid hex color. Please enter a valid ARGB hex string, e.g. FFFF0000 for red."));
                    }
                }
            });
            menu.addKey('!', "Reset to old color\n", () -> {
                this.rgba[0] = (this.originalColor >> 16) & 0xFF;
                this.rgba[1] = (this.originalColor >> 8) & 0xFF;
                this.rgba[2] = this.originalColor & 0xFF;
                this.rgba[3] = (this.originalColor >> 24) & 0xFF;
                this.syncColors(MODE_RGBA);
            });
            menu.addKey('-', "Back");
            if (this.mode == MODE_RGBA) {
                this.addRGBAKeys(menu, out, scanner);
            } else if (this.mode == MODE_HSVA) {
                this.addHSVAKeys(menu, out, scanner);
            }
            menu.updateDescription(sb.toString());

            char c = menu.scan(out, scanner);
            if (c == '-') {
                break;
            }
        }

        return this;
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
            int syncMode) {
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
        for (int i = 0; i < 52; i++) {
            int threshold = i * 255 / 51;
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

    private String hsvColorBar(char hOrSOrV, int currentValue) {
        StringBuilder sb = new StringBuilder();
        boolean metCurrent = false;
        int max = switch (hOrSOrV) {
            case 'H' -> 359;
            case 'S', 'V' -> 100;
            default -> 255;
        };
        for (int i = 0; i < 52; i++) {
            int threshold = i * max / 51;
            int backgroundColor = switch (hOrSOrV) {
                case 'H' -> hsvToRgb(threshold * 360 / max, 100, 100);
                case 'S' -> hsvToRgb(this.hsva[0], threshold * 100 / max, this.hsva[2]);
                case 'V' -> hsvToRgb(this.hsva[0], this.hsva[1], threshold * 100 / max);
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
        for (int i = 0; i < 52; i++) {
            int threshold = i * 255 / 51;
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

    private String refrenceBar0to255() {
        return "|0   |25  |50  |75  |100 |125 |150 |175 |200 |225  |255";
    }

    private void addRGBAKeys(TerminalMenu menu, PrintStream out, Scanner scanner) {
        String redColorBar = this.rgbColorBar('R', this.rgba[0]);
        String greenColorBar = this.rgbColorBar('G', this.rgba[1]);
        String blueColorBar = this.rgbColorBar('B', this.rgba[2]);
        String alphaColorBar = this.grayColorBar(this.rgba[3]);
        String redNum = ANSIHelper.red("  %3d  ".formatted(this.rgba[0])) + " " + redColorBar;
        String greenNum = ANSIHelper.green("  %3d  ".formatted(this.rgba[1])) + " " + greenColorBar;
        String blueNum = ANSIHelper.blue("  %3d  ".formatted(this.rgba[2])) + " " + blueColorBar;
        String alphaNum = ("  %3d  ".formatted(this.rgba[3])) + " " + alphaColorBar;
        String redSelect = ANSIHelper.bold(ANSIHelper.red("> %3d <".formatted(this.rgba[0]))) + " " + redColorBar;
        String greenSelect = ANSIHelper.bold(ANSIHelper.green("> %3d <".formatted(this.rgba[1]))) + " " + greenColorBar;
        String blueSelect = ANSIHelper.bold(ANSIHelper.blue("> %3d <".formatted(this.rgba[2]))) + " " + blueColorBar;
        String alphaSelect = ANSIHelper.bold(("> %3d <".formatted(this.rgba[3]))) + " " + alphaColorBar;
        if (ANSIHelper.ansiEnabled())
            menu.updateKeyDescription('-', "Back            " + refrenceBar0to255());
        menu.addKey('R', "Red   | " + redNum, this.editNumber(
                menu, out, scanner,
                this.rgba[0], (r) -> this.rgba[0] = r, 0, 255,
                'R',
                "Red   | " + redNum,
                "Red   | " + redSelect,
                MODE_RGBA));
        menu.addKey('G', "Green | " + greenNum, this.editNumber(
                menu, out, scanner,
                this.rgba[1], (g) -> this.rgba[1] = g, 0, 255,
                'G',
                "Green | " + greenNum,
                "Green | " + greenSelect,
                MODE_RGBA));
        menu.addKey('B', "Blue  | " + blueNum, this.editNumber(
                menu, out, scanner,
                this.rgba[2], (b) -> this.rgba[2] = b, 0, 255,
                'B',
                "Blue  | " + blueNum,
                "Blue  | " + blueSelect,
                MODE_RGBA));
        menu.addKey('A', "Alpha | " + alphaNum, this.editNumber(
                menu, out, scanner,
                this.rgba[3], (a) -> this.rgba[3] = a, 0, 255,
                'A',
                "Alpha | " + alphaNum,
                "Alpha | " + alphaSelect,
                MODE_RGBA));
    }

    public void addHSVAKeys(TerminalMenu menu, PrintStream out, Scanner scanner) {
        String hueColorBar = this.hsvColorBar('H', this.hsva[0]);
        String saturationColorBar = this.hsvColorBar('S', this.hsva[1]);
        String valueColorBar = this.hsvColorBar('V', this.hsva[2]);
        String alphaColorBar = this.grayColorBar(this.hsva[3]);
        String hueNum = ANSIHelper.magenta("  %3d\u00B0  ".formatted(this.hsva[0])) + " " + hueColorBar;
        String saturationNum = ANSIHelper.cyan("  %3d%%  ".formatted(this.hsva[1])) + " " + saturationColorBar;
        String valueNum = ANSIHelper.yellow("  %3d%%  ".formatted(this.hsva[2])) + " " + valueColorBar;
        String alphaNum = ("  %3d   ".formatted(this.hsva[3])) + " " + alphaColorBar;
        String hueSelect = ANSIHelper.bold(ANSIHelper.magenta("> %3d\u00B0 <".formatted(this.hsva[0]))) + " "
                + hueColorBar;
        String saturationSelect = ANSIHelper.bold(ANSIHelper.cyan("> %3d%% <".formatted(this.hsva[1]))) + " "
                + saturationColorBar;
        String valueSelect = ANSIHelper.bold(ANSIHelper.yellow("> %3d%% <".formatted(this.hsva[2]))) + " "
                + valueColorBar;
        String alphaSelect = ANSIHelper.bold(("> %3d <".formatted(this.hsva[3]))) + " " + alphaColorBar;
        menu.addKey('H', "Hue        | " + hueNum, this.editNumber(
                menu, out, scanner,
                this.hsva[0], (h) -> this.hsva[0] = h, 0, 359,
                'H',
                "Hue        | " + hueNum,
                "Hue        | " + hueSelect,
                MODE_HSVA));
        menu.addKey('S', "Saturation | " + saturationNum, this.editNumber(
                menu, out, scanner,
                this.hsva[1], (s) -> this.hsva[1] = s, 0, 100,
                'S',
                "Saturation | " + saturationNum,
                "Saturation | " + saturationSelect,
                MODE_HSVA));
        menu.addKey('V', "Value      | " + valueNum, this.editNumber(
                menu, out, scanner,
                this.hsva[2], (v) -> this.hsva[2] = v, 0, 100,
                'V',
                "Value      | " + valueNum,
                "Value      | " + valueSelect,
                MODE_HSVA));
        menu.addKey('A', "Alpha      | " + alphaNum, this.editNumber(
                menu, out, scanner,
                this.hsva[3], (a) -> this.hsva[3] = a, 0, 255,
                'A',
                "Alpha      | " + alphaNum,
                "Alpha      | " + alphaSelect,
                MODE_HSVA));
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
        sb.append("HSVA: ").append(String.format("%d\u00B0, %d%%, %d%%, %d", hsva[0], hsva[1], hsva[2], hsva[3]))
                .append("\n");
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

    private int hsvToRgb(int h, int s, int v) {
        double hue = (h % 360) / 360.0;
        double saturation = Math.max(0, Math.min(100, s)) / 100.0;
        double value = Math.max(0, Math.min(100, v)) / 100.0;

        int r, g, b;

        if (saturation == 0) {
            // 灰色
            r = g = b = (int) Math.round(value * 255);
        } else {
            double hi = Math.floor(hue * 6);
            double f = hue * 6 - hi;
            double p = value * (1 - saturation);
            double q = value * (1 - f * saturation);
            double t = value * (1 - (1 - f) * saturation);

            int region = (int) hi % 6;
            switch (region) {
                case 0:
                    r = (int) Math.round(value * 255);
                    g = (int) Math.round(t * 255);
                    b = (int) Math.round(p * 255);
                    break;
                case 1:
                    r = (int) Math.round(q * 255);
                    g = (int) Math.round(value * 255);
                    b = (int) Math.round(p * 255);
                    break;
                case 2:
                    r = (int) Math.round(p * 255);
                    g = (int) Math.round(value * 255);
                    b = (int) Math.round(t * 255);
                    break;
                case 3:
                    r = (int) Math.round(p * 255);
                    g = (int) Math.round(q * 255);
                    b = (int) Math.round(value * 255);
                    break;
                case 4:
                    r = (int) Math.round(t * 255);
                    g = (int) Math.round(p * 255);
                    b = (int) Math.round(value * 255);
                    break;
                case 5:
                default:
                    r = (int) Math.round(value * 255);
                    g = (int) Math.round(p * 255);
                    b = (int) Math.round(q * 255);
                    break;
            }
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return (r << 16) | (g << 8) | b;
    }

    private void syncColors(int sourceMode) {
        switch (sourceMode) {
            case MODE_RGBA: {
                // Update this.hsva from this.rgba
                double r = Math.max(0, Math.min(255, this.rgba[0])) / 255.0;
                double g = Math.max(0, Math.min(255, this.rgba[1])) / 255.0;
                double b = Math.max(0, Math.min(255, this.rgba[2])) / 255.0;

                double max = Math.max(r, Math.max(g, b));
                double min = Math.min(r, Math.min(g, b));
                double delta = max - min;

                double h = 0.0;  // 色相（度）
                double s = 0.0;  // 饱和度 [0,1]
                double v = max;  // 明度 [0,1]

                if (delta != 0) {
                    s = delta / max;

                    if (max == r) {
                        h = 60 * ((g - b) / delta);
                        if (g < b) {
                            h += 360;
                        }
                    } else if (max == g) {
                        h = 60 * ((b - r) / delta + 2);
                    } else { // max == b
                        h = 60 * ((r - g) / delta + 4);
                    }
                }
                // 当 delta == 0 时，s = 0，h 保持为 0（任意值均可，通常取 0）

                // 四舍五入到整数，并处理边界
                int hInt = (int) Math.round(h);
                if (hInt == 360) hInt = 0;          // 360° 等价于 0°
                int sInt = (int) Math.round(s * 100);
                int vInt = (int) Math.round(v * 100);

                // 确保在范围内（四舍五入可能产生 101，但极罕见）
                sInt = Math.max(0, Math.min(100, sInt));
                vInt = Math.max(0, Math.min(100, vInt));

                this.hsva[0] = hInt;
                this.hsva[1] = sInt;
                this.hsva[2] = vInt;
                this.hsva[3] = this.rgba[3];
                break;
            }
            case MODE_HSVA: {
                // Update this.rgba from this.hsva
                int h = this.hsva[0];
                int s = this.hsva[1];
                int v = this.hsva[2];
                int rgb = hsvToRgb(h, s, v);
                this.rgba[0] = (rgb >> 16) & 0xFF;
                this.rgba[1] = (rgb >> 8) & 0xFF;
                this.rgba[2] = rgb & 0xFF;
                this.rgba[3] = this.hsva[3];
                break;
            }
        }
        // Must sync rgba to current color
        this.currentColor = (rgba[3] << 24) | (rgba[0] << 16) | (rgba[1] << 8) | rgba[2];
    }
}
