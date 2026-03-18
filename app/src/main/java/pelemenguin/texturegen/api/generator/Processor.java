package pelemenguin.texturegen.api.generator;

import java.util.List;

import com.google.gson.Gson;

import pelemenguin.texturegen.api.util.JsonRegistry;

public interface Processor extends JsonRegistry.Registrable<Processor> {

    public static final JsonRegistry<Processor> REGISTRY = new JsonRegistry<>(Processor.class);
    public static final Gson GSON = REGISTRY.createGson();

    public void process(GenerationContext context, GenerationExecutor.Parameter parameters, GenerationExecutor.Result result);

    public List<Class<?>> getInputTypes();
    public List<Class<?>> getOutputTypes();

    public default String getProcessorName() {
        return this.getClass().getName();
    }

    public default String getProcessorTitle() {
        return this.toString();
    }

}
