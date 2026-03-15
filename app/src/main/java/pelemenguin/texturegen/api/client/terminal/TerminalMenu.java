package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class TerminalMenu {

    private String description;
    private LinkedHashMap<String, String> descriptions = new LinkedHashMap<>();
    private LinkedHashMap<Character, Runnable> actions = new LinkedHashMap<>();
    private boolean autoUppercase = false;

    public TerminalMenu(String description) {
        this.description = description;
    }

    public TerminalMenu() {
    }

    public TerminalMenu updateDescription(String description) {
        this.description = description;
        return this;
    }

    public TerminalMenu updateKeyDescription(Character key, String description) {
        String keyRepr = key.toString();
        if (autoUppercase) {
            keyRepr = keyRepr.toUpperCase();
        }
        this.descriptions.put(keyRepr, description);
        return this;
    }

    public TerminalMenu addKey(Character key, String keyRepr, String description, Runnable action) {
        this.descriptions.put(keyRepr, description);
        this.actions.put(key, action);
        return this;
    }

    private static final Runnable EMPTY_RUNNABLE = () -> {};
    public TerminalMenu addKey(Character key, String description) {
        return addKey(key, description, EMPTY_RUNNABLE);
    }

    public TerminalMenu addKey(Character key, String description, Runnable action) {
        return addKey(key, this.autoUppercase ? String.valueOf(Character.toUpperCase(key)) : key.toString(), description, action);
    }

    public char scan(TerminalMenuContext context) {
        return this.scan(context.outStream(), context.scanner());
    }

    public String getDisplayContent() {
        StringBuilder sb = new StringBuilder();
        if (this.description != null) {
            sb.append(description).append('\n');
        }

        for (String keyRepr : descriptions.keySet()) {
            sb.append(ANSIHelper.blue("[" + keyRepr + "] ") + descriptions.get(keyRepr)).append('\n');
        }
        if (sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public char scan(PrintStream out, Scanner scanner) {
        out.println("==========\n");
        out.println(this.getDisplayContent());
        out.print(ANSIHelper.magenta("\n> "));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                continue;
            }
            char firstChar = line.charAt(0);
            if (autoUppercase) {
                firstChar = Character.toUpperCase(firstChar);
            }
            Runnable action = actions.get(firstChar);
            ANSIHelper.clear(out);
            if (action != null) {
                action.run();
                return firstChar;
            } else {
                out.println(ANSIHelper.red("Invalid input: " + firstChar));
                return scan(out, scanner);
            }
        }

        return '\0';
    }

    public TerminalMenu autoUppercase() {
        this.autoUppercase = true;
        return this;
    }

    public TerminalMenu clearKeys() {
        this.descriptions.clear();
        this.actions.clear();
        return this;
    }

}
