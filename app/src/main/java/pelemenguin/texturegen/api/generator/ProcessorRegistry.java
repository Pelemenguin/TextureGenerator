package pelemenguin.texturegen.api.generator;

import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import pelemenguin.texturegen.client.terminal.ANSIHelper;

public class ProcessorRegistry {
    
    private static HashMap<String, Class<? extends Processor>> registry = new HashMap<>();
    private static HashMap<String, JsonDeserializer<? extends Processor>> deserializerRegistry = new HashMap<>();
    private static HashMap<String, JsonSerializer<? extends Processor>> serializerRegistry = new HashMap<>();
    
    public static void register(String id, Class<? extends Processor> processorClass, JsonSerializer<? extends Processor> serializer, JsonDeserializer<? extends Processor> deserializer) {
        registry.put(id, processorClass);
        if (deserializer != null) deserializerRegistry.put(id, deserializer);
        if (serializer != null) serializerRegistry.put(id, serializer);
    }

    public static void register(String id, Class<? extends Processor> processorClass) {
        register(id, processorClass, null, null);
    }

    public static void register(String id, Class<? extends Processor> processorClass, JsonDeserializer<? extends Processor> deserializer) {
        register(id, processorClass, null, deserializer);
    }

    public static void register(String id, Class<? extends Processor> processorClass, JsonSerializer<? extends Processor> serializer) {
        register(id, processorClass, serializer, null);
    }

    public static Class<? extends Processor> getProcessorClass(String id) {
        return registry.get(id);
    }

    public static JsonDeserializer<? extends Processor> getDeserializer(String id) {
        return deserializerRegistry.get(id);
    }

    public static void refreshService() {
        System.out.println("Loading processors from service loader...");
        // Clear the registry and re-register all processors from the service loader
        registry.clear();
        deserializerRegistry.clear();
        ServiceLoader<Processor> service = ServiceLoader.load(Processor.class);
        List<Provider<Processor>> processors = service.stream().toList();
        System.out.println("Found " + ANSIHelper.cyan(String.valueOf(processors.size())) + " processors");
        processors.forEach(processorProvider -> {
            boolean anyProblem = false;
            Processor processor = processorProvider.get();
            System.out.print("Registering processor: " + processor.getClass());
            ProcessorInfo info = processor.getClass().getAnnotation(ProcessorInfo.class);
            if (info != null) {
                String id = info.value();
                try {
                    Class<? extends JsonDeserializer<? extends Processor>> deserializerClass = info.deserializer();
                    Class<? extends JsonSerializer<? extends Processor>> serializerClass = info.serializer();
                    JsonDeserializer<? extends Processor> deserializer = null;
                    JsonSerializer<? extends Processor> serializer = null;
                    if (deserializerClass != ProcessorInfo.NoSerializer.class) {
                        try {
                            deserializer = deserializerClass.getDeclaredConstructor().newInstance();
                        } catch (Throwable t) {
                            System.out.print(ANSIHelper.red("\nFailed to instantiate deserializer for processor " + id + ": " + t.getMessage()));
                            anyProblem = true;
                        }
                    }
                    if (serializerClass != ProcessorInfo.NoSerializer.class) {
                        try {
                            serializer = serializerClass.getDeclaredConstructor().newInstance();
                        } catch (Throwable t) {
                            System.out.print(ANSIHelper.red("\nFailed to instantiate serializer for processor " + id + ": " + t.getMessage()));
                            anyProblem = true;
                        }
                    }
                    register(id, processor.getClass(), serializer, deserializer);
                    if (!anyProblem) {
                        System.out.println(ANSIHelper.green(" - Success"));
                    }
                } catch (Throwable t) {
                    register(id, processor.getClass());
                    System.out.println(ANSIHelper.red("\nFailed to register processor's serializer or deserializer " + id + ": " + t.getMessage()));
                }
            }
        });
        serviceLoaded = true;
    }

    private static boolean serviceLoaded = false;
    public static void ensureServiceLoaded() {
        if (!serviceLoaded) {
            refreshService();
        }
    }

}
