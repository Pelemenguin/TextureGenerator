package pelemenguin.texturegen.api.client.terminal;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.NoiseProvider;

public interface TerminalNoiseProviderEditorProvider extends CommonRegistry.Registrable<TerminalNoiseProviderEditorProvider> {

    public static final CommonRegistry<TerminalNoiseProviderEditorProvider> REGISTRY = new CommonRegistry<>(TerminalNoiseProviderEditorProvider.class);

    Editor<? extends NoiseProvider> getEditor();

    public static Runnable getEditorLooop(Supplier<NoiseProvider> getter, Consumer<NoiseProvider> setter, TerminalMenuContext context) {
        return () -> {
            NoiseProvider noiseProvider = getter.get();
            Class<? extends NoiseProvider> noiseClass = noiseProvider.getClass();
            String id = NoiseProvider.REGISTRY.getIdOf(noiseClass);
            if (id == null) {
                context.outStream().println(ANSIHelper.red("Noise provider type not registered: " + noiseClass.getName()));
                return;
            }
            TerminalNoiseProviderEditorProvider provider = REGISTRY.get(id);
            if (provider == null) {
                context.outStream().println(ANSIHelper.red("No terminal editor provider for noise provider type: " + id));
                return;
            }
            ((Editor<NoiseProvider>) provider.getEditor()).editorLoop(noiseProvider, setter, context);
        };
    }

    public static Runnable getSelectionList(Consumer<NoiseProvider> setter, TerminalMenuContext context) {
        return () -> {
            Set<String> registeredIds = NoiseProvider.REGISTRY.getRegisteredIds();
            String resultId = new ListEditorMenu<>(List.copyOf(registeredIds))
                .strigifier(NoiseProvider::getNameFor)
                .description("Select a new Noise Provider:")
                .loop(context);
            if (resultId == null) {
                return;
            }
            Class<? extends NoiseProvider> result = NoiseProvider.REGISTRY.getClassOf(resultId);
            if (result == null) {
                context.outStream().println(ANSIHelper.red("Invalid noise provider type: " + resultId));
                return;
            }
            try {
                NoiseProvider newInstance = result.getConstructor().newInstance();
                setter.accept(newInstance);
            } catch (Throwable t) {
                context.outStream().println(ANSIHelper.red("Failed to create noise provider instance: " + t.getMessage()));
                t.printStackTrace();
            }
        };
    }

    public static interface Editor<P extends NoiseProvider> {
        void editorLoop(P noiseProvider, Consumer<P> setter, TerminalMenuContext context);
    }

}
