package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;

public class ImageRecolorer implements Processor {

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        // TODO: Replace here with input palette
        // Currently map every pixel to red for test
        BufferedImage image = parameters.load(0, BufferedImage.class);
        int width = image.getWidth();
        int height = image.getHeight();
        // Just modify the original image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xFFFF0000);
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

}
