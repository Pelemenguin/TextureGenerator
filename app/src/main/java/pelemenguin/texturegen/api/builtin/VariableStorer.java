package pelemenguin.texturegen.api.builtin;

import java.util.List;
import java.util.Objects;
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

public class VariableStorer implements Processor {

    @SerializedName("name")
    String variableName;

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        context.variables().store(variableName, parameters.load(0, Object.class));
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.variable.store", VariableStorer.class);
    }

    @Override
    public void processorInit() {
        if (variableName == null) {
            throw new IllegalStateException("Variable name is not set!");
        }
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(Object.class);
    }

    @Override
    public String getProcessorName() {
        return "Store Variable";
    }

    @Override
    public String getProcessorTitle() {
        return "Store Variable to " + Objects.requireNonNullElse(this.variableName, "[Variable Unset]");
    }

    public static class TerminalEditor implements TerminalProcessorEditorProvider.Editor<VariableStorer>, TerminalProcessorEditorProvider {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.variable.store", this);
        }

        @Override
        public Editor<? extends Processor> getEditor() {
            return this;
        }

        @Override
        public void editorLoop(VariableStorer processor, Consumer<VariableStorer> setter, TerminalMenuContext context) {
            new StringInput("Enter new variable name: (Leave empty to cancel. Original: " + ANSIHelper.blue(processor.variableName) + ")")
                .allowEmpty()
                .scanAndRun(context, (resString, error) -> {
                    if (resString.isBlank()) return;
                    processor.variableName = resString;
                });
        }

    }

}
