package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.JsonRegistry;

/**
 * Clones the input image so that later processors can modify it without affecting the original image.
 * This is useful when you want to use the same image as input for multiple processors, but you don't want them to interfere with each other.
 */
public class ImageCloner implements Processor {

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage original = parameters.load(0, BufferedImage.class);

        BufferedImage cloned = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        cloned.setData(original.getData());

        result.push(0, original);
        result.push(1, cloned);
    }

    @Override
    public void register(JsonRegistry<Processor> registry) {
        registry.register("texturegen.image.cloner", ImageCloner.class);
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class);
    }

    @Override
    public List<Class<?>> getOutputTypes() {
        return List.of(BufferedImage.class, BufferedImage.class);
    }

    @Override
    public String getProcessorName() {
        return "Image Cloner";
    }

    @Override
    public String getProcessorTitle() {
        return "Image Cloner";
    }

}
