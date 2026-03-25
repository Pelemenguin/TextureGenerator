package pelemenguin.texturegen.api.util;

import java.awt.image.BufferedImage;

import pelemenguin.texturegen.api.generator.GenerationContext;

public interface NoiseGenerator {

    public void generateNoise(GenerationContext context, BufferedImage image, BufferedImage noiseResult);

    public default BufferedImage generateNoise(GenerationContext context, BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        this.generateNoise(context, image, result);
        return result;
    }

}
