package pelemenguin.texturegen.client;

import java.io.File;

import pelemenguin.texturegen.client.terminal.TerminalClient;

public class TextureGeneratorClient {

    public File currentWorkspaceFile;
    public TextureGeneratorWorkspace workspace;

    public static void main(String[] args) {
        TerminalClient.INSTANCE.run(new TextureGeneratorClient());
    }

}
