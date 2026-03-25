package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StringInput {
    
    private String description;
    private boolean allowEmpty = false;

    public StringInput(String description) {
        this.description = description;
    }

    public StringInput() {
    }

    public String scan(TerminalMenuContext context) {
        return this.scan(context.outStream(), context.scanner());
    }

    public String scan(PrintStream out, Scanner scanner) {
        out.println("==========\n");
        if (this.description != null) {
            out.println(description);
        }

        out.print(ANSIHelper.magenta("\n> "));

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

    public String scanAndRun(TerminalMenuContext context, BiConsumer<String, Consumer<String>> action) {
        String[] error = new String[1];
        String result;
        while (true) {
            result = this.scan(context);
            action.accept(result, errorMessage -> error[0] = errorMessage);
            if (error[0] != null) {
                context.outStream().println(error[0]);
            } else {
                return result;
            }
            error[0] = null;
        }
    }

    public StringInput allowEmpty() {
        this.allowEmpty = true;
        return this;
    }

}
