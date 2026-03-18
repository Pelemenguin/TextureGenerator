package pelemenguin.texturegen.api.client.terminal;

import java.util.function.Consumer;

import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;

public interface TerminalProcessorEditorProvider extends CommonRegistry.Registrable<TerminalProcessorEditorProvider> {

    public static final CommonRegistry<TerminalProcessorEditorProvider> REGISTRY = new CommonRegistry<>(TerminalProcessorEditorProvider.class);

    public Editor<? extends Processor> getEditor();

    public static interface Editor<P extends Processor> {
        public void editorLoop(P processor, Consumer<P> setter, TerminalMenuContext context);
    }

}
