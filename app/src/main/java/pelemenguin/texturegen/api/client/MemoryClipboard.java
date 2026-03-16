package pelemenguin.texturegen.api.client;

public class MemoryClipboard implements ClipboardWrapper {

    String text = "";

    @Override
    public synchronized void copyText(String text) {
        this.text = text;
    }

    @Override
    public synchronized String pasteText() {
        return text;
    }

}
