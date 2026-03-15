package pelemenguin.texturegen.api.client.terminal;

import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.generator.Processor;

public interface TerminalProcessorEditorProvider {
    
    public void registerProcessorEditor(Registry registry);

    public static <P extends Processor> Editor<P> getEditorFor(Class<P> editorClass) {
        return (Editor<P>) Registry.INSTANCE.processorEditors.get(editorClass);
    }

    public static class Registry {

        public static final Registry INSTANCE = new Registry();

        private HashMap<Class<? extends Processor>, Editor<? extends Processor>> processorEditors = new HashMap<>();
        
        public <P extends Processor> void registerEditor(Class<P> processorClass, Editor<P> editorMenu) {
            this.processorEditors.put(processorClass, editorMenu);
        }

        public void refreshService() {
            ServiceLoader<TerminalProcessorEditorProvider> serviceLoader;
            try {
                serviceLoader = ServiceLoader.load(TerminalProcessorEditorProvider.class);
            } catch (Throwable t) {
                System.out.println("Failed to load TerminalProcessorEditorProvider from service loader. This may be caused by a missing dependency or an incompatible Java version.");
                t.printStackTrace();
                return;
            }
            for (TerminalProcessorEditorProvider provider : serviceLoader) {
                try {
                    provider.registerProcessorEditor(this);
                } catch (Throwable t) {
                    System.out.println("Failed to register TerminalProcessorEditorProvider: " + provider.getClass().getName());
                    t.printStackTrace();
                }
            }
        }

    }

    @FunctionalInterface
    public static interface Editor<P extends Processor> {
        public void processorEditorLoop(P processor, Consumer<P> setter, TerminalMenuContext context);
    }

}
