package pelemenguin.texturegen.client.terminal;

import java.io.PrintStream;
import java.util.Scanner;

public class StringInput {
    
    private String description;
    private boolean allowEmpty = false;

    public StringInput(String description) {
        this.description = description;
    }

    public StringInput() {
    }

    public String scan(PrintStream out, Scanner scanner) {
        if (this.description != null) {
            out.println(description);
        }

        out.print("\n> ");

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (!allowEmpty && line.isBlank()) {
                out.println(ANSIHelper.red("Input cannot be empty. Please try again."));
                out.print(ANSIHelper.magenta("\n> "));
                continue;
            }
            ANSIHelper.clear(out);
            return line;
        }

        ANSIHelper.clear(out);
        return null;
    }

    public StringInput allowEmpty() {
        this.allowEmpty = true;
        return this;
    }

}
