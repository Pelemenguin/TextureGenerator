package pelemenguin.texturegen.api.client;

public class TermuxClipboard implements ClipboardWrapper {

    @Override
    public void copyText(String text) {
        // Try to save to system clipboard using Termux API
        try {
            Runtime.getRuntime().exec(new String[]{"termux-clipboard-set", text});
        } catch (Exception e) {}
    }

    @Override
    public String pasteText() {
        // Try to get text from system clipboard using Termux API
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"termux-clipboard-get"});
            process.waitFor();
            byte[] output = process.getInputStream().readAllBytes();
            return new String(output);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean available() {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"which", "termux-clipboard-get"});
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

}
