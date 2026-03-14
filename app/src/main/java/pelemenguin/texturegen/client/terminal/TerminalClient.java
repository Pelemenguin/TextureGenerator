package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import pelemenguin.texturegen.api.generator.TextureInfo;
import pelemenguin.texturegen.client.CommandArgs;
import pelemenguin.texturegen.client.TextureGeneratorClient;
import pelemenguin.texturegen.client.TextureGeneratorWorkspace;

public class TerminalClient {

    public static final TerminalClient INSTANCE = new TerminalClient();

    TextureGeneratorClient client;

    public void run(TextureGeneratorClient client, CommandArgs args) {
        INSTANCE.client = client;

        try (Scanner scanner = new Scanner(System.in)) {
            if (args.workspace == null) {
                new TerminalMenu("""

                        Welcome to Texture Generator terminal!
                        Let's select a workspace!
                        """.stripIndent())
                    .addKey('0', "Exit", () -> System.exit(0))
                    .addKey('1', "Create a new workspace", () -> this.createNewWorkspace(scanner))
                    .addKey('2', "Open an existing workspace", () -> openExistingWorkspace(scanner))
                    .scan(System.out, scanner);
            } else {
                try {
                    client.currentWorkspaceFile = args.workspace;
                    client.workspace = TextureGeneratorWorkspace.openFromFile(args.workspace);
                } catch (FileNotFoundException e) {
                    System.out.println("Workspace file not found: " + client.workspace);
                    System.exit(1);
                } catch (Throwable e) {
                    System.out.println("Failed to open workspace: " + e.getMessage());
                    System.exit(1);
                }
            }

            if (client.workspace == null) return;

            this.inWorkspaceLoop(scanner);
        }
    }

    private void inWorkspaceLoop(Scanner scanner) {
        TerminalMenu menu = new TerminalMenu(
                "Current workspace: " + client.currentWorkspaceFile + "\n"
            )
                .addKey('E', "Exit", () -> {})
                .addKey('I', "", () -> this.selectInPath(scanner))
                .addKey('O', "", () -> this.selectOutPath(scanner))
                .addKey('S', "", () -> this.selectGeneratorsPath(scanner))
                .addKey('U', "Edit texture infos", () -> this.enterTextureInfosEditingLoop(scanner))
                .addKey('M', "Open texture generators folder", () -> this.enterMaterialListLoop(scanner))
                .addKey('R', "Refresh", () -> {}) // Doing nothing will refresh the menu and thus update the descriptions.
                .autoUppercase();
        while (true) {
            menu.updateKeyDescription('I', "Input assets folder. Current: " + (client.workspace.inPath == null ? ANSIHelper.red("NULL! Select one now.") : ANSIHelper.blue(client.workspace.inPath.toString())));
            menu.updateKeyDescription('O', "Output folder. Current: " + (client.workspace.outPath == null ? ANSIHelper.red("NULL! Select one now.") : ANSIHelper.blue(client.workspace.outPath.toString())));
            menu.updateKeyDescription('S', "Set the root folder of texture generators. Current: " + ANSIHelper.blue(client.workspace.generatorsPath.toString()));
            char input = menu.scan(System.out, scanner);
            if (input == 'e' || input == 'E') {
                break;
            }
        }
    }

