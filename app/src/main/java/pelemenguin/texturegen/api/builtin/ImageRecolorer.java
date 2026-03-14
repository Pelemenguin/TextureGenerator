package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;

public class ImageRecolorer implements Processor {

    private Palette palette;

    private ImageRecolorer(Palette palette) {
        this.palette = palette;
    }

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage image = parameters.load(0, BufferedImage.class);
        int width = image.getWidth();
        int height = image.getHeight();
        // Just modify the original image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                // Skip if not grey
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                if (r != g || g != b) {
                    continue;
                }
                // Skip if transparent
                int a = (color >> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }
                image.setRGB(x, y, this.palette.getColor(r));
            }
        }
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

    public static Palette builder() {
        return new Palette();
    }

    public static class Palette {

        private HashMap<Integer, Integer> colors = new HashMap<>();

        private boolean locked = false;
        private int[] cachedColors = new int[256];

        private Palette() {}

        public Palette putColor(int grey, int colorARGB) {
            if (locked) {
                throw new IllegalStateException("Palette is already built");
            }
            colors.put(grey, colorARGB);
            return this;
        }

        public int getColor(int grey) {
            if (grey < 0 || grey > 255) {
                throw new IllegalArgumentException("Grey value must be between 0 and 255");
            }
            return cachedColors[grey];
        }

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

        public ImageRecolorer build() {
            this.locked = true;
            this.refreshCache();
            return new ImageRecolorer(this);
        }

    }

}
