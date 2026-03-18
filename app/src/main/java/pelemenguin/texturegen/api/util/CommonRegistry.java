package pelemenguin.texturegen.api.util;

import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;

public class CommonRegistry<R extends CommonRegistry.Registrable<R>> {

    private Class<R> baseType;
    private HashMap<String, R> registry = new HashMap<>();
    private HashMap<R, String> inversedRegistry = new HashMap<>();

    private boolean loaded = false;

    public CommonRegistry(Class<R> baseType) {
        this.baseType = baseType;
    }

    public synchronized void register(String id, R obj) {
        registry.put(id, obj);
    }

    public Set<String> getRegisteredIds() {
        ensureServiceLoaded();
        return Set.copyOf(registry.keySet());
    }

    public R get(String id) {
        ensureServiceLoaded();
        return registry.get(id);
    }

    public String getIdOf(R obj) {
        ensureServiceLoaded();
        return inversedRegistry.get(obj);
    }

    public synchronized void reloadFromServiceLoader() {
        ServiceLoader<R> loader = ServiceLoader.load(baseType);

        for (Registrable<R> obj : loader) {
            try {
                obj.register(this);
            } catch (Throwable t) {
                // Ignore
            }
        }
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

    public static interface Registrable<R extends Registrable<R>> {
        public void register(CommonRegistry<R> registry);
    }

}
