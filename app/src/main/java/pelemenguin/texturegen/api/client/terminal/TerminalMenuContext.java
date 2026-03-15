package pelemenguin.texturegen.api.client.terminal;

import java.awt.datatransfer.Clipboard;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public record TerminalMenuContext(
    PrintStream outStream,
    InputStream inStream,
    Scanner scanner,
    Clipboard clipboard
) {
}
