package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;

public class ANSIHelper {
    private static boolean enableANSI = true;

    public static boolean ansiEnabled() {
        return enableANSI;
    }

    public static void disableANSI() {
        enableANSI = false;
    }

    public static String red(String content) {
        if (!enableANSI) return content;
        return "\u001B[31m" + content + "\u001B[0m";
    }

    public static String green(String content) {
        if (!enableANSI) return content;
        return "\u001B[32m" + content + "\u001B[0m";
    }

    public static String yellow(String content) {
        if (!enableANSI) return content;
        return "\u001B[33m" + content + "\u001B[0m";
    }

    public static String blue(String content) {
        if (!enableANSI) return content;
        return "\u001B[34m" + content + "\u001B[0m";
    }

    public static String magenta(String content) {
        if (!enableANSI) return content;
        return "\u001B[35m" + content + "\u001B[0m";
    }

    public static String cyan(String content) {
        if (!enableANSI) return content;
        return "\u001B[36m" + content + "\u001B[0m";
    }

    public static String rgb(String content, int color) {
        if (!enableANSI) return content;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return "\u001B[38;2;" + r + ";" + g + ";" + b + "m" + content + "\u001B[0m";
    }

    public static String rgbBackground(String content, int color) {
        if (!enableANSI) return content;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return "\u001B[48;2;" + r + ";" + g + ";" + b + "m" + content + "\u001B[0m";
    }

    public static String rgbWithBackground(String content, int fgColor, int bgColor) {
        if (!enableANSI) return content;
        int r1 = (fgColor >> 16) & 0xFF;
        int g1 = (fgColor >> 8) & 0xFF;
        int b1 = fgColor & 0xFF;
        int r2 = (bgColor >> 16) & 0xFF;
        int g2 = (bgColor >> 8) & 0xFF;
        int b2 = bgColor & 0xFF;
        return "\u001B[38;2;" + r1 + ";" + g1 + ";" + b1 + "m\u001B[48;2;" + r2 + ";" + g2 + ";" + b2 + "m" + content + "\u001B[0m";
    }

    public static void clear(PrintStream out) {
        if (!enableANSI) {
            out.println("\n================\n\n");
            return;
        }
        out.print("\033[H\033[2J");
        out.flush();
    }

    public static void clearLine(PrintStream out) {
        if (!enableANSI) {
            out.println();
            return;
        }
        out.print("\033[2K");
        out.flush();
    }

    public static void cr(PrintStream out) {
        if (!enableANSI) {
            out.println();
            return;
        }
        out.print("\r");
        out.flush();
    }

    public static void moveUp(int lines, PrintStream out) {
        if (!enableANSI) {
            return;
        }
        out.print("\033[" + lines + "A");
        out.flush();
    }

    public static void moveDown(int lines, PrintStream out) {
        if (!enableANSI) {
            return;
        }
        out.print("\033[" + lines + "B");
        out.flush();
    }

    public static void moveTo(int x, int y, PrintStream out) {
        if (!enableANSI) {
            return;
        }
        out.print("\033[" + y + ";" + x + "H");
        out.flush();
    }
}
