package pelemenguin.texturegen.api.builtin;

import java.util.List;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;

public class StackDuplicator implements Processor {

    public int duplicateCount = 1;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        for (int i = 0; i < duplicateCount; i++) {
            result.push(i, parameters.load(i, Object.class));
            result.push(i + duplicateCount, parameters.load(i, Object.class));
        }
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.stack.duplicate", StackDuplicator.class);
    }

    private List<Class<?>> getTypesToDuplicate(List<Class<?>> currentStack) {
        if (currentStack.size() < duplicateCount) {
            throw new IllegalStateException("Not enough elements on the stack to duplicate. Required: " + duplicateCount + ", but only " + currentStack.size() + " available.");
        }
        Class<?>[] classes = new Class[duplicateCount];
        for (int i = 0; i < duplicateCount; i++) {
            classes[i] = currentStack.get(i);
        }
        return List.of(classes);
    }

    @Override
    public List<Class<?>> getInputTypes(List<Class<?>> currentStack) {
        return getTypesToDuplicate(currentStack);
    }

    @Override
    public List<Class<?>> getOutputTypes(List<Class<?>> currentStack) {
        // Return 2x getTypesToDuplicate
        List<Class<?>> typesToDuplicate = getTypesToDuplicate(currentStack);
        Class<?>[] classes = new Class<?>[typesToDuplicate.size() * 2];
        typesToDuplicate.toArray(classes);
        System.arraycopy(classes, 0, classes, typesToDuplicate.size(), typesToDuplicate.size());
        return List.of(classes);
    }

    @Override
    public String getProcessorName() {
        return "Duplicate Stack Element(s)";
    }

    @Override
    public String getProcessorTitle() {
        return "Duplicate " + this.duplicateCount + " Stack Element(s)";
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<StackDuplicator> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.stack.duplicate", this);
        }

        @Override
        public void editorLoop(StackDuplicator processor, Consumer<StackDuplicator> setter, TerminalMenuContext context) {
            StringInput input = new StringInput("Enter new duplicate count (leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.duplicateCount)) + ")")
                .allowEmpty();
            while (true) {
                String result = input.scan(context);
                if (result.isBlank()) {
                    return;
                }
                try {
                    int newDuplicateCount = Integer.parseInt(result);
                    if (newDuplicateCount <= 0) {
                        context.outStream().println(ANSIHelper.red("Invalid input. Duplicate count must be positive"));
                        throw new NumberFormatException();
                    }
                    processor.duplicateCount = newDuplicateCount;
                    return;
                } catch (NumberFormatException e) {
                    context.outStream().println(ANSIHelper.red("Invalid input. Not a number"));
                }
            }
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

    }

}
