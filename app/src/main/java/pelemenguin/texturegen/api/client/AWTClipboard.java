package pelemenguin.texturegen.api.client;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class AWTClipboard implements ClipboardWrapper {

    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    @Override
    public void copyText(String text) {
        clipboard.setContents(new StringSelection(text), null);
    }

    @Override
    public String pasteText() {
        try {
            return Objects.requireNonNullElse((String) clipboard.getData(DataFlavor.stringFlavor), "");
        } catch (Exception e) {
            return "";
        }
    }

}