    private void enterTextureInfosEditingLoop(Scanner scanner) {
        try {
            this.client.workspace.reloadFromFile(this.client.currentWorkspaceFile);
        } catch (Throwable t) {
            System.out.println("Failed to reload workspace file, using cached texture info list: " + ANSIHelper.red(t.getMessage()));
        }
        new ListEditorMenu<>(this.client.workspace.textures, (info, infoSetter) -> {
            new FieldEditorMenu<>(info)
                .<Path>specify("path", 'P', "Texture path", (value, setter) -> {
                    String newPathStr = new StringInput("Enter the texture path:\n(Leave empty to cancel)").allowEmpty().scan(System.out, scanner);
                    if (newPathStr.isBlank()) {
                        return;
                    }
                    setter.accept(Path.of(newPathStr));
                })
                .specify("allowAnimated", 'A', "Allow animated")
                .specify("skipVariant", 'S', "Skip variant")
                .<Set<String>>specify("types", 'T', "Texture types", (value, setter) -> {
                    ArrayList<String> list = new ArrayList<>(value);
                    new ListEditorMenu<>(list, (original, stringSetter) -> {
                        String newString = new StringInput("Enter the texture type:\n(Leave empty to cancel)")
                            .allowEmpty()
                            .scan(System.out, scanner);
                        if (newString.isBlank()) {
                            return;
                        }
                        stringSetter.accept(newString);
                    }, () -> "[EMPTY]").loop(System.out, scanner);
                    value.clear();
                    value.addAll(list);
                })
                .loop(System.out, scanner);
            infoSetter.accept(info);
        }, TextureInfo::new)
            .strigifier(t -> t.getPath() == null ? "[NULL]" : t.getPath().toString())
            .extraKeys(menu -> {
                menu.addKey('R', "Read texture infos from input assets folder", () -> this.readTextureInfos(scanner));
            })
            .loop(System.out, scanner);
        try {
            this.client.workspace.saveToFile(this.client.currentWorkspaceFile);
        } catch (IOException e) {
            System.out.println("Failed to save workspace file: " + e.getMessage());
        }
    }

    private void readTextureInfos(Scanner scanner) {
        char c = new TerminalMenu("Sure to read texture infos from files? This will " + ANSIHelper.red("overwrite") + " the current texture info list in the workspace file!")
            .addKey('Y', "Yes, read them.", () -> {})
            .addKey('N', "No, cancel.", () -> {})
            .autoUppercase()
            .scan(System.out, scanner);
        if (c != 'Y') {
            return;
        }
        try {
            this.client.workspace.readTextureInfos();
            this.client.workspace.saveToFile(this.client.currentWorkspaceFile);
        } catch (Throwable t) {
            System.out.println("Failed to read texture infos: " + ANSIHelper.red(t.getMessage()));
            return;
        }
        System.out.println(ANSIHelper.green("Successfully read texture infos from input assets folder!"));
    }

    private void selectInPath(Scanner scanner) {
        String inPathStr = new StringInput("Enter the input assets folder path:\n(Leave empty to cancel)").allowEmpty().scan(System.out, scanner);
        if (inPathStr.isBlank()) return;
        try {
            Path inPath = Path.of(inPathStr);
            client.workspace.inPath = inPath;
            client.workspace.saveToFile(client.currentWorkspaceFile);
        } catch (InvalidPathException e) {
            System.out.println("Invalid path. Please try again.");
            selectInPath(scanner);
            return;
        } catch (UnsupportedOperationException e) {
            System.out.println("Path is not associated with the default provider.");
            return;
        } catch (JsonIOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectInPath(scanner);
            return;
        } catch (IOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectInPath(scanner);
            return;
        }
    }

    private void selectOutPath(Scanner scanner) {
        String outPathStr = new StringInput("Enter the output folder path:\n(Leave empty to cancel)").allowEmpty().scan(System.out, scanner);
        if (outPathStr.isBlank()) return;
        try {
            Path outputPath = Path.of(outPathStr);
            client.workspace.outPath = outputPath;
            client.workspace.saveToFile(client.currentWorkspaceFile);
        } catch (InvalidPathException e) {
            System.out.println("Invalid path. Please try again.");
            selectOutPath(scanner);
            return;
        } catch (UnsupportedOperationException e) {
            System.out.println("Path is not associated with the default provider.");
            return;
        } catch (JsonIOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectOutPath(scanner);
            return;
        } catch (IOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectOutPath(scanner);
            return;
        }
    }

    private void selectGeneratorsPath(Scanner scanner) {
        String generatorsPathStr = new StringInput("Enter the root folder of texture generators:\n(Leave empty to cancel)").allowEmpty().scan(System.out, scanner);
        if (generatorsPathStr.isBlank()) return;
        try {
            Path generatorsPath = Path.of(generatorsPathStr);
            client.workspace.generatorsPath = generatorsPath;
            client.workspace.saveToFile(client.currentWorkspaceFile);
        } catch (InvalidPathException e) {
            System.out.println("Invalid path. Please try again.");
            selectGeneratorsPath(scanner);
            return;
        } catch (UnsupportedOperationException e) {
            System.out.println("Path is not associated with the default provider.");
            return;
        } catch (JsonIOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectGeneratorsPath(scanner);
            return;
        } catch (IOException e) {
            System.out.println("Error saving workspace file. Please try again.");
            selectGeneratorsPath(scanner);
            return;
        }
    }

