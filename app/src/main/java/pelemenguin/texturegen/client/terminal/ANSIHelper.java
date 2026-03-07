package pelemenguin.texturegen.client.terminal;

import java.io.PrintStream;

public class ANSIHelper {
    private static boolean enableANSI = true;

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
}
