package pelemenguin.texturegen.api.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class JsonRegistry<R extends JsonRegistry.Registrable<R>> {

    private Class<R> baseType;
    private BiConsumer<JsonRegistry<R>, R> extraRegistryAction;
    private String typeField = "type";
    private boolean allowNull = false;
    private HashMap<String, Class<? extends R>> typeRegistry = new HashMap<>();
    private HashMap<Class<? extends R>, String> inversedTypeRegistry = new HashMap<>();
    private HashMap<Class<? extends R>, TypeAdapter<? extends R>> typeAdapters = new HashMap<>();
    private String errorFallbackId = null;

    private boolean loaded = false;

    public JsonRegistry(Class<R> baseType, BiConsumer<JsonRegistry<R>, R> extraRegistryAction) {
        this.baseType = baseType;
        this.extraRegistryAction = extraRegistryAction;
    }

    public JsonRegistry(Class<R> baseType) {
        this(baseType, (r1, r2) -> {});
    }

    public synchronized <E extends R> void register(String id, Class<E> type, TypeAdapter<E> typeAdapter) {
        typeRegistry.put(id, type);
        inversedTypeRegistry.put(type, id);
        typeAdapters.put(type, typeAdapter);
    }

    public <E extends R> void register(String id, Class<E> type) {
        register(id, type, null);
    }

    public Set<String> getRegisteredIds() {
        ensureServiceLoaded();
        TreeSet<String> result = new TreeSet<>(CommonRegistry.ID_COMPARATOR);
        result.addAll(typeRegistry.keySet());
        if (errorFallbackId != null) result.remove(errorFallbackId);
        return result;
    }

    public boolean isIdRegistered(String id) {
        ensureServiceLoaded();
        return typeRegistry.containsKey(id);
    }

    public Class<? extends R> getClassOf(String id) {
        ensureServiceLoaded();
        return typeRegistry.get(id);
    }

    public String getIdOf(Class<? extends R> type) {
        ensureServiceLoaded();
        return inversedTypeRegistry.get(type);
    }
    
    public TypeAdapter<? extends R> getTypeAdapter(String id) {
        ensureServiceLoaded();
        var type = typeRegistry.get(id);
        if (type == null) {
            return null;
        }
        return typeAdapters.get(type);
    }

    public synchronized void ensureServiceLoaded() {
        if (!loaded) {
            reloadFromServiceLoader();
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
        typeAdapters.clear();
        this.loaded = true;
        for (R entry : loader) {
            try {
                entry.register(this);
                this.extraRegistryAction.accept(this, entry);
            } catch (Throwable e) {
                // Ignore
            }
        }
    }

    public synchronized JsonRegistry<R> setTypeField(String field) {
        this.typeField = field;
        return this;
    }

    public synchronized JsonRegistry<R> allowNull() {
        this.allowNull = true;
        return this;
    }

    public synchronized JsonRegistry<R> setFallbackId(String id) {
        this.errorFallbackId = id;
        return this;
    }

    public TypeAdapterFactory createTypeAdapterFactory() {
        return new RegistryTypeAdapterFactory<>(this);
    }

    public GsonBuilder addToGsonBuilder(GsonBuilder builder) {
        builder.registerTypeAdapterFactory(createTypeAdapterFactory());
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

    public static interface ErrorFallback<R extends JsonRegistry.Registrable<R>> {

        public JsonElement getRawJson();
        public Throwable getCause();
        public void setRawJson(JsonElement json);
        public void setCause(Throwable cause);

        public default void setReason(DeserializationFailedReason reason) {
        }

    }

    public static enum DeserializationFailedReason {
        MISSING_TYPE, TYPE_NOT_FOUND, ADAPTER_THREW_EXCEPTION;
    }

    private static class RegistryTypeAdapterFactory<R extends JsonRegistry.Registrable<R>> implements TypeAdapterFactory {

        private JsonRegistry<R> registry;

        private RegistryTypeAdapterFactory(JsonRegistry<R> registry) {
            this.registry = registry;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!registry.baseType.isAssignableFrom(type.getRawType())) {
                return null;
            }

            String typeFieldName = registry.typeField;
            boolean allowNull = registry.allowNull;
            String errorFallbackId = registry.errorFallbackId;

            return new TypeAdapter<T>() {

                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    registry.ensureServiceLoaded();

                    if (value == null) {
                        if (!allowNull) {
                            throw new JsonParseException("Null value is not allowed for type: " + type.getRawType().getName());
                        }
                        out.nullValue();
                        return;
                    }

                    Class<? extends R> clazz = (Class<? extends R>) value.getClass();
                    String id = registry.getIdOf(clazz);
                    if (id == null) {
                        // We have to throw here as we don't know how to deserialize this
                        throw new JsonParseException("Unregistered type: " + clazz.getName());
                    } else if (id.equals(errorFallbackId)) {
                        // Just write the raw Json back
                        JsonElement rawJsonElement = ((ErrorFallback<R>) value).getRawJson();
                        out.jsonValue(rawJsonElement.toString());
                        return;
                    }
                    TypeAdapter<R> typeAdapter = (TypeAdapter<R>) registry.getTypeAdapter(id);
                    if (typeAdapter == null) {
                        typeAdapter = (TypeAdapter<R>) gson.getDelegateAdapter(RegistryTypeAdapterFactory.this, TypeToken.get(clazz));
                    }
                    JsonObject jsonObject;
                    try {
                        jsonObject = typeAdapter.toJsonTree((R) value).getAsJsonObject();
                    } catch (IllegalStateException e) {
                        throw new JsonParseException("Failed to serialize type: " + clazz.getName() + ", the result JsonElement is not an object", e);
                    }
                    jsonObject.addProperty(typeFieldName, id);
                    gson.toJson(jsonObject, out);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    registry.ensureServiceLoaded();

                    if (in.peek() == JsonToken.NULL) {
                        if (!allowNull) {
                            // We don't use fallback here
                            throw new JsonParseException("Null value is not allowed for type: " + type.getRawType().getName());
                        }
                        in.nextNull();
                        return null;
                    }

                    JsonObject jsonObject = gson.fromJson(in, JsonObject.class);
                    JsonElement typeElement = jsonObject.get(typeFieldName);
                    if (typeElement == null) {
                        return useFallback(typeFieldName, jsonObject, new JsonParseException("Missing type field: " + typeFieldName), DeserializationFailedReason.MISSING_TYPE);
                    }
                    String id = typeElement.getAsString();
                    Class<? extends R> clazz = registry.getClassOf(id);
                    if (clazz == null) {
                        return useFallback(typeFieldName, jsonObject, new JsonParseException("Unregistered type id: " + id), DeserializationFailedReason.TYPE_NOT_FOUND);
                    }
                    TypeAdapter<R> typeAdapter = (TypeAdapter<R>) registry.getTypeAdapter(id);
                    try {
                        if (typeAdapter != null) {
                            return (T) typeAdapter.fromJsonTree(jsonObject);
                        } else {
                            TypeAdapter<R> delegateAdapter = (TypeAdapter<R>) gson.getDelegateAdapter(RegistryTypeAdapterFactory.this, TypeToken.get(clazz));
                            return (T) delegateAdapter.fromJsonTree(jsonObject);
                        }
                    } catch (Throwable e) {
                        return useFallback(typeFieldName, jsonObject, new JsonParseException(e), DeserializationFailedReason.ADAPTER_THREW_EXCEPTION);
                    }
                }

                private T useFallback(String typeFieldName, JsonObject jsonObject, JsonParseException cause, DeserializationFailedReason reason) {
                    if (errorFallbackId == null) {
                        throw cause;
                    } else {
                        // Use fallback
                        Class<? extends R> fallbackClass = registry.getClassOf(errorFallbackId);
                        try {
                            ErrorFallback<? extends R> fallback = (ErrorFallback<? extends R>) fallbackClass.getConstructor().newInstance();
                            fallback.setCause(cause);
                            fallback.setRawJson(jsonObject);
                            fallback.setReason(reason);
                            return (T) fallback;
                        } catch (Throwable t) {
                            throw new JsonParseException("Failed to create error fallback instance of type: " + fallbackClass.getName(), t);
                        }
                    }
                }

            };
        }
    }

}
