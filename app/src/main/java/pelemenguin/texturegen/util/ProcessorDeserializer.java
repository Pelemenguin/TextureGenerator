package pelemenguin.texturegen.util;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.generator.ProcessorRegistry;

public class ProcessorDeserializer implements JsonDeserializer<Processor>, JsonSerializer<Processor> {

    public static final ProcessorDeserializer INSTANCE = new ProcessorDeserializer();

    private ProcessorDeserializer() {}

    @Override
    public Processor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            JsonElement typeField = json.getAsJsonObject().get("type");
            if (typeField == null || !typeField.isJsonPrimitive()) {
                throw new JsonParseException("Processor JSON must contain 'type' and fields");
            }
            String processorType = typeField.getAsString();
            if (processorType == null) {
                throw new JsonParseException("Processor JSON must contain 'type' and fields");
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

    @Override
    public JsonElement serialize(Processor src, Type typeOfSrc, JsonSerializationContext context) {
        // Get the processor type from the registry
        String processorType = ProcessorRegistry.getProcessorId(src.getClass());
        if (processorType == null) {
            throw new JsonParseException("Unknown processor class: " + src.getClass().getName());
        }
        // Get the serializer for this processor type
        JsonSerializer<? extends Processor> serializer = ProcessorRegistry.getSerializer(processorType);
        if (serializer == null) {
            // Serialize with no serializer specified
            JsonElement json = context.serialize(src, src.getClass());
            json.getAsJsonObject().addProperty("type", processorType);
            return json;
        } else {
            // Serialize the processor data using the serializer
            @SuppressWarnings("unchecked")
            JsonElement json = ((JsonSerializer<Processor>) serializer).serialize(src, src.getClass(), context);
            json.getAsJsonObject().addProperty("type", processorType);
            return json;
        }

    }

}
