package pelemenguin.texturegen.util;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.generator.ProcessorRegistry;

public class ProcessorDeserializer implements JsonDeserializer<Processor> {

    public static final ProcessorDeserializer INSTANCE = new ProcessorDeserializer();

    private ProcessorDeserializer() {}

    @Override
    public Processor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            String processorType = json.getAsJsonObject().get("type").getAsString();
            if (processorType == null) {
                throw new JsonParseException("Processor JSON must contain 'type' and 'data' fields");
            }
            // Get the processor class from the registry
            Class<? extends Processor> processorClass = ProcessorRegistry.getProcessorClass(processorType);
            if (processorClass == null) {
                throw new JsonParseException("Unknown processor type: " + processorType);
            }
            // Get the deserializer for this processor type
            JsonDeserializer<? extends Processor> deserializer = ProcessorRegistry.getDeserializer(processorType);
            if (deserializer == null) {
                // Deserialize with no deserializer specified
                return context.deserialize(json, processorClass);
            } else {
                // Deserialize the processor data using the deserializer
                return deserializer.deserialize(json, processorClass, context);
            }
        } else {
            throw new JsonParseException("Processor JSON must be an object");
        }
    }

}
