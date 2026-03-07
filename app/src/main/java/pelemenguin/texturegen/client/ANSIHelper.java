package pelemenguin.texturegen.client;

import java.io.PrintStream;

public class ANSIHelper {
    public static String red(String content) {
        return "\u001B[31m" + content + "\u001B[0m";
    }

    public static String green(String content) {
        return "\u001B[32m" + content + "\u001B[0m";
    }

    public static String yellow(String content) {
        return "\u001B[33m" + content + "\u001B[0m";
    }

    public static String blue(String content) {
        return "\u001B[34m" + content + "\u001B[0m";
    }

    public static String magenta(String content) {
        return "\u001B[35m" + content + "\u001B[0m";
    }

    public static String cyan(String content) {
        return "\u001B[36m" + content + "\u001B[0m";
    }

    public static void clear(PrintStream out) {
        out.print("\033[H\033[2J");
        out.flush();
    }
}
