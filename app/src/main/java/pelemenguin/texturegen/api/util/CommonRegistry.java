package pelemenguin.texturegen.api.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;

public class CommonRegistry<R extends CommonRegistry.Registrable<R>> {

    public static final Comparator<String> ID_COMPARATOR = (s1, s2) -> {
        int lastDot1 = s1.lastIndexOf('.');
        int lastDot2 = s2.lastIndexOf('.');
        if (lastDot1 == -1) {
            if (lastDot2 == -1) {
                return s1.compareTo(s2);
            } else {
                return -1;
            }
        } else if (lastDot2 == -1) {
            return 1;
        } else {
            String prefix1 = s1.substring(0, lastDot1);
            String prefix2 = s2.substring(0, lastDot2);
            int cmp = prefix1.compareTo(prefix2);
            if (cmp != 0) {
                return cmp;
            } else {
                String suffix1 = s1.substring(lastDot1 + 1);
                String suffix2 = s2.substring(lastDot2 + 1);
                return suffix1.compareTo(suffix2);
            }
        }
    };

    private Class<R> baseType;
    private BiConsumer<CommonRegistry<R>, R> extraRegistryAction;
    private TreeMap<String, R> registry = new TreeMap<>(ID_COMPARATOR);
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

    public synchronized Set<String> getRegisteredIds() {
        ensureServiceLoaded();
        TreeSet<String> result = new TreeSet<>(ID_COMPARATOR);
        result.addAll(registry.keySet());
        return result;
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
