package pelemenguin.texturegen.api.generator;

import java.util.List;

public interface Processor {
    
    public void process(GenerationContext context, GenerationExecutor.Parameter parameters, GenerationExecutor.Result result);

    public List<Class<?>> getInputTypes();
    public List<Class<?>> getOutputTypes();

}
