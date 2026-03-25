package pelemenguin.texturegen.api.generator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;

import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.NoiseProvider;
import pelemenguin.texturegen.api.util.PointFilter;
import pelemenguin.texturegen.api.util.JsonRegistry.DeserializationFailedReason;

/**
 * {@code Processor}s are used in texture generation.
 * 
 * <p>
 * A {@code Processor} is similar to a function (but can have multiple return values).
 * They accept 0, 1, or more values as parameters, and produce 0, 1 or more values as return values.
 * 
 * <p>
 * Values are stored on a {@link Deque} (or an {@link ArrayDeque}, more specificly).
 * When running a {@code Processor}, required values are polled out from the {@link Deque} using {@link Deque#pollFirst()}.
 * After processing, produced values are pushed into the {@link Deque} using {@link Deque#addFirst(Object)}.
 */
public interface Processor extends JsonRegistry.Registrable<Processor> {

    public static final JsonRegistry<Processor> REGISTRY = new JsonRegistry<>(Processor.class, (registry, processor) -> {
        PrivateObjectHolder.PROCESSOR_NAMES.put(registry.getIdOf(processor.getClass()), processor.getProcessorName());
    })
        .setFallbackId("texturegen.error");
    public static final Gson GSON = REGISTRY.createGsonBuilder()
        .registerTypeAdapterFactory(PointFilter.TYPE_ADAPTER)
        .registerTypeAdapterFactory(NoiseProvider.TYPE_ADAPTER)
        .setPrettyPrinting()
        .create();

    public static class PrivateObjectHolder {
        private static final HashMap<String, String> PROCESSOR_NAMES = new HashMap<>();
    }

    /**
     * Gets the cached display name of a processor by its ID.
     * If no display name is registered, returns the ID itself.
     * 
     * <p>
     * The display name is specified by {@link #getProcessorName()} when registering.
     * 
     * @param processorId The ID of the {@link Processor}
     * @return            The display name of {@link Processor}
     */
    public static String getNameOf(String processorId) {
        return PrivateObjectHolder.PROCESSOR_NAMES.getOrDefault(processorId, processorId);
    }

    /**
     * Processes the {@link Processor}.
     * 
     * @param context    The {@link GenerationContext} of the processing image
     * @param parameters A helper object ({@link GenerationExecutor.Parameter}) to access parameters passed in
     * @param result     A helper object ({@link GenerationExecutor.Result}) to store results produced by the processor
     */
    public void process(GenerationContext context, GenerationExecutor.Parameter parameters, GenerationExecutor.Result result);

    /**
     * Registers the {@link Processor} to the {@link #REGISTRY}.
     * 
     * @param registry The {@link #REGISTRY}
     * 
     * @see JsonRegistry#register(String, Class)
     * @see JsonRegistry#register(String, Class, TypeAdapter)
     */
    public void register(JsonRegistry<Processor> registry);

    /**
     * Gets the parameter types of the {@link Processor}.
     * 
     * @return A list of {@link Class} representing the parameter types of the {@link Processor}.
     */
    public default List<Class<?>> getInputTypes() {
        return List.of();
    }

    /**
     * Gets the parameter types of the {@link Processor} depending on current stack.
     * 
     * @param currentStack The current operation stack, with the top being the first element of the {@link List}.
     * @return             A list of {@link Class} representing the parameter types of the {@link Processor}.
     */
    public default List<Class<?>> getInputTypes(List<Class<?>> currentStack) {
        return this.getInputTypes();
    }

    /**
     * Gets the returned types of the {@link Processor}.
     * 
     * @return A list of {@link Class} representing the returned types of the {@link Processor}.
     */
    public default List<Class<?>> getOutputTypes() {
        return List.of();
    }

    /**
     * Gets the returned types of the {@link Processor} depending on current stack.
     * 
     * @param currentStack The current operation stack, with the top being the first element of the {@link List}.
     * @return             A list of {@link Class} representing the returned types of the {@link Processor}.
     */
    public default List<Class<?>> getOutputTypes(List<Class<?>> currentStack) {
        return this.getOutputTypes();
    }

    /**
     * Specifies the name to be displayed of the {@link Processor}.
     * 
     * <p>
     * This name represents the a entire subclass of {@link Processor}.
     * Therefore, it should be a <b>constant</b> {@link String} and independent of the instance.
     * 
     * @return The name of the {@link Processor}.
     */
    public default String getProcessorName() {
        return REGISTRY.getIdOf(this.getClass());
    }

    /**
     * Specifies the title of the {@link Processor}.
     * 
     * <p>
     * Unlike {@link #getProcessorName()}, result {@link String} here can depend on the instance.
     * 
     * @return The title of a {@link Processor}.
     */
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
