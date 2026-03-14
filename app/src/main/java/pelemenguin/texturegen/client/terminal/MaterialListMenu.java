package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.client.TextureGeneratorWorkspace;

public class MaterialListMenu {

    private static final MaterialListMenu INSTANCE = new MaterialListMenu();

    static void loop(TextureGeneratorWorkspace workspace, Scanner scanner, File folder) {
        INSTANCE._loop(workspace, scanner, folder);
    }

    private static File[] listFile(File folder) {
        File[] files;
        try {
            files = folder.listFiles();
            if (files == null) {
                files = new File[0];
            }
        } catch (SecurityException e) {
            System.out.println("Failed to access the folder: " + ANSIHelper.red(e.getMessage()));
            files = new File[0];
        }
        return files;
    }

    private int page = 0;
    private void _loop(TextureGeneratorWorkspace workspace, Scanner scanner, File folder) {
        if (!folder.exists()) {
            try {
                folder.mkdirs();
                // Ignore the case where it failed to create folder
            } catch (SecurityException e) {
                System.out.println("Failed to create the folder: " + ANSIHelper.red(e.getMessage()));
                return;
            }
        }

        TerminalMenu menu = new TerminalMenu(" ----- GENERATOR LIST --------\nCurrent Folder: " + ANSIHelper.blue(folder.getPath()) + "\n")
            .autoUppercase();

        while (true) {
            File[] files = listFile(folder);

            menu.addKey('B', "Back", () -> {});
            menu.addKey('N', "Create new generator file", () -> this.createNewFile(folder, scanner));
            menu.addKey('F', "Create new generator folder\n", () -> this.createNewFolder(folder, scanner));

            for (int i = 0; i < 10 && i < files.length - page * 10; i++) {
                int index = page * 10 + i;
                File file = files[index];
                menu.addKey((char) ('0' + i), file.getName() + (file.isDirectory() ? ANSIHelper.cyan(" [FOLDER]") : ""), () -> {
                    if (file.isDirectory()) {
                        this._loop(workspace, scanner, file);
                    } else {
                        GeneratorInfoMenu.loop(workspace, scanner, file);
                    }
                });
            }
            if (page > 0) {
                menu.addKey('<', "Previous Page", () -> page--);
            } else if (page < (files.length - 1) / 10) {
                menu.addKey('>', "Next Page", () -> page++);
            }

            char result = menu.scan(System.out, scanner);
            if (result == 'B') {
                break;
            }

            menu.clearKeys();
        }
    }

    private void createNewFile(File curFolder, Scanner scanner) {
        String fileName = new StringInput("Enter file name:\n(Leave empty to cancel)")
            .allowEmpty().scan(System.out, scanner);
        if (fileName.isEmpty()) {
            return;
        }
        // Ensure fileName ends with ".json"
        if (!fileName.endsWith(".json")) {
            fileName += ".generator-info.json";
        }
        Path filePath = curFolder.toPath().resolve(fileName);
        try {
            File file = filePath.toFile();
            if (file.exists()) {
                System.out.println(ANSIHelper.red("File already exists!"));
                return;
            }
            file.createNewFile();
            System.out.println(ANSIHelper.green("File created successfully: " + filePath));
        } catch (Exception e) {
            System.out.println(ANSIHelper.red("Failed to create file: " + e.getMessage()));
        }
    }

    private void createNewFolder(File curFolder, Scanner scanner) {
        String folderName = new StringInput("Enter folder name:\n(Leave empty to cancel)")
            .allowEmpty().scan(System.out, scanner);
        if (folderName.isEmpty()) {
            return;
        }
        Path folderPath = curFolder.toPath().resolve(folderName);
        try {
            File folder = folderPath.toFile();
            if (folder.exists()) {
                System.out.println(ANSIHelper.red("Folder already exists!"));
                return;
            }
            folder.mkdirs();
            System.out.println(ANSIHelper.green("Folder created successfully: " + folderPath));
        } catch (Exception e) {
            System.out.println(ANSIHelper.red("Failed to create folder: " + e.getMessage()));
        }
    }

}
