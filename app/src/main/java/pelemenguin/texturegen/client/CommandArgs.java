package pelemenguin.texturegen.client;

import java.io.File;
import java.nio.file.Path;

import pelemenguin.texturegen.client.terminal.ANSIHelper;

public class CommandArgs {
    
    public boolean disableANSI = false;
    public File workspace = null;

    private CommandArgs() {
    }

    public static CommandArgs parse(String[] args) {
        CommandArgs result = new CommandArgs();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--no-ansi":
                    result.disableANSI = true;
                    break;
                case "--workspace", "-w":
                    try {
                        i++;
                        result.workspace = Path.of(args[i]).toFile();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Expect workspace file path after " + arg);
                    } catch (Throwable t) {
                        System.out.println("Failed to open workspace: " + ANSIHelper.red(t.getMessage()));
                    }
                    break;
                default:
                    System.err.println("Unrecognized argument: " + args);
                    System.exit(1);
                    break;
            }
        }

        return result;
    }

}
