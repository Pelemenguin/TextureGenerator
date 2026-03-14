package pelemenguin.texturegen.client;

import java.io.File;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.generator.ProcessorRegistry;
import pelemenguin.texturegen.client.terminal.TerminalClient;

public class TextureGeneratorClient {

    public File currentWorkspaceFile;
    public TextureGeneratorWorkspace workspace;

    public static void main(String[] args) {
        CommandArgs commandArgs = CommandArgs.parse(args);

        if (commandArgs.disableANSI) {
            ANSIHelper.disableANSI();
        }

        ANSIHelper.clear(System.out);

        ProcessorRegistry.refreshService();

        TerminalClient.INSTANCE.run(new TextureGeneratorClient(), commandArgs);
    }

}
