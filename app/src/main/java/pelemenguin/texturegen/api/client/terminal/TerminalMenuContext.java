package pelemenguin.texturegen.api.client.terminal;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import pelemenguin.texturegen.api.client.ClipboardWrapper;

public record TerminalMenuContext(
    PrintStream outStream,
    InputStream inStream,
    Scanner scanner,
    ClipboardWrapper clipboard
) {
}
