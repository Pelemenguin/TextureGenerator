package pelemenguin.texturegen.api.util;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ListEditorMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;

public interface PointFilter extends JsonRegistry.Registrable<PointFilter> {

    public static final JsonRegistry<PointFilter> REGISTRY = new JsonRegistry<>(PointFilter.class, (registry, filter) -> {
        PrivateObjectHolder.ID_TO_NAME.put(registry.getIdOf(filter.getClass()), filter.getPointFilterName());
    }).setFallbackId("texturegen.error");
    // public static final JsonRegistry<PointFilter> REGISTRY = new JsonRegistry<>(PointFilter.class);
    public static final TypeAdapterFactory TYPE_ADAPTER = REGISTRY.createTypeAdapterFactory();
    public static final Gson GSON = REGISTRY.createGsonBuilder()
        .registerTypeAdapterFactory(TYPE_ADAPTER)
        .setPrettyPrinting()
        .create();

    public static class PrivateObjectHolder {
        private static final HashMap<String, String> ID_TO_NAME = new HashMap<>();
    }

    public static String getNameFor(String processorId) {
        return PrivateObjectHolder.ID_TO_NAME.get(processorId);
    }

    /**
     * Filters the points of the given image and writes the result to the given {@code maskResult} image.
     * The {@code maskResult} image should be a binary image where white pixels represent points that passed the filter and black pixels represent points that failed the filter.
     * 
     * @param image      The image to filter
     * @param maskResult The image to write the filter result to.
     *                   This image should be a binary image ({@link BufferedImage#TYPE_BYTE_BINARY}) where white pixels represent points that passed the filter and black pixels represent points that failed the filter.
     *                   The image passed to this method is guaranteed to have the same dimensions as the {@code image} parameter.
     * 
     *                   <p>
     *                   All pixels in this image are initially black (0) and should be modified to white (1) for points that pass the filter.
     */
    void filter(BufferedImage image, BufferedImage maskResult);

    public default String getPointFilterName() {
        return REGISTRY.getIdOf(this.getClass());
    }

    public default String getPointFilterTitle() {
        return this.toString();
    }

    public default BufferedImage filter(BufferedImage image) {
        BufferedImage maskResult = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        filter(image, maskResult);
        return maskResult;
    }

    public default Iterator<Point> pointIterator(BufferedImage image) {
        BufferedImage maskImage = this.filter(image);
        Raster raster = maskImage.getData();
        int width = maskImage.getWidth();
        int height = maskImage.getHeight();
        return new Iterator<Point>() {
            
            private int x = 0;
            private int y = 0;
            private Point next = findNext();

            private Point findNext() {
                while (y < height) {
                    if (raster.getSample(x, y, 0) != 0) {
                        Point point = new Point(x, y);
                        x++;
                        if (x >= width) {
                            x = 0;
                            y++;
                        }
                        return point;
                    }
                    x++;
                    if (x >= width) {
                        x = 0;
                        y++;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Point next() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                Point current = next;
                next = findNext();
                return current;
            }

        };
    }

    public default void forEachPixel(BufferedImage image, Consumer<Point> action) {
        this.pointIterator(image).forEachRemaining(action);
    }

    public static AlwaysPass alwaysPass() {
        return AlwaysPass.INSTANCE;
    }

    public static AlwaysFail alwaysFail() {
        return AlwaysFail.INSTANCE;
    }

    public static And and(PointFilter... filters) {
        And and = new And();
        and.filters = List.of(filters);
        return and;
    }

    public static Or or(PointFilter... filters) {
        Or or = new Or();
        or.filters = List.of(filters);
        return or;
    }

    public static Not not(PointFilter filter) {
        Not not = new Not();
        not.filter = filter;
        return not;
    }

    public static class ErrorPointFilter implements PointFilter, JsonRegistry.ErrorFallback<PointFilter> {

        private JsonElement raw;
        private Throwable cause;
        private JsonRegistry.DeserializationFailedReason reason;

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.error", ErrorPointFilter.class);
        }

        @Override
        public JsonElement getRawJson() {
            return this.raw;
        }

        @Override
        public Throwable getCause() {
            return this.cause;
        }

        @Override
        public void setRawJson(JsonElement json) {
            this.raw = json;
        }

        @Override
        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public void setReason(JsonRegistry.DeserializationFailedReason reason) {
            this.reason = reason;
        }

        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            throw new IllegalStateException("Cannot filter with error point filter", cause);
        }

        @Override
        public String getPointFilterName() {
            return "[ERROR]";
        }

        @Override
        public String getPointFilterTitle() {
            return switch (this.reason) {
                case MISSING_TYPE -> "[UNKNOWN]";
                case TYPE_NOT_FOUND -> this.raw.getAsJsonObject().get("type") + " [UNRECOGNIZED]";
                case ADAPTER_THREW_EXCEPTION -> this.raw.getAsJsonObject().get("type") + " [BROKEN]";
            };
        }

    }

