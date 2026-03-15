package pelemenguin.texturegen.client.terminal;

import java.io.File;
import java.nio.file.Path;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.client.TextureGeneratorWorkspace;

public class MaterialListMenu {

    private static final MaterialListMenu INSTANCE = new MaterialListMenu();

    static void loop(TextureGeneratorWorkspace workspace, TerminalMenuContext context, File folder) {
        INSTANCE._loop(workspace, context, folder);
    }

    private static File[] listFile(File folder, TerminalMenuContext context) {
        File[] files;
        try {
            files = folder.listFiles();
            if (files == null) {
                files = new File[0];
            }
        } catch (SecurityException e) {
            context.outStream().println("Failed to access the folder: " + ANSIHelper.red(e.getMessage()));
            files = new File[0];
        }
        return files;
    }

    private int page = 0;
    private boolean deleteMode = false;
    private void _loop(TextureGeneratorWorkspace workspace, TerminalMenuContext context, File folder) {
        if (!folder.exists()) {
            try {
                folder.mkdirs();
                // Ignore the case where it failed to create folder
            } catch (SecurityException e) {
                context.outStream().println("Failed to create the folder: " + ANSIHelper.red(e.getMessage()));
                return;
            }
        }

        TerminalMenu menu = new TerminalMenu(" ----- GENERATOR LIST --------\nCurrent Folder: " + ANSIHelper.blue(folder.getPath()) + "\n")
            .autoUppercase();

        while (true) {
            File[] files = listFile(folder, context);

            menu.addKey('B', "Back", () -> {});
            menu.addKey('N', "Create new generator file", () -> this.createNewFile(folder, context));
            menu.addKey('F', "Create new generator folder", () -> this.createNewFolder(folder, context));
            menu.addKey('D', this.deleteMode ? "Cancel deletion\n" : "Delete a generator file/folder\n", () -> {
                this.deleteMode = !this.deleteMode;
            });

            this.listFiles(menu, files, workspace, context);

            char result = menu.scan(context);
            if (result == 'B') {
                break;
            }

            menu.clearKeys();
        }
    }

    private void listFiles(TerminalMenu menu, File[] files, TextureGeneratorWorkspace workspace, TerminalMenuContext context) {
        for (int i = 0; i < 10 && i < files.length - page * 10; i++) {
            int index = page * 10 + i;
            File file = files[index];
            menu.addKey((char) ('0' + (i + 1) % 10),  (file.isDirectory() ? ANSIHelper.cyan("[FOLDER] ") : "") + (this.deleteMode ? ANSIHelper.red(file.getName()) : file.getName()), () -> {
                if (this.deleteMode) {
                    new TerminalMenu("Are you sure you want to delete " + ANSIHelper.red(file.getName()) + "?\n" + ANSIHelper.red("This action cannot be undone!"))
                        .autoUppercase()
                        .addKey('Y', "Yes", () -> {
                            if (file.isDirectory()) {
                                this.deleteDirectory(file);
                            } else {
                                file.delete();
                            }
                            this.deleteMode = false;
                        })
                        .addKey('N', "No", () -> {})
                        .scan(context);
                    return;
                }
                if (file.isDirectory()) {
                    this._loop(workspace, context, file);
                } else {
                    GeneratorInfoMenu.loop(workspace, context, file);
                }
            });
        }
        if (page > 0) {
            menu.addKey('<', "Previous Page", () -> page--);
        } else if (page < (files.length - 1) / 10) {
            menu.addKey('>', "Next Page", () -> page++);
        }
    }

    private void deleteDirectory(File target) {
        File[] contents = target.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        target.delete();
    }

    private void createNewFile(File curFolder, TerminalMenuContext context) {
        String fileName = new StringInput("Enter file name:\n(Leave empty to cancel)")
            .allowEmpty().scan(context);
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
                context.outStream().println(ANSIHelper.red("File already exists!"));
                return;
            }
            file.createNewFile();
            context.outStream().println(ANSIHelper.green("File created successfully: " + filePath));
        } catch (Exception e) {
            context.outStream().println(ANSIHelper.red("Failed to create file: " + e.getMessage()));
        }
    }

    private void createNewFolder(File curFolder, TerminalMenuContext context) {
        String folderName = new StringInput("Enter folder name:\n(Leave empty to cancel)")
            .allowEmpty().scan(context);
        if (folderName.isEmpty()) {
            return;
        }
        Path folderPath = curFolder.toPath().resolve(folderName);
        try {
            File folder = folderPath.toFile();
            if (folder.exists()) {
                context.outStream().println(ANSIHelper.red("Folder already exists!"));
                return;
            }
            folder.mkdirs();
            context.outStream().println(ANSIHelper.green("Folder created successfully: " + folderPath));
        } catch (Exception e) {
            context.outStream().println(ANSIHelper.red("Failed to create folder: " + e.getMessage()));
        }
    }

}
