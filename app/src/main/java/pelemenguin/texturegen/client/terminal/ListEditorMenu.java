package pelemenguin.texturegen.client.terminal;

import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListEditorMenu<E> {

    private List<E> data;
    private Consumer<E> editor;
    private String description = null;
    private Consumer<TerminalMenu> extraKeys = t -> {};
    private Function<E, String> strigifier = E::toString;
    private int page = 0;

    public ListEditorMenu(List<E> data, Consumer<E> editor) {
        this.data = data;
        this.editor = editor;
    }

    public ListEditorMenu<E> description(String description) {
        this.description = description;
        return this;
    }

    public ListEditorMenu<E> strigifier(Function<E, String> strigifier) {
        this.strigifier = strigifier;
        return this;
    }

    public void loop(Scanner scanner) {
        int totalPage = (this.data.size() - 1) / 10 + 1;
        TerminalMenu menu = new TerminalMenu()
            .autoUppercase();
        while (true) {
            menu.updateDescription((this.description == null ? "" : (this.description + "\n\n"))
                + "Page: " + ANSIHelper.green((page + 1) + "/" + totalPage));
            menu.clearKeys();
            menu.addKey('-', "Back", () -> {});
            // Add 0-9 entry
            int offset = page * 10;
            for (int i = 0; i < 10 && i < data.size() - offset; i++) {
                int index = i + offset;
                E data = this.data.get(index);
                menu.addKey((char) ('0' + i), ANSIHelper.cyan((index + 1) + ". ") + this.strigifier.apply(data), () -> editor.accept(data));
            }

            if (this.page > 0) {
                menu.addKey('<', "Previous Page", () -> page--);
            }
            if (this.page < totalPage - 1) {
                menu.addKey('>', "Next Page", () -> page++);
            }
            if (totalPage > 3) {
                menu.addKey('J', "Jump to page", () -> {
                    String newPage = new StringInput("Jump to page (1 ~ " + totalPage + ") (Leave empty to cancel)")
                        .allowEmpty()
                        .scan(System.out, scanner);
                    if (newPage.isEmpty()) {
                        return;
                    }
                    try {
                        int page = Integer.parseInt(newPage);
                        if (page < 1 || page > totalPage) {
                            throw new NumberFormatException();
                        }
                        this.page = page - 1;
                    } catch (NumberFormatException e) {
                        System.out.println(ANSIHelper.red("Invalid page number: " + newPage));
                    }
                });
            }

            this.extraKeys.accept(menu);

            char result = menu.scan(System.out, scanner);
            if (result == '-') {
                break;
            }
        }
    }

    public ListEditorMenu<E> extraKeys(Consumer<TerminalMenu> addingKeys) {
        this.extraKeys = addingKeys;
        return this;
    }

}
