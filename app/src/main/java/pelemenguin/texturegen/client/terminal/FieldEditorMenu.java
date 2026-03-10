package pelemenguin.texturegen.client.terminal;

import java.io.PrintStream;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FieldEditorMenu<T> {

    private String description;
    private T obj;
    private HashMap<String, Character> keys = new HashMap<>();
    private HashMap<String, String> fieldNames = new HashMap<>();
    private HashMap<String, BiConsumer<Object, Consumer<Object>>> tasks = new HashMap<>();
    private TerminalMenu cachedTerminalMenu;

    public FieldEditorMenu(T obj) {
        this.obj = obj;
    }

    public T getObject() {
        return this.obj;
    }

    public FieldEditorMenu<T> description(String description) {
        this.description = description;
        return this;
    }

    public <F> FieldEditorMenu<T> specify(String fieldName, Character key, String displayName, BiConsumer<F, Consumer<F>> action) {
        this.specifyKey(fieldName, key);
        this.specifyFieldName(fieldName, displayName);
        this.specifyAction(fieldName, action);
        return this;
    }

    public FieldEditorMenu<T> specify(String fieldName, Character key, String displayName) {
        this.specifyKey(fieldName, key);
        this.specifyFieldName(fieldName, displayName);
        return this;
    }

    public FieldEditorMenu<T> specifyKey(String fieldName, Character key) {
        keys.put(fieldName, key);
        return this;
    }

    public FieldEditorMenu<T> specifyFieldName(String fieldName, String displayName) {
        fieldNames.put(fieldName, displayName);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <F> FieldEditorMenu<T> specifyAction(String fieldName, BiConsumer<F, Consumer<F>> actions) {
        tasks.put(fieldName, (v, s) -> {
            try {
                actions.accept((F) v, (o) -> s.accept(o));
            } catch (Throwable t) {
                System.out.println("Failed to execute action for field " + ANSIHelper.green(fieldName) + ": " + ANSIHelper.red(t.getMessage()));
            }
        });
        return this;
    }

    public FieldEditorMenu<T> loop(PrintStream out, Scanner scanner) {
        while (true) {
            char c = this.getTerminalMenu(out, scanner).scan(out, scanner);
            if (c == '-') {
                break;
            }
        }
        return this;
    }

    private Scanner currentScanner;
    private TerminalMenu getTerminalMenu(PrintStream out, Scanner scanner) {
        this.currentScanner = scanner;

        TerminalMenu menu = this.cachedTerminalMenu;
        if (menu == null) {
            menu = new TerminalMenu(description);
            this.cachedTerminalMenu = menu;
        }
        menu.autoUppercase()
            .clearKeys()
            .addKey('-', "Back", () -> {});
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & AccessFlag.STATIC.mask()) != 0) {
                continue;
            }
            try {
                field.setAccessible(true);
            } catch (Throwable t) {
                out.println(ANSIHelper.red("Cannot set accessible: " + t.getMessage()));
                continue;
            }
            String fieldName = field.getName();
            String displayName = this.fieldNames.getOrDefault(fieldName, fieldName);
            Character key = keys.get(fieldName);
            if (key == null) {
                key = displayName.charAt(0);
            }
            key = Character.toUpperCase(key);
            final BiConsumer<Object, Consumer<Object>> action;
            Object originalValue;
            try {
                originalValue = field.get(obj);
            } catch (Throwable t) {
                continue;
            }
            var actionFromMap = tasks.get(fieldName);
            if (actionFromMap == null) {
                action = getDefaultEditorForType(field, displayName, out, scanner);
            } else {
                action = actionFromMap;
            }
            menu.addKey(key, displayName + ": " + (originalValue == null ? ANSIHelper.red("NULL!") : ANSIHelper.blue(String.valueOf(originalValue))), () -> {
                action.accept(originalValue, (f) -> {
                    try {
                        field.set(obj, f);
                    } catch (Throwable t) {
                        out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                    }
                });
            });
        }
        this.cachedTerminalMenu = menu;
        return menu;
    }

    public <F> BiConsumer<F, Consumer<F>> getDefaultEditorForType(Field field, String displayName, PrintStream out, Scanner scanner) {
        Object originalValue;
        try {
            originalValue = field.get(obj);
        } catch (Throwable t) {
            return (f, c) -> {};
        }
        if (field.getType() == String.class) {
            return (value, setter) -> {
                try {
                    String result = new StringInput("Enter new value for " + ANSIHelper.green(displayName) + " (current: " + ANSIHelper.blue(String.valueOf(originalValue)) + ")")
                        .scan(out, this.currentScanner);
                    if (!result.isBlank()) {
                        field.set(obj, result);
                    }
                } catch (Throwable t) {
                    out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                }
            };
        // Or Numbers
        } else if (Number.class.isAssignableFrom(field.getType())) {
            return (value, setter) -> {
                try {
                    String result = new StringInput("Enter new value for " + ANSIHelper.green(displayName) + " (current: " + ANSIHelper.blue(String.valueOf(originalValue)) + ")")
                        .scan(out, this.currentScanner);
                    if (!result.isBlank()) {
                        Number numberValue = null;
                        if (field.getType() == Integer.class || field.getType() == int.class) {
                            numberValue = Integer.parseInt(result);
                        } else if (field.getType() == Long.class || field.getType() == long.class) {
                            numberValue = Long.parseLong(result);
                        } else if (field.getType() == Float.class || field.getType() == float.class) {
                            numberValue = Float.parseFloat(result);
                        } else if (field.getType() == Double.class || field.getType() == double.class) {
                            numberValue = Double.parseDouble(result);
                        }
                        if (numberValue != null) {
                            field.set(obj, numberValue);
                        }
                    }
                } catch (Throwable t) {
                    out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                }
            };
        } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
            return (value, setter) -> {
                try {
                    boolean newValue = !(((Boolean) originalValue).booleanValue());
                    field.set(obj, newValue);
                } catch (Throwable t) {
                    out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                }
            };
        } else {
            return (value, setter) -> {};
        }
    }
}
