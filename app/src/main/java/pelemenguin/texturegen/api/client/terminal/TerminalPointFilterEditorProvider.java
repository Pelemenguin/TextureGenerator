package pelemenguin.texturegen.api.client.terminal;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public interface TerminalPointFilterEditorProvider extends CommonRegistry.Registrable<TerminalPointFilterEditorProvider> {
    
    public static final CommonRegistry<TerminalPointFilterEditorProvider> REGISTRY = new CommonRegistry<>(TerminalPointFilterEditorProvider.class);

    Editor<? extends PointFilter> getEditor();

    public static Runnable getEditorLoop(PointFilter filter, Consumer<PointFilter> setter, TerminalMenuContext context) {
        Class<? extends PointFilter> filterClass = filter.getClass();
        String id = PointFilter.REGISTRY.getIdOf(filterClass);
        if (id == null) {
            return () -> {
                context.outStream().println(ANSIHelper.red("Point filter type not registered: " + filterClass.getName()));
            };
        }
        TerminalPointFilterEditorProvider provider = REGISTRY.get(id);
        if (provider == null) {
            return () -> {
                context.outStream().println(ANSIHelper.red("No terminal editor provider for point filter type: " + id));
            };
        }
        return () -> ((Editor<PointFilter>) provider.getEditor()).editorLoop(filter, setter, context);
    }

    public static Runnable getSelectionList(Consumer<PointFilter> setter, TerminalMenuContext context) {
        return () -> {
            Set<String> registeredIds = PointFilter.REGISTRY.getRegisteredIds();
            String resultId = new ListEditorMenu<>(List.copyOf(registeredIds))
                .strigifier(PointFilter::getNameFor)
                .description("Select a new Point Filter:")
                .loop(context);
            if (resultId == null) {
                return;
            }
            Class<? extends PointFilter> result = PointFilter.REGISTRY.getClassOf(resultId);
            if (result == null) {
                context.outStream().println(ANSIHelper.red("Invalid point filter type: " + resultId));
                return;
            }
            try {
                PointFilter newInstance = result.getConstructor().newInstance();
                setter.accept(newInstance);
            } catch (Throwable t) {
                context.outStream().println(ANSIHelper.red("Failed to create point filter instance: " + t.getMessage()));
                t.printStackTrace();
            }
        };
    }

    public static interface Editor<F extends PointFilter> {
        void editorLoop(F pointFilter, Consumer<F> setter, TerminalMenuContext context);
    }

}
