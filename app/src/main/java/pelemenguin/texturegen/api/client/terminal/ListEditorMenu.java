package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ListEditorMenu<E> {

    private List<E> data;
    private BiConsumer<E, Consumer<E>> editor;
    private Supplier<E> defaultInstanceSupplier;
    private String description = null;
    private Consumer<TerminalMenu> extraKeys = t -> {};
    private Function<E, String> strigifier = String::valueOf;
    private int page = 0;
    private boolean immutable = false;

    public ListEditorMenu(List<E> data, BiConsumer<E, Consumer<E>> editor, Supplier<E> defaultInstanceSupplier) {
        this.data = data;
        this.editor = editor;
        this.defaultInstanceSupplier = defaultInstanceSupplier;
    }

    public ListEditorMenu<E> description(String description) {
        this.description = description;
        return this;
    }

    public ListEditorMenu<E> strigifier(Function<E, String> strigifier) {
        this.strigifier = strigifier;
        return this;
    }

    public ListEditorMenu<E> immutable() {
        this.immutable = true;
        return this;
    }

    public void loop(PrintStream out, Scanner scanner) {
        TerminalMenu menu = new TerminalMenu()
            .autoUppercase();
        while (true) {
            int totalPage = (this.data.size() - 1) / 10 + 1;
            menu.updateDescription((this.description == null ? "" : (this.description + "\n\n"))
                + "Page: " + ANSIHelper.green((page + 1) + "/" + totalPage));
            menu.clearKeys();
            menu.addKey('-', "Back", () -> {});
            // Add 0-9 entry
            int offset = page * 10;
            for (int i = 0; i < 10 && i < data.size() - offset; i++) {
                int index = i + offset;
                E data = this.data.get(index);
                menu.addKey((char) ('0' + (i + 1) % 10), ANSIHelper.cyan((index + 1) + ". ") + (data == null ? "[NULL]" : this.strigifier.apply(data)), () -> editor.accept(data, d -> this.data.set(index, d)));
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
                        .scan(out, scanner);
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
                        out.println(ANSIHelper.red("Invalid page number: " + newPage));
                    }
                });
            }

            if (!this.immutable) {
                menu.addKey('A', "Append new", () -> {
                    this.data.addLast(this.defaultInstanceSupplier.get());
                    this.page = totalPage - 1;
                });
                menu.addKey('D', "Delete", () -> {
                    String result = new StringInput("Deleting element's index? (1 ~ " + this.data.size() + ") (Leave empty to cancel)")
                        .allowEmpty()
                        .scan(out, scanner);
                    if (result.isEmpty()) {
                        return;
                    }
                    try {
                        int index = Integer.parseInt(result);
                        if (index < 1 || index > this.data.size()) {
                            throw new NumberFormatException();
                        }
                        this.data.remove(index - 1);
                        if (page >= (this.data.size() - 1) / 10 + 1) {
                            page = Math.max(0, page - 1);
                        }
                    } catch (NumberFormatException e) {
                        out.println(ANSIHelper.red("Invalid index: " + result));
                    }
                });
                menu.addKey('M', "Move", () -> {
                    String result = new StringInput("Moving element's index? (1 ~ " + this.data.size() + ") (Leave empty to cancel)")
                        .allowEmpty()
                        .scan(out, scanner);
                    if (result.isEmpty()) {
                        return;
                    }
                    try {
                        int index = Integer.parseInt(result);
                        if (index < 1 || index > this.data.size()) {
                            throw new NumberFormatException();
                        }
                        E element = this.data.remove(index - 1);
                        String newIndexResult = new StringInput("New index? (1 ~ " + this.data.size() + ") (Leave empty to cancel)")
                            .allowEmpty()
                            .scan(out, scanner);
                        if (newIndexResult.isEmpty()) {
                            // Put back the element
                            this.data.add(index - 1, element);
                            return;
                        }
                        int newIndex = Integer.parseInt(newIndexResult);
                        if (newIndex < 1 || newIndex > this.data.size() + 1) {
                            throw new NumberFormatException();
                        }
                        this.data.add(newIndex - 1, element);
                    } catch (NumberFormatException e) {
                        out.println(ANSIHelper.red("Invalid index: " + result));
                    }
                });
            }

            this.extraKeys.accept(menu);

            char result = menu.scan(out, scanner);
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
