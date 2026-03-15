package pelemenguin.texturegen.api.client.terminal;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
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
    private BiConsumer<E, Clipboard> clipboardCopier;
    private Function<Clipboard, E> clipboardPaster;
    private int page = 0;
    private boolean immutable = false;
    private boolean selectMode = false;

    public ListEditorMenu(List<E> data) {
        this(data, null, null);
        this.immutable()
            .selectMode();
    }

    @SuppressWarnings("unchecked")
    public ListEditorMenu(List<E> data, BiConsumer<E, Consumer<E>> editor) {
        this(data, editor, null);
    }

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

    public ListEditorMenu<E> selectMode() {
        this.selectMode = true;
        return this;
    }

    public ListEditorMenu<E> copyToClipboard(BiConsumer<E, Clipboard> copier) {
        this.clipboardCopier = copier;
        return this;
    }

    public ListEditorMenu<E> copyToClipboardAsString(Function<E, String> strigifier) {
        return this.copyToClipboard((e, clipboard) -> {
            StringSelection selection = new StringSelection(strigifier.apply(e));
            clipboard.setContents(selection, null);
        });
    }

    public ListEditorMenu<E> pasteFromClipboard(Function<Clipboard, E> paster) {
        this.clipboardPaster = paster;
        return this;
    }

    public ListEditorMenu<E> pasteFromClipboardFromString(Function<String, E> parser) {
        return this.pasteFromClipboard(clipboard -> {
            try {
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                return parser.apply(data);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public E loop(TerminalMenuContext context) {
        return this.loop(context.outStream(), context.scanner(), context.clipboard());
    }

    private boolean deleteMode = false;
    private int movingModeSelected = -1; // -1 for not selecting, -2 for waiting for the element to move
    private boolean copyMode = false;
    private boolean pasteMode = false;

    public E loop(PrintStream out, Scanner scanner, Clipboard clipboard) {
        TerminalMenu menu = new TerminalMenu()
            .autoUppercase();
        E[] resultHolder = (E[]) new Object[1];
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
                String strigified = (data == null ? "[NULL]" : this.strigifier.apply(data));
                String repr = ANSIHelper.cyan((index + 1) + ". ") + (this.deleteMode
                        ? ANSIHelper.red(strigified)
                        : this.movingModeSelected != -1
                        ? ANSIHelper.yellow(strigified)
                        : this.copyMode
                        ? ANSIHelper.magenta(strigified)
                        : this.pasteMode
                        ? ANSIHelper.green(strigified)
                        : strigified
                    );
                menu.addKey((char) ('0' + (i + 1) % 10), repr, () -> {
                    if (this.selectMode) {
                        resultHolder[0] = data;
                    } else {
                        if (this.deleteMode) {
                            new TerminalMenu("Are you sure you want to delete " + repr + "?\n" + ANSIHelper.red("This action cannot be undone!"))
                                .autoUppercase()
                                .addKey('Y', "Yes", () -> {
                                    this.data.remove(index);
                                    this.deleteMode = false;
                                })
                                .addKey('N', "No")
                                .scan(out, scanner);
                        } else if (this.movingModeSelected == -2) {
                            this.movingModeSelected = index;
                        } else if (this.movingModeSelected >= 0) {
                            if (this.movingModeSelected != index) {
                                E temp = this.data.get(this.movingModeSelected);
                                this.data.remove(this.movingModeSelected);
                                this.data.add(index, temp);
                            }
                            this.movingModeSelected = -1;
                        } else if (this.copyMode) {
                            try {
                                this.clipboardCopier.accept(data, clipboard);
                            } catch (Throwable t) {
                                out.println(ANSIHelper.red("Failed to copy to clipboard: " + t.getMessage()));
                            }
                            this.copyMode = false;
                        } else if (this.pasteMode) {
                            try {
                                E pasted = this.clipboardPaster.apply(clipboard);
                                if (pasted == null) {
                                    out.println(ANSIHelper.red("Failed to paste from clipboard!"));
                                    return;
                                }
                                this.data.add(index, pasted);
                            } catch (Throwable t) {
                                out.println(ANSIHelper.red("Failed to paste from clipboard: " + t.getMessage()));
                            }
                            this.pasteMode = false;
                        } else {
                            editor.accept(data, d -> this.data.set(index, d));
                        }
                    }
                });
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

            if (!this.selectMode && !this.immutable) {
                menu.addKey('A', "Append new", () -> {
                    if (this.defaultInstanceSupplier == null) {
                        E[] holder = (E[]) new Object[1];
                        editor.accept(null, (e) -> {
                            holder[0] = e;
                        });
                        if (holder[0] == null) {
                            out.println(ANSIHelper.red("Creation cancelled!"));
                            return;
                        }
                        this.data.addLast(holder[0]);
                    } else {
                        this.data.addLast(this.defaultInstanceSupplier.get());
                    }
                    this.page = totalPage - 1;
                });
                menu.addKey('D', this.deleteMode ? "Cancel deletion" : "Delete", () -> {
                    this.copyMode = false;
                    this.pasteMode = false;
                    if (this.movingModeSelected != -1) {
                        this.movingModeSelected = -1;
                    }
                    this.deleteMode = !this.deleteMode;
                });
                menu.addKey('M', switch (this.movingModeSelected) {
                    case -1 -> "Move";
                    case -2 -> "Cancel move (Selecting target)";
                    default -> {
                        String repr = ANSIHelper.cyan((this.movingModeSelected + 1) + ". ");
                        E cur = this.data.get(this.movingModeSelected);
                        repr += ANSIHelper.yellow(cur == null ? "[NULL]" : (this.strigifier.apply(cur)));
                        yield "Cancel move (Inserting before)" + (this.movingModeSelected < 0 ? ""
                            : (" (Selected: " + repr + ")"));
                    }
                }, () -> {
                    this.copyMode = false;
                    this.deleteMode = false;
                    this.pasteMode = false;
                    if (this.movingModeSelected == -1) {
                        this.movingModeSelected = -2;
                    } else {
                        this.movingModeSelected = -1;
                    }
                });
            }

            if (this.clipboardCopier != null) {
                menu.addKey('C', this.copyMode ? "Cancel copy" : "Copy element", () -> {
                    this.deleteMode = false;
                    this.movingModeSelected = -1;
                    this.pasteMode = false;
                    this.copyMode = !this.copyMode;
                });
            }
            if (this.clipboardPaster != null) {
                menu.addKey('P', this.pasteMode ? "Cancel paste (Insert element)" : "Paste element", () -> {
                    this.deleteMode = false;
                    this.movingModeSelected = -1;
                    this.copyMode = false;
                    this.pasteMode = !this.pasteMode;
                });
            }

            this.extraKeys.accept(menu);

            char result = menu.scan(out, scanner);
            if (result == '-') {
                return null;
            } else if (this.selectMode && resultHolder[0] != null) {
                return resultHolder[0];
            }
        }
    }

    public ListEditorMenu<E> extraKeys(Consumer<TerminalMenu> addingKeys) {
        this.extraKeys = addingKeys;
        return this;
    }

}
