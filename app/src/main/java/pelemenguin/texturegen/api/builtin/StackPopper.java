package pelemenguin.texturegen.api.builtin;

import java.util.ArrayList;
import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.JsonRegistry;

public class StackPopper implements Processor {

    private int popCount;

    public StackPopper(int popCount) {
        this.popCount = popCount;
    }

    public StackPopper() {
        this(1);
    }

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

}
