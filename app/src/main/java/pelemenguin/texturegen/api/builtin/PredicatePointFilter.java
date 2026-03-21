package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import pelemenguin.texturegen.api.util.PointFilter;

public interface PredicatePointFilter extends PointFilter {

    public boolean testPoint(BufferedImage image, int x, int y);

    @Override
    default void filter(BufferedImage image, BufferedImage maskResult) {
        WritableRaster raster = maskResult.getRaster();
        for (int x = 0; x < maskResult.getWidth(); x++) {
            for (int y = 0; y < maskResult.getHeight(); y++) {
                int value = testPoint(image, x, y) ? 1 : 0;
                raster.setSample(x, y, 0, value);
            }
        }
    }

}
