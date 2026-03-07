package pelemenguin.texturegen.client;

public class CommandArgs {
    
    public boolean disableANSI = false;

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
                default:
                    System.err.println("Unrecognized argument: " + args);
                    System.exit(1);
                    break;
            }
        }

        return result;
    }

}
