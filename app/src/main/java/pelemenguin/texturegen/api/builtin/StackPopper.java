package pelemenguin.texturegen.api.builtin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

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

public class StackPopper implements Processor {

    @SerializedName("pop_count")
    public int popCount = 1;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.stack.pop", StackPopper.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        ArrayList<Class<?>> result = new ArrayList<>(popCount);
        for (int i = 0; i < popCount; i++) {
            result.add(Object.class);
        }
        return result;
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of();
    }

    @Override
    public String getProcessorName() {
        return "Pop Stack Elements";
    }

    @Override
    public String getProcessorTitle() {
        return "Pop " + this.popCount + " Stack Element(s)";
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<StackPopper> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.stack.pop", this);
        }

        @Override
        public void editorLoop(StackPopper processor, Consumer<StackPopper> setter, TerminalMenuContext context) {
            StringInput input = new StringInput("Enter new pop count (leave empty to cancel. Original: " + ANSIHelper.blue(String.valueOf(processor.popCount)) + ")")
                .allowEmpty();
            while (true) {
                String result = input.scan(context);
                if (result.isBlank()) {
                    break;
                }
                try {
                    int newPopCount = Integer.parseInt(result);
                    if (newPopCount <= 0) {
                        context.outStream().println(ANSIHelper.red("Invalid input. Pop count must be positive"));
                        throw new NumberFormatException();
                    }
                    processor.popCount = newPopCount;
                    break;
                } catch (NumberFormatException e) {
                    context.outStream().println(ANSIHelper.red("Invalid input. Not a number"));
                }
            }
            setter.accept(processor);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

    }

}
