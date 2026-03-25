package pelemenguin.texturegen.api.util;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class ImageUtils {

    /**
     * Returns a hash code based on the content of the {@link BufferedImage}.
     * 
     * <p>
     * The hash code is calculated from:
     * <pre><code>
     *    int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
     *    return Arrays.hashCode(pixels);
     * </code></pre>
     * 
     * @param image The input image
     * @return      A hash code for the image.
     */
    public static int hashCode(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return Arrays.hashCode(pixels);
    }

    private ImageUtils() {}

}
