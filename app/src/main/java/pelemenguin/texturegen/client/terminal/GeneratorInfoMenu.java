package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import pelemenguin.texturegen.api.generator.GenerationExecutor;
import pelemenguin.texturegen.api.generator.GeneratorInfo;
import pelemenguin.texturegen.client.TextureGeneratorWorkspace;

public class GeneratorInfoMenu {

    public static final GeneratorInfoMenu INSTANCE = new GeneratorInfoMenu();

    private TextureGeneratorWorkspace workspace;
    private File file;
    private GeneratorInfo info;

    public static void loop(TextureGeneratorWorkspace workspace, Scanner scanner, File file) {
        INSTANCE.workspace = workspace;
        INSTANCE.file = file;
        try {
            INSTANCE.info = GeneratorInfo.openFromFile(file);
            if (INSTANCE.info == null) {
                INSTANCE.info = new GeneratorInfo();
            }
        } catch (Throwable t) {
            System.out.println("Failed to open generator info: " + ANSIHelper.red(t.getMessage()));
        }
        INSTANCE.loop(scanner);
    }

    private List<GenerationExecutor.GenerationError> lastExceptions;
    public void loop(Scanner scanner) {
        TerminalMenu menu = new TerminalMenu()
            .autoUppercase();
        while (true) {
            menu.addKey('B', "Save and back", () -> {})
                .addKey('R', "Run texture generation", () -> {
                    GenerationExecutor.ExecutionResult result = GenerationExecutor.run(this.workspace.inPath, this.workspace.outPath, this.workspace.textures, this.info, System.out);

                    if (!result.exceptions().isEmpty()) {
                        this.lastExceptions = result.exceptions();
                    } else {
                        this.lastExceptions = null;
                    }
                })
                .addKey('S', "Suffix", () -> {
                    String result =  new StringInput("Enter suffix: (leave empty to caccel)")
                        .allowEmpty()
                        .scan(System.out, scanner);
                    if (!result.isBlank()) {
                        this.info.suffix = result;
                    }
                });

            if (this.lastExceptions != null) {
                menu.addKey('E', "Dump exceptions (" + this.lastExceptions.size() + ")", () -> {
                    try {
                        File outFile = this.workspace.outPath.resolve("exceptions.txt").toFile();

                        try (PrintStream stream = new PrintStream(outFile)) {
                            stream.println("Exceptions during generation:\n");
                            for (GenerationExecutor.GenerationError e : this.lastExceptions) {
                                e.printStackTrace(stream);
                                stream.println();
                            }
                            System.out.println("Dumped exceptions to: " + ANSIHelper.blue(outFile.toString()));
                        }
                    } catch (Throwable t) {
                        System.out.println("Failed to dump exceptions: " + ANSIHelper.red(t.getMessage()));
                        return;
                    }
                });
            }

            System.out.println("Opened: " + ANSIHelper.blue(file.toString()) + "\n");
            char result = menu
                .updateKeyDescription('S', "Suffix: " + (this.info.suffix == null ? ANSIHelper.red("Unset") : ANSIHelper.blue(this.info.suffix)))
                .scan(System.out, scanner);
            if (result == 'B') {
                break;
            }
        }
        try {
            info.saveToFile(this.file);
        } catch (Throwable t) {
            new TerminalMenu("Failed to save file: " + ANSIHelper.red(t.getMessage()))
                .autoUppercase()
                .addKey('R', "Return to editor", () -> this.loop(scanner))
                .addKey('F', "Force exit " + ANSIHelper.red("without saving"), () -> {})
                .scan(System.out, scanner);
        }
    }

}

