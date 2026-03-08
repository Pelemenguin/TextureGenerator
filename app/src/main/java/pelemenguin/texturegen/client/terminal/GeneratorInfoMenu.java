package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.util.Scanner;

import pelemenguin.texturegen.api.generator.GeneratorInfo;

public class GeneratorInfoMenu {

    public static final GeneratorInfoMenu INSTANCE = new GeneratorInfoMenu();

    private File file;
    private GeneratorInfo info;

    public static void loop(Scanner scanner, File file) {
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

    public void loop(Scanner scanner) {
        TerminalMenu menu = new TerminalMenu()
            .autoUppercase()
            .addKey('B', "Save and back", () -> {})
            .addKey('R', "Run texture generation", () -> {
                // TODO: Implement this
            })
            .addKey('S', "Suffix", () -> {
                String result =  new StringInput("Enter suffix: (leave empty to caccel)")
                    .allowEmpty()
                    .scan(System.out, scanner);
                if (!result.isBlank()) {
                    this.info.suffix = result;
                }
            });
        while (true) {
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

