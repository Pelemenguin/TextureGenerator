package pelemenguin.texturegen.api.generator;

import java.util.List;

import com.google.gson.Gson;

import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public interface Processor extends JsonRegistry.Registrable<Processor> {

    public static final JsonRegistry<Processor> REGISTRY = new JsonRegistry<>(Processor.class);
    public static final Gson GSON = REGISTRY.createGsonBuilder()
        .registerTypeAdapterFactory(PointFilter.TYPE_ADAPTER)
        .setPrettyPrinting()
        .create();

    public void process(GenerationContext context, GenerationExecutor.Parameter parameters, GenerationExecutor.Result result);

    public void register(JsonRegistry<Processor> registry);

    public List<Class<?>> getInputTypes();
    public List<Class<?>> getOutputTypes();

    public default String getProcessorName() {
        return REGISTRY.getIdOf(this.getClass());
    }

    public default String getProcessorTitle() {
        return this.toString();
    }

}
