package pelemenguin.texturegen.api.util;

import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;

public class CommonRegistry<R extends CommonRegistry.Registrable<R>> {

    private Class<R> baseType;
    private BiConsumer<CommonRegistry<R>, R> extraRegistryAction;
    private HashMap<String, R> registry = new HashMap<>();
    private HashMap<R, String> inversedRegistry = new HashMap<>();

    private boolean loaded = false;

    public CommonRegistry(Class<R> baseType, BiConsumer<CommonRegistry<R>, R> extraRegistryAction) {
        this.baseType = baseType;
        this.extraRegistryAction = extraRegistryAction;
    }

    public CommonRegistry(Class<R> baseType) {
        this(baseType, (r1, r2) -> {});
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

        this.loaded = true;
        for (R obj : loader) {
            try {
                obj.register(this);
                this.extraRegistryAction.accept(this, obj);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    public synchronized void ensureServiceLoaded() {
        if (!loaded) {
            reloadFromServiceLoader();
        }
    }

    public synchronized void markAsUnloaded() {
        loaded = false;
    }

    public static interface Registrable<R extends Registrable<R>> {
        public void register(CommonRegistry<R> registry);
    }

}
