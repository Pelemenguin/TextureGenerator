package pelemenguin.texturegen.client.terminal;

import java.io.PrintStream;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;

public class FieldEditorMenu<T> {

    private String description;
    private T obj;
    private HashMap<String, Character> keys = new HashMap<>();
    private HashMap<String, String> fieldNames = new HashMap<>();
    private HashMap<String, Runnable> tasks = new HashMap<>();
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

    public FieldEditorMenu<T> specifyKey(String fieldName, Character key) {
        keys.put(fieldName, key);
        return this;
    }

    public FieldEditorMenu<T> specifyFieldName(String fieldName, String displayName) {
        fieldNames.put(fieldName, displayName);
        return this;
    }

    public FieldEditorMenu<T> specifyAction(String fieldName, Runnable actions) {
        tasks.put(fieldName, actions);
        return this;
    }

    public FieldEditorMenu<T> loop(PrintStream out, Scanner scanner) {
        while (true) {
            char c = this.getTerminalMenu(scanner).scan(out, scanner);
            if (c == '-') {
                break;
            }
        }
        return this;
    }

    private Scanner currentScanner;
    private TerminalMenu getTerminalMenu(Scanner scanner) {
        this.currentScanner = scanner;

        TerminalMenu menu = this.cachedTerminalMenu;
        if (menu == null) {
            menu = new TerminalMenu(description);
            this.cachedTerminalMenu = menu;
        }
        menu.autoUppercase()
            .clearKeys()
            .addKey('-', "Back", () -> {});
        Field[] fields = obj.getClass().getFields();
        for (Field field : fields) {
            // Skip non-public and static fields
            if ((field.getModifiers() & AccessFlag.PUBLIC.mask()) == 0 || (field.getModifiers() & AccessFlag.STATIC.mask()) != 0) {
                continue;
            }
            String fieldName = field.getName();
            String displayName = this.fieldNames.getOrDefault(fieldName, fieldName);
            Character key = keys.get(fieldName);
            if (key == null) {
                key = displayName.charAt(0);
            }
            key = Character.toUpperCase(key);
            Runnable action = tasks.get(fieldName);
            Object originalValue;
            try {
                originalValue = field.get(obj);
            } catch (Throwable t) {
                continue;
            }
            if (action == null) {
                if (field.getType() == String.class) {
                    action = () -> {
                        try {
                            String result = new StringInput("Enter new value for " + ANSIHelper.green(displayName) + " (current: " + ANSIHelper.blue(String.valueOf(originalValue)) + ")")
                                .scan(System.out, this.currentScanner);
                            if (!result.isBlank()) {
                                field.set(obj, result);
                            }
                        } catch (Throwable t) {
                            System.out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                        }
                    };
                // Or Numbers
                } else if (Number.class.isAssignableFrom(field.getType())) {
                    action = () -> {
                        try {
                            String result = new StringInput("Enter new value for " + ANSIHelper.green(displayName) + " (current: " + ANSIHelper.blue(String.valueOf(originalValue)) + ")")
                                .scan(System.out, this.currentScanner);
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
                            System.out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                        }
                    };
                } else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
                    action = () -> {
                        try {
                            boolean newValue = !(((Boolean) originalValue).booleanValue());
                            field.set(obj, newValue);
                        } catch (Throwable t) {
                            System.out.println("Failed to set field: " + ANSIHelper.red(t.getMessage()));
                        }
                    };
                } else if (field.getClass().isArray()) {
                    action = () -> {
                        // TODO: Implement this
                    };
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    action = () -> {
                        // TODO: Implement this
                    };
                } else {
                    continue; // Skip current field
                }
            }
            menu.addKey(key, displayName + ": " + (originalValue == null ? ANSIHelper.red("NULL!") : ANSIHelper.blue(String.valueOf(originalValue))), action);
        }
        this.cachedTerminalMenu = menu;
        return menu;
    }

}
