package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.ColorPickerMenu;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public class ImageRecolorer implements Processor {

    public Palette palette;
    public PointFilter filter = PointFilter.alwaysPass();

    /**
     * Constructs an {@link ImageRecolorer} with a default palette that maps 0 to black and 255 to white, and linearly interpolates in between.
     * 
     * <p>
     * This constructor is provided for Java SPI and should not be used directly.
     * Use {@link #builder()} instead to create an instance with a custom palette.
     * 
     * @see #builder()
     */
    public ImageRecolorer() {
        this.palette = new Palette();
    }

    private ImageRecolorer(Palette palette) {
        this.palette = palette;
    }

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage image = parameters.load(0, BufferedImage.class);
        // Just modify the original image
        this.filter.forEachPixel(context, image, point -> {
            int x = point.x();
            int y = point.y();
            int color = image.getRGB(x, y);
            // Skip if transparent
            int a = (color >> 24) & 0xFF;
            if (a == 0) {
                return;
            }
            // Skip if not grey
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            if (r != g || g != b) {
                return;
            }
            image.setRGB(x, y, this.palette.getColor(r));
        });
        result.push(0, image);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Image Recolorer";
    }

    @Override
    public String getProcessorTitle() {
        return "Image Recolorer";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((palette == null) ? 0 : palette.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageRecolorer other = (ImageRecolorer) obj;
        if (palette == null) {
            if (other.palette != null)
                return false;
        } else if (!palette.equals(other.palette))
            return false;
        return true;
    }

    public static Palette builder() {
        return new Palette();
    }

    public static class Palette {

        private TreeMap<Integer, Integer> colors = new TreeMap<>();

        private int[] cachedColors = new int[256];

        private Palette() {}

        /**
         * Adds a color mapping from a grey value to an ARGB color. The grey value must be between 0 and 255, inclusive.
         * 
         * @param grey      The grey value to map, must be between 0 and 255, inclusive
         * @param colorARGB The ARGB color to map to, in the format 0xAARRGGBB
         * @return          This palette instance, for chaining
         */
        public Palette putColor(int grey, int colorARGB) {
            colors.put(grey, colorARGB);
            return this;
        }

        /**
         * Gets the ARGB color for a given grey value. The grey value must be between 0 and 255, inclusive.
         * 
         * @return The ARGB color mapped to the given grey value, in the format 0xAARRGGBB
         */
        public int getColor(int grey) {
            if (grey < 0 || grey > 255) {
                throw new IllegalArgumentException("Grey value must be between 0 and 255");
            }
            return cachedColors[grey];
        }

        /**
         * Refreshes the internal cache of colors based on the current color mappings.
         * This should be called after adding all desired color mappings and before building the {@link ImageRecolorer}.
         */
        public void refreshCache() {
            // Calculate colors for each grey

            // Set 0 -> 0xFF000000 and 255 -> 0xFFFFFFFF if not in the palette
            if (!colors.containsKey(0)) {
                colors.put(0, 0xFF000000);
            }
            if (!colors.containsKey(255)) {
                colors.put(255, 0xFFFFFFFF);
            }

            int lastGrey = 0;
            int nextGrey = 1;
            int lastColor = this.colors.get(0);
            int nextColor;
            while (nextGrey < 256) {
                while (!this.colors.containsKey(nextGrey)) {
                    nextGrey++;
                }
                nextColor = this.colors.get(nextGrey);

                // Interpolate
                for (int grey = lastGrey; grey <= nextGrey; grey++) {
                    float t = (grey - lastGrey) / (float)(nextGrey - lastGrey);
                    int r = (int)(((nextColor >> 16) & 0xFF) * t + ((lastColor >> 16) & 0xFF) * (1 - t));
                    int g = (int)(((nextColor >> 8) & 0xFF) * t + ((lastColor >> 8) & 0xFF) * (1 - t));
                    int b = (int)((nextColor & 0xFF) * t + (lastColor & 0xFF) * (1 - t));
                    int a = (int)(((nextColor >> 24) & 0xFF) * t + ((lastColor >> 24) & 0xFF) * (1 - t));
                    cachedColors[grey] = (a << 24) | (r << 16) | (g << 8) | b;
                }

                lastGrey = nextGrey;
                lastColor = nextColor;
                nextGrey++;
            }
        }

        /**
         * Builds an {@link ImageRecolorer} instance with this palette.
         * After calling this method, the palette will be locked and cannot be modified.
         * 
         * @return An {@link ImageRecolorer} instance that uses this palette
         */
        public ImageRecolorer build() {
            this.refreshCache();
            return new ImageRecolorer(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((colors == null) ? 0 : colors.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Palette other = (Palette) obj;
            if (colors == null) {
                if (other.colors != null)
                    return false;
            } else if (!colors.equals(other.colors))
                return false;
            return true;
        }

    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.recolorer", ImageRecolorer.class, new Adapter());
    }

    @Override
    public void processorInit() {
        this.filter.pointFilterInit();
    }

    @Override
    public void processorFinalize() {
        this.filter.pointFilterFinalize();
    }

    public static class Adapter extends TypeAdapter<ImageRecolorer> {

        @Override
        public void write(JsonWriter out, ImageRecolorer value) throws IOException {
            out.beginObject();
            out.name("palette");
            out.beginObject();
            for (var entry : value.palette.colors.entrySet()) {
                out.name(Integer.toString(entry.getKey())).value(String.format("%08X", entry.getValue()));
            }
            out.endObject();
            out.name("filter");
            PointFilter.GSON.toJson(value.filter, PointFilter.class, out);
            out.endObject();
        }

        @Override
        public ImageRecolorer read(JsonReader in) throws IOException {
            Palette palette = ImageRecolorer.builder();
            PointFilter filter = null;
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                if (name.equals("palette")) {
                    in.beginObject();
                    while (in.hasNext()) {
                        String greyString = in.nextName();
                        int grey = Integer.parseInt(greyString);
                        int colorARGB = Integer.parseUnsignedInt(in.nextString(), 16);
                        palette.putColor(grey, colorARGB);
                    }
                    in.endObject();
                } else if (name.equals("filter")) {
                    filter = PointFilter.GSON.fromJson(in, PointFilter.class);
                } else {
                    in.skipValue();
                }
            }
            in.endObject();
            ImageRecolorer result = palette.build();
            result.filter = filter == null ? PointFilter.alwaysPass() : filter;
            return result;
        }

    }

    public static class Editor implements TerminalProcessorEditorProvider, TerminalProcessorEditorProvider.Editor<ImageRecolorer> {

        @Override
        public void register(CommonRegistry<TerminalProcessorEditorProvider> registry) {
            registry.register("texturegen.image.recolorer", this);
        }

        @Override
        public pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider.Editor<? extends Processor> getEditor() {
            return this;
        }

        @Override
        public void editorLoop(ImageRecolorer processor, Consumer<ImageRecolorer> setter, TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu("Image Recolorer")
                .autoUppercase()
                .addKey('-', "Back")
                .addKey('A', "Add or modify color", () -> {
                    String greyString = new StringInput("Enter grey value (0-255): (Leave empty to cancel)")
                        .allowEmpty()
                        .scan(context);
                    if (greyString.isBlank()) {
                        return;
                    }
                    int grey;
                    try {
                        grey = Integer.parseInt(greyString);
                        if (grey < 0 || grey > 255) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        context.outStream().println(ANSIHelper.red("Invalid grey value: " + greyString));
                        return;
                    }
                    int original = 0xFFFFFFFF;
                    if (processor.palette.colors.containsKey(grey)) {
                        original = processor.palette.colors.get(grey);
                    }
                    int colorARGB = new ColorPickerMenu(original).loop(context).getIntARGB();
                    processor.palette.putColor(grey, colorARGB);

                    processor.palette.refreshCache();
                })
                .addKey('D', "Delete color", () -> {
                    String greyString = new StringInput("Enter grey value to delete (0-255): (Leave empty to cancel)")
                        .allowEmpty()
                        .scan(context);
                    if (greyString.isBlank()) {
                        return;
                    }
                    int grey;
                    try {
                        grey = Integer.parseInt(greyString);
                        if (grey < 0 || grey > 255) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        context.outStream().println(ANSIHelper.red("Invalid grey value: " + greyString));
                        return;
                    }
                    Integer i = processor.palette.colors.remove(grey);
                    if (i == null) {
                        context.outStream().println(ANSIHelper.red("Grey value " + grey + " is not in the palette"));
                    }
                    processor.palette.refreshCache();
                })
                .addKey('F', "Point filter", () -> TerminalPointFilterEditorProvider.getEditorLoop(processor.filter, filter -> processor.filter = filter, context).run())
                .addKey('G', "Replace point filter", TerminalPointFilterEditorProvider.getSelectionList(filter -> processor.filter = filter, context));
            while (true) {
                StringBuilder descStringBuilder = new StringBuilder("Image Recolorer\n\nPalette:\n");

                for (var entry : processor.palette.colors.entrySet()) {
                    int gray = entry.getKey();
                    int color = entry.getValue();
                    if (ANSIHelper.ansiEnabled()) {
                        descStringBuilder.append(ANSIHelper.rgbBackground("      ", (gray << 16) | (gray << 8) | gray));
                    }
                    descStringBuilder.append(String.format(" %3d -> 0x%08X", gray, color));
                    if (ANSIHelper.ansiEnabled()) {
                        descStringBuilder.append(' ');
                        descStringBuilder.append(ANSIHelper.rgbBackground("      ", color));
                    }
                    descStringBuilder.append('\n');
                }

                descStringBuilder.append('\n');

                // Preview bar, length 52
                // Use lower half block in palette color
                // and background color in grey to show the grey value corresponding to the color
                if (ANSIHelper.ansiEnabled()) {
                    descStringBuilder.append("Preview:\n");
                    for (int i = 0; i < 52; i++) {
                        int grey = i * 255 / 51;
                        int color = processor.palette.getColor(grey);
                        descStringBuilder.append(ANSIHelper.rgbWithBackground("\u2584", color, (grey << 16) | (grey << 8) | grey));
                    }
                    descStringBuilder.append('\n');
                }

                menu.updateDescription(descStringBuilder.toString());
                menu.updateKeyDescription('F', "Point filter: " + (processor.filter == null ? ANSIHelper.red("Unset") : ANSIHelper.blue(processor.filter.getPointFilterTitle())));

                char c = menu.scan(context);

                if (c == '-') {
                    break;
                }
            }
            setter.accept(processor);
        }

    }

}
