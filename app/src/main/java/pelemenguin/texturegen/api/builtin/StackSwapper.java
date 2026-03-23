package pelemenguin.texturegen.api.builtin;

import java.util.ArrayList;
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

public class StackSwapper implements Processor {

    public int insertThourgh = 1;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        Object target = parameters.load(0, Object.class);
        for (int i = 1; i <= insertThourgh; i++) {
            Object o = parameters.load(i, Object.class);
            result.push(i - 1, o);
        }
        result.push(insertThourgh, target);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.stack.swap", StackSwapper.class);
    }

    @Override
    public List<Class<?>> getInputTypes(List<Class<?>> currentStack) {
        return currentStack.subList(0, insertThourgh + 1);
    }

    @Override
    public List<Class<?>> getOutputTypes(List<Class<?>> currentStack) {
        Class<?> movingType = currentStack.get(0);
        List<Class<?>> through = currentStack.subList(1, insertThourgh + 1);
        List<Class<?>> output = new ArrayList<>();
        output.addAll(through);
        output.add(movingType);
        return output;
    }

    @Override
    public String getProcessorName() {
        return "Swap Stack Element Down";
    }

    @Override
    public String getProcessorTitle() {
        return "Swap Stack Element Down by " + insertThourgh;
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<StackSwapper> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.stack.swap", this);
        }

        @Override
        public void editorLoop(StackSwapper processor, Consumer<StackSwapper> setter, TerminalMenuContext context) {
            StringInput input = new StringInput("Enter new swap count (leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.insertThourgh)) + ")")
                .allowEmpty();
            while (true) {
                String result = input.scan(context);
                if (result.isBlank()) {
                    return;
                }
                try {
                    int newSwapCount = Integer.parseInt(result);
                    if (newSwapCount <= 0) {
                        context.outStream().println(ANSIHelper.red("Invalid input. Swap count must be positive"));
                        throw new NumberFormatException();
                    }
                    processor.insertThourgh = newSwapCount;
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
