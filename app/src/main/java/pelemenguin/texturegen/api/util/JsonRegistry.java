package pelemenguin.texturegen.api.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonRegistry<R extends JsonRegistry.Registrable<R>> {

    private Class<R> baseType;
    private HashMap<String, Class<? extends R>> typeRegistry = new HashMap<>();
    private HashMap<Class<? extends R>, String> inversedTypeRegistry = new HashMap<>();
    private HashMap<String, JsonSerializer<? extends R>> serializers = new HashMap<>();
    private HashMap<String, JsonDeserializer<? extends R>> deserializers = new HashMap<>();

    private boolean loaded = false;

    public JsonRegistry(Class<R> baseType) {
        this.baseType = baseType;
    }

    public synchronized <E extends R> void register(String id, Class<E> type, JsonSerializer<E> serializer, JsonDeserializer<E> deserializer) {
        typeRegistry.put(id, type);
        inversedTypeRegistry.put(type, id);
        serializers.put(id, serializer);
        deserializers.put(id, deserializer);
    }

    public <E extends R, T extends JsonSerializer<E> & JsonDeserializer<E>> void register(String id, Class<E> type, T serializerDeserializer) {
        register(id, type, serializerDeserializer, serializerDeserializer);
    }

    public <E extends R> void register(String id, Class<E> type) {
        register(id, type, null, null);
    }

    public Set<String> getRegisteredIds() {
        ensureServiceLoaded();
        return Set.copyOf(typeRegistry.keySet());
    }

    public Class<? extends R> getClassOf(String id) {
        ensureServiceLoaded();
        return typeRegistry.get(id);
    }

    public String getIdOf(Class<? extends R> type) {
        ensureServiceLoaded();
        return inversedTypeRegistry.get(type);
    }

    public JsonSerializer<? extends R> getSerializer(String id) {
        ensureServiceLoaded();
        return serializers.get(id);
    }

    public JsonDeserializer<? extends R> getDeserializer(String id) {
        ensureServiceLoaded();
        return deserializers.get(id);
    }

    public synchronized void ensureServiceLoaded() {
        if (!loaded) {
            reloadFromServiceLoader();
            loaded = true;
        }
    }

    public synchronized void markAsUnloaded() {
        loaded = false;
    }

    public synchronized void reloadFromServiceLoader() {
        ServiceLoader<R> loader = ServiceLoader.load(baseType);
        // Clear previous registries
        typeRegistry.clear();
        inversedTypeRegistry.clear();
        serializers.clear();
        deserializers.clear();
        for (R entry : loader) {
            try {
                entry.register(this);
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    public JsonSerializer<R> createSerializer() {
        return new Serializer();
    }

    public JsonDeserializer<R> createDeserializer() {
        return new Deserializer();
    }

    public GsonBuilder addToGsonBuilder(GsonBuilder builder) {
        builder.registerTypeAdapter(baseType, createSerializer());
        builder.registerTypeAdapter(baseType, createDeserializer());
        return builder;
    }
    
    public GsonBuilder createGsonBuilder() {
        return addToGsonBuilder(new GsonBuilder());
    }

    public Gson createGson() {
        return createGsonBuilder().setPrettyPrinting().create();
    }

    public static interface Registrable<R extends Registrable<R>> {
        public void register(JsonRegistry<R> registry);
    }

    private class Serializer implements JsonSerializer<R> {

        @Override
        public JsonElement serialize(R src, Type typeOfSrc, JsonSerializationContext context) {
            ensureServiceLoaded();
            var typeId = inversedTypeRegistry.get(src.getClass());
            if (typeId == null) {
                throw new JsonParseException("Unknown type: " + src.getClass().getName());
            }
            @SuppressWarnings("unchecked")
            var serializer = (JsonSerializer<R>) serializers.get(typeId);

            JsonObject result;
            if (serializer == null) {
                // Use default serializer provided by GSON
                try {
                    result = context.serialize(src).getAsJsonObject();
                } catch (Throwable e) {
                    throw new JsonParseException("Serialization failed for type: " + typeId, e);
                }
            } else {
                try {
                    result = serializer.serialize(src, typeOfSrc, context).getAsJsonObject();
                } catch (Throwable e) {
                    throw new JsonParseException("Serialization failed for type: " + typeId, e);
                }
            }

            result.addProperty("type", typeId);
            return result;
        }

    }

    private class Deserializer implements JsonDeserializer<R> {

        @Override
        public R deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            ensureServiceLoaded();
            if (!json.isJsonObject()) {
                throw new JsonParseException("Expected JSON object");
            }
            var jsonObject = json.getAsJsonObject();
            var typeElement = jsonObject.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive() || !typeElement.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("Expected 'type' field of type string");
            }
            var typeId = typeElement.getAsString();
            var deserializer = deserializers.get(typeId);

            R result;
            if (deserializer == null) {
                // Use default serializer provided by GSON
                var typeClass = typeRegistry.get(typeId);
                if (typeClass == null) {
                    throw new JsonParseException("Unknown type: " + typeId);
                }
                result = context.deserialize(json, typeClass);
            } else {
                try {
                    result = deserializer.deserialize(json, typeOfT, context);
                } catch (Throwable e) {
                    throw new JsonParseException("Deserialization failed for type: " + typeId, e);
                }
            }

            return result;
        }

    }

}