    public static class AlwaysPass implements PointFilter {

        public static final AlwaysPass INSTANCE = new AlwaysPass();

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.always_pass", AlwaysPass.class, new TypeAdapter<AlwaysPass>() {

                @Override
                public void write(JsonWriter out, AlwaysPass value) throws IOException {
                    out.beginObject();
                    out.endObject();
                }

                @Override
                public AlwaysPass read(JsonReader in) throws IOException {
                    in.skipValue();
                    return AlwaysPass.INSTANCE;
                }

            });
        }
        
        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            WritableRaster raster = maskResult.getRaster();
            int width = maskResult.getWidth();
            int height = maskResult.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    raster.setSample(x, y, 0, 1);
                }
            }
        }

        @Override
        public Iterator<Point> pointIterator(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();
            return new Iterator<Point>() {
                
                private int x = 0;
                private int y = 0;

                @Override
                public boolean hasNext() {
                    return y < height;
                }

                @Override
                public Point next() {
                    if (y >= height) {
                        throw new NoSuchElementException();
                    }
                    Point point = new Point(x, y);
                    x++;
                    if (x >= width) {
                        x = 0;
                        y++;
                    }
                    return point;
                }

            };
        }

        @Override
        public String getPointFilterName() {
            return "Always Pass";
        }
        
        @Override
        public String toString() {
            return "Always Pass";
        }

    }

    public static class AlwaysFail implements PointFilter {

        public static final AlwaysFail INSTANCE = new AlwaysFail();

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.always_fail", AlwaysFail.class, new TypeAdapter<AlwaysFail>() {

                @Override
                public void write(JsonWriter out, AlwaysFail value) throws IOException {
                    out.beginObject();
                    out.endObject();
                }

                @Override
                public AlwaysFail read(JsonReader in) throws IOException {
                    in.skipValue();
                    return AlwaysFail.INSTANCE;
                }

            });
        }
        
        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            // Do nothing, all pixels are already black (0xFF000000)
        }

        @Override
        public Iterator<Point> pointIterator(BufferedImage image) {
            return new Iterator<Point>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Point next() {
                    throw new NoSuchElementException();
                }

            };
        }

        @Override
        public String getPointFilterName() {
            return "Always Fail";
        }

        @Override
        public String toString() {
            return "Always Fail";
        }

    }

    public static class And implements PointFilter {

        private List<PointFilter> filters = new ArrayList<>();

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.and", And.class);
        }

        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            if (filters.size() == 0) {
                // If there are no filters, pass all points
                AlwaysPass.INSTANCE.filter(image, maskResult);
                return;
            }
            Raster[] masks = new Raster[filters.size()];
            for (int i = 0; i < filters.size(); i++) {
                BufferedImage mask = filters.get(i).filter(image);
                masks[i] = mask.getData();
            }
            WritableRaster raster = maskResult.getRaster();
            int width = maskResult.getWidth();
            int height = maskResult.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean pass = true;
                    for (Raster mask : masks) {
                        if (mask.getSample(x, y, 0) == 0) {
                            pass = false;
                            break;
                        }
                    }
                    raster.setSample(x, y, 0, pass ? 1 : 0);
                }
            }
        }

        @Override
        public String getPointFilterName() {
            return "And";
        }

        @Override
        public String getPointFilterTitle() {
            return "And (%d filters)".formatted(filters.size());
        }

        @Override
        public String toString() {
            return "And" + this.filters;
        }

        public static class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<And> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.and", this);
            }

            @Override
            public void editorLoop(And pointFilter, Consumer<And> setter, TerminalMenuContext context) {
                ListEditorMenu<PointFilter> editor = new ListEditorMenu<>(pointFilter.filters, (filter, filterSetter) -> {
                    if (filter == null) {
                        TerminalPointFilterEditorProvider.getSelectionList(filterSetter, context).run();
                    } else {
                        TerminalPointFilterEditorProvider.getEditorLoop(filter, filterSetter, context).run();
                    }
                })
                    .strigifier(PointFilter::getPointFilterTitle)
                    .description("Edit filters for And point filter:");
                editor.loop(context);
            }

            @Override
            public Editor<? extends PointFilter> getEditor() {
                return this;
            }

        }

    }

    public static class Or implements PointFilter {

        private List<PointFilter> filters = new ArrayList<>();

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.or", Or.class);
        }

        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            if (filters.size() == 0) {
                // If there are no filters, fail all points
                AlwaysFail.INSTANCE.filter(image, maskResult);
                return;
            }
            Raster[] masks = new Raster[filters.size()];
            for (int i = 0; i < filters.size(); i++) {
                BufferedImage mask = filters.get(i).filter(image);
                masks[i] = mask.getData();
            }
            WritableRaster raster = maskResult.getRaster();
            int width = maskResult.getWidth();
            int height = maskResult.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean pass = false;
                    for (Raster mask : masks) {
                        if (mask.getSample(x, y, 0) != 0) {
                            pass = true;
                            break;
                        }
                    }
                    raster.setSample(x, y, 0, pass ? 1 : 0);
                }
            }
        }

        @Override
        public String getPointFilterName() {
            return "Or";
        }

        @Override
        public String getPointFilterTitle() {
            return "Or (%d filters)".formatted(filters.size());
        }

        @Override
        public String toString() {
            return "Or" + filters;
        }

        public static class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<Or> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.or", this);
            }

            @Override
            public void editorLoop(Or pointFilter, Consumer<Or> setter, TerminalMenuContext context) {
                ListEditorMenu<PointFilter> editor = new ListEditorMenu<>(pointFilter.filters, (filter, filterSetter) -> {
                    if (filter == null) {
                        TerminalPointFilterEditorProvider.getSelectionList(filterSetter, context).run();
                    } else {
                        TerminalPointFilterEditorProvider.getEditorLoop(filter, filterSetter, context).run();
                    }
                })
                    .strigifier(PointFilter::getPointFilterTitle)
                    .description("Edit filters for Or point filter:");
                editor.loop(context);
            }

            @Override
            public Editor<? extends PointFilter> getEditor() {
                return this;
            }

        }

    }

    public static class Not implements PointFilter {

        PointFilter filter = AlwaysPass.INSTANCE;

        @Override
        public void register(JsonRegistry<PointFilter> registry) {
            registry.register("texturegen.not", Not.class);
        }

        @Override
        public void filter(BufferedImage image, BufferedImage maskResult) {
            if (filter == null) {
                throw new IllegalStateException("Filter cannot be null");
            }
            filter.filter(image, maskResult);
            Raster mask = maskResult.getData();
            WritableRaster raster = maskResult.getRaster();
            int width = maskResult.getWidth();
            int height = maskResult.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    raster.setSample(x, y, 0, 1 - mask.getSample(x, y, 0));
                }
            }
        }

        @Override
        public String getPointFilterName() {
            return "Not";
        }

        @Override
        public String getPointFilterTitle() {
            return "Not (%s)".formatted(filter == null ? "null" : filter.getPointFilterTitle());
        }

        @Override
        public String toString() {
            return "Not(" + filter + ")";
        }

        public static class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<Not> {

            @Override
            public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
                registry.register("texturegen.not", this);
            }

            @Override
            public void editorLoop(Not pointFilter, Consumer<Not> setter, TerminalMenuContext context) {
                TerminalMenu menu = new TerminalMenu("Edit Not Point Filter")
                    .autoUppercase()
                    .addKey('-', "Back")
                    .addKey('E', "", () -> {
                        if (pointFilter.filter == null) {
                            TerminalPointFilterEditorProvider.getSelectionList(newFilter -> {
                                pointFilter.filter = newFilter;
                                setter.accept(pointFilter);
                            }, context).run();
                        } else {
                            TerminalPointFilterEditorProvider.getEditorLoop(pointFilter.filter, newFilter -> {
                                pointFilter.filter = newFilter;
                                setter.accept(pointFilter);
                            }, context).run();
                        }
                    }).addKey('R', "Replace inner filter", () -> {
                        TerminalPointFilterEditorProvider.getSelectionList(newFilter -> {
                            pointFilter.filter = newFilter;
                            setter.accept(pointFilter);
                        }, context).run();
                    });
                while (true) {
                    menu.updateKeyDescription('E', "Edit inner filter (Current: %s)".formatted(pointFilter.filter == null ? ANSIHelper.red("Unset") : ANSIHelper.blue(pointFilter.filter.getPointFilterTitle())));
                    char c = menu.scan(context);
                    if (c == '-') {
                        break;
                    }
                }
            }

            @Override
            public Editor<? extends PointFilter> getEditor() {
                return this;
            }

        }

    }

}
