package pelemenguin.texturegen.api.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ServiceLoader;
import java.util.Set;
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
    private LinkedHashMap<String, Class<? extends R>> typeRegistry = new LinkedHashMap<>();
    private HashMap<Class<? extends R>, String> inversedTypeRegistry = new HashMap<>();
    private HashMap<Class<? extends R>, TypeAdapter<? extends R>> typeAdapters = new HashMap<>();

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
                        throw new JsonParseException("Unregistered type: " + clazz.getName());
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
                            throw new JsonParseException("Null value is not allowed for type: " + type.getRawType().getName());
                        }
                        in.nextNull();
                        return null;
                    }

                    JsonObject jsonObject = gson.fromJson(in, JsonObject.class);
                    JsonElement typeElement = jsonObject.get(typeFieldName);
                    if (typeElement == null) {
                        throw new JsonParseException("Missing type field: " + typeFieldName);
                    }
                    String id = typeElement.getAsString();
                    Class<? extends R> clazz = registry.getClassOf(id);
                    if (clazz == null) {
                        throw new JsonParseException("Unknown type id: " + id);
                    }
                    TypeAdapter<R> typeAdapter = (TypeAdapter<R>) registry.getTypeAdapter(id);
                    if (typeAdapter != null) {
                        return (T) typeAdapter.fromJsonTree(jsonObject);
                    } else {
                        TypeAdapter<R> delegateAdapter = (TypeAdapter<R>) gson.getDelegateAdapter(RegistryTypeAdapterFactory.this, TypeToken.get(clazz));
                        return (T) delegateAdapter.fromJsonTree(jsonObject);
                    }
                }

            };
        }
    }

}
