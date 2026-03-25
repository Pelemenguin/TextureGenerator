package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

/**
 * {@code MinRectPointFilter} finds the minimum axis-aligned rectangle that can contain all non-transparent points in the input image,
 * and passes all the point within that rectangle
 */
public class MinRectPointFilter implements PointFilter {

    @Override
    public void register(JsonRegistry<PointFilter> registry) {
        registry.register("texturegen.image.min_rect", MinRectPointFilter.class);
    }

    @Override
    public void filter(GenerationContext context, BufferedImage image, BufferedImage maskResult) {
        int minX = image.getWidth(), minY = image.getHeight(), maxX = 0, maxY = 0;
        Raster raster = image.getRaster();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (raster.getSample(x, y, 3) > 0) { // Check if the pixel is non-transparent
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        // If no non-transparent pixels were found, return an empty mask
        if (minX > maxX || minY > maxY) {
            return;
        }

        // Fill the mask with the points within the minimum rectangle
        WritableRaster resultRaster = maskResult.getRaster();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                resultRaster.setSample(x, y, 0, 1);
            }
        }
    }

    @Override
    public String getPointFilterName() {
        return "Minimum Rectangle";
    }

    @Override
    public String getPointFilterTitle() {
        return "Minimum Rectangle";
    }

}