    private void enterMaterialListLoop(Scanner scanner) {
        try {
            MaterialListMenu.loop(this.client.workspace, scanner, this.client.workspace.generatorsPath.toFile());
        } catch (UnsupportedOperationException e) {
            // Ignore
        }
    }

    private void createNewWorkspace(Scanner scanner) {
        System.out.println("Current directory: " + System.getProperty("user.dir"));

        String workspacePath = new StringInput("Enter the workspace file path:\n(Leave empty to exit)").allowEmpty().scan(System.out, scanner);
        if (workspacePath.isBlank()) return;
        Path currentWorkspacePath;
        try {
            if (!workspacePath.endsWith(".texturegen-workspace.json")) {
                workspacePath += ".texturegen-workspace.json";
            }
            currentWorkspacePath = Path.of(System.getProperty("user.dir")).resolve(workspacePath);

            File currentWorkspaceFile = currentWorkspacePath.toFile();
            if (currentWorkspaceFile.exists()) {
                TerminalMenu menu = new TerminalMenu(
                        "A file already exists at the specified path. Do you want to overwrite it?"
                    )
                    .addKey('Y', "Yes, overwrite it.", () -> {})
                    .addKey('N', "No, let me enter another path.", () -> createNewWorkspace(scanner))
                    .autoUppercase();
                char result = menu.scan(System.out, scanner);
                if (result == 'n' || result == 'N') {
                    return;
                }
            }

            this.client.currentWorkspaceFile = currentWorkspaceFile;

            this.client.workspace = new TextureGeneratorWorkspace();
            this.client.workspace.saveToFile(this.client.currentWorkspaceFile);
        } catch (InvalidPathException e) {
            System.out.println("Invalid path. Please try again.");
            createNewWorkspace(scanner);
            return;
        } catch (UnsupportedOperationException e) {
            System.out.println("Path is not associated with the default provider. Exiting...");
            System.exit(1);
            return;
        } catch (JsonIOException e) {
            System.out.println("Error reading workspace file. Please try again.");
            createNewWorkspace(scanner);
            return;
        } catch (IOException e) {
            System.out.println("Error opening workspace file. Please try again.");
            createNewWorkspace(scanner);
            return;
        }
    }

    private void openExistingWorkspace(Scanner scanner) {
        String workspacePath = new StringInput("Enter the workspace file path:\n(Leave empty to exit)").allowEmpty().scan(System.out, scanner);
        if (workspacePath.isBlank()) return;
        Path currentWorkspacePath;
        try {
            currentWorkspacePath = Path.of(workspacePath);
            this.client.currentWorkspaceFile = currentWorkspacePath.toFile();

            this.client.workspace = TextureGeneratorWorkspace.openFromFile(this.client.currentWorkspaceFile);
        } catch (InvalidPathException e) {
            System.out.println("Invalid path. Please try again.");
            openExistingWorkspace(scanner);
            return;
        } catch (UnsupportedOperationException e) {
            System.out.println("Path is not associated with the default provider. Exiting...");
            System.exit(1);
            return;
        } catch (FileNotFoundException e) {
            System.out.println("Workspace file not found. Please try again.");
            openExistingWorkspace(scanner);
            return;
        } catch (JsonSyntaxException e) {
            e.printStackTrace(System.out);
            System.out.println("Syntax error found in JSON file. Please try again.");
            openExistingWorkspace(scanner);
            return;
        } catch (JsonIOException e) {
            System.out.println("Error reading workspace file. Please try again.");
            openExistingWorkspace(scanner);
            return;
        } catch (IOException e) {
            System.out.println("Error opening workspace file. Please try again.");
            openExistingWorkspace(scanner);
            return;
        } 
    }
}
