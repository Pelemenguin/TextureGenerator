package pelemenguin.texturegen.client.terminal;

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
