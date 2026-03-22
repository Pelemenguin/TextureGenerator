package pelemenguin.texturegen.api.generator;

import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;
import pelemenguin.texturegen.api.util.JsonRegistry.DeserializationFailedReason;

public interface Processor extends JsonRegistry.Registrable<Processor> {

    public static final JsonRegistry<Processor> REGISTRY = new JsonRegistry<>(Processor.class, (registry, processor) -> {
        PrivateObjectHolder.PROCESSOR_NAMES.put(registry.getIdOf(processor.getClass()), processor.getProcessorName());
    })
        .setFallbackId("texturegen.error");
    public static final Gson GSON = REGISTRY.createGsonBuilder()
        .registerTypeAdapterFactory(PointFilter.TYPE_ADAPTER)
        .setPrettyPrinting()
        .create();
        
    public static class PrivateObjectHolder {
        private static final HashMap<String, String> PROCESSOR_NAMES = new HashMap<>();
    }

    public static String getNameOf(String processorId) {
        return PrivateObjectHolder.PROCESSOR_NAMES.getOrDefault(processorId, processorId);
    }

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

    public static class ErrorProcessor implements Processor, JsonRegistry.ErrorFallback<Processor> {

        private Throwable cause;
        private JsonElement raw;
        private JsonRegistry.DeserializationFailedReason reason;

        @Override
        public JsonElement getRawJson() {
            return raw;
        }

        @Override
        public Throwable getCause() {
            return cause;
        }

        @Override
        public void setRawJson(JsonElement json) {
            this.raw = json;
        }

        @Override
        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public void setReason(DeserializationFailedReason reason) {
            this.reason = reason;
        }

        @Override
        public void process(GenerationContext context, Parameter parameters, Result result) {
            throw new IllegalStateException("Cannot process with error processor", cause);
        }

        @Override
        public void register(JsonRegistry<Processor> registry) {
            registry.register("texturegen.error", ErrorProcessor.class);
        }

        @Override
        public List<Class<?>> getInputTypes() {
            throw new IllegalStateException("Wrong Processor format", cause);
        }

        @Override
        public List<Class<?>> getOutputTypes() {
            throw new IllegalStateException("Wrong Processor format", cause);
        }

        @Override
        public String getProcessorName() {
            return "[ERROR]";
        }

        @Override
        public String getProcessorTitle() {
            return switch (this.reason) {
                case MISSING_TYPE -> "[UNKNOWN]";
                case TYPE_NOT_FOUND -> this.raw.getAsJsonObject().get("type") + " [UNRECOGNIZED]";
                case ADAPTER_THREW_EXCEPTION -> this.raw.getAsJsonObject().get("type") + " [BROKEN]";
            };
        }

    }

}
