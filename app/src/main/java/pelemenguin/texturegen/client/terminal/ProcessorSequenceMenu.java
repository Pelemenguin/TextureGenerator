package pelemenguin.texturegen.client.terminal;

import java.util.List;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ListEditorMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.generator.GeneratorInfo;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.spi.TerminalProcessorEditorProvider;

public class ProcessorSequenceMenu {

    public static void loop(TerminalMenuContext context, GeneratorInfoMenu parent) {
        ListEditorMenu<Processor> menu = new ListEditorMenu<Processor>(parent.info.processors, (original, setter) -> {
            if (original == null) {
                create(setter, context);
            } else {
                edit(original, setter, context);
            }
        }).strigifier(Processor::getProcessorTitle)
            .copyToClipboardAsString((p) -> GeneratorInfo.GSON.toJson(p))
            .pasteFromClipboardFromString((p) -> GeneratorInfo.GSON.fromJson(p, Processor.class));
        menu.loop(context);
    }

    private static void create(Consumer<Processor> setter, TerminalMenuContext context) {
        List<String> processors = List.copyOf(Processor.REGISTRY.getRegisteredIds());
        ListEditorMenu<String> processorSelector = new ListEditorMenu<>(processors)
            .description("Select a processor to add:");
        String selected = processorSelector.loop(context);
        if (selected != null) {
            Class<? extends Processor> processorClass = Processor.REGISTRY.getClassOf(selected);
            try {
                Processor processor = processorClass.getDeclaredConstructor().newInstance();
                setter.accept(processor); // In case editor not found, for example a simple processor with no editable properties,
                                          // this will still add the processor to the sequence.
                edit(processor, setter, context);
            } catch (Throwable t) {
                context.outStream().println(ANSIHelper.red("Failed to create processor: " + t.getMessage()));
                t.printStackTrace();
            }
        }
    }

    public static void edit(Processor original, Consumer<Processor> setter, TerminalMenuContext context) {
        String id = Processor.REGISTRY.getIdOf(original.getClass());
        TerminalProcessorEditorProvider.Editor<Processor> editor =
            (TerminalProcessorEditorProvider.Editor<Processor>) TerminalProcessorEditorProvider.REGISTRY.get(id);
        if (editor == null) {
            context.outStream().println(ANSIHelper.red("No editor found for processor of processor " + id));
            return;
        }
        editor.editorLoop(original, setter, context);
    }

}
