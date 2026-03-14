package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.Scanner;

public record TerminalMenuContext(
    PrintStream outStream,
    Scanner scanner
) {
}
