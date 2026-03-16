package pelemenguin.texturegen.api.client;

import java.awt.GraphicsEnvironment;

public interface ClipboardWrapper {
    
    public void copyText(String text);
    public String pasteText();

    public static ClipboardWrapper getClipboard() {
        // 1. Try AWT clipboard
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                return new AWTClipboard();
            } catch (Exception e) {
                // Failed to access system clipboard, fallback to memory clipboard
            }
        }

        // 2. Use memory clipboard
        return new MemoryClipboard();
    }

}
