package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ListEditorMenu;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor;
import pelemenguin.texturegen.api.generator.GeneratorInfo;
import pelemenguin.texturegen.client.TextureGeneratorWorkspace;

public class GeneratorInfoMenu {

    public static final GeneratorInfoMenu INSTANCE = new GeneratorInfoMenu();

    private TextureGeneratorWorkspace workspace;
    private File file;
    GeneratorInfo info;

    public static void loop(TextureGeneratorWorkspace workspace, TerminalMenuContext context, File file) {
        INSTANCE.workspace = workspace;
        INSTANCE.file = file;
        try {
            INSTANCE.info = GeneratorInfo.openFromFile(file);
            if (INSTANCE.info == null) {
                INSTANCE.info = new GeneratorInfo();
            }
        } catch (Throwable t) {
            context.outStream().println("Failed to open generator info: " + ANSIHelper.red(t.getMessage()));
            t.printStackTrace();
            return;
        }
        INSTANCE.loop(context);
    }

    private List<GenerationExecutor.GenerationError> lastExceptions;
    public void loop(TerminalMenuContext context) {
        TerminalMenu menu = new TerminalMenu("Opened: " + ANSIHelper.blue(this.file.toString()))
            .autoUppercase();
        while (true) {
            menu.addKey('B', "Save and back", () -> {})
                .addKey('R', "Run texture generation", () -> {
                    GenerationExecutor.ExecutionResult result = GenerationExecutor.run(this.workspace.inPath, this.workspace.outPath, this.workspace.textures, this.info, context.outStream());

                    if (!result.exceptions().isEmpty()) {
                        this.lastExceptions = result.exceptions();
                    } else {
                        this.lastExceptions = null;
                    }
                })
                .addKey('S', "Suffix", () -> {
                    String result =  new StringInput("Enter suffix: (leave empty to cancel)")
                        .allowEmpty()
                        .scan(context);
                    if (!result.isBlank()) {
                        this.info.suffix = result;
                    }
                })
                .addKey('T', "Texture types", () -> {
                    new ListEditorMenu<>(this.info.types, (t, setter) -> {
                        String type = new StringInput("Enter texture type: (leave empty to cancel)")
                            .allowEmpty().scan(context);
                        if (type != null) {
                            setter.accept(type);
                        }
                    }).loop(context);
                })
                .addKey('F', "Fallbacks", () -> {
                    new ListEditorMenu<>(this.info.fallbacks, (original, setter) -> {
                        String result = new StringInput("Enter fallbak: (Original: %s)\n(Leave empty to cancel)"
                            .formatted(ANSIHelper.blue(String.valueOf(original)))
                        )
                            .allowEmpty()
                            .scan(context);
                        if (!result.isBlank()) {
                            setter.accept(result);
                        }
                    }).loop(context);
                })
                .addKey('P', "Processor sequence", () -> ProcessorSequenceMenu.loop(context, this));

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
                            context.outStream().println("Dumped exceptions to: " + ANSIHelper.blue(outFile.toString()));
                        }
                    } catch (Throwable t) {
                        context.outStream().println("Failed to dump exceptions: " + ANSIHelper.red(t.getMessage()));
                        return;
                    }
                });
            }

            // context.outStream().println("Opened: " + ANSIHelper.blue(file.toString()) + "\n");
            char result = menu
                .updateKeyDescription('S', "Suffix: " + (this.info.suffix == null ? ANSIHelper.red("Unset") : ANSIHelper.blue(this.info.suffix)))
                .scan(context);
            if (result == 'B') {
                break;
            }
        }
        try {
            info.saveToFile(this.file);
        } catch (Throwable t) {
            new TerminalMenu("Failed to save file: " + ANSIHelper.red(t.getMessage()))
                .autoUppercase()
                .addKey('R', "Return to editor", () -> this.loop(context))
                .addKey('F', "Force exit " + ANSIHelper.red("without saving"), () -> {})
                .scan(context);
        }
    }

}

