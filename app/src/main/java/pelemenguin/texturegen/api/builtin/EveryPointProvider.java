package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationContext;
import pelemenguin.texturegen.api.generator.ProcessorInfo;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Parameter;
import pelemenguin.texturegen.api.generator.GenerationExecutor.Result;

@ProcessorInfo(
    "texturegen.every_point_provider"
)
public class EveryPointProvider implements PointProvider {

    @Override
    public void process(GenerationContext context, Parameter parameters, Result result) {
        BufferedImage image = parameters.load(0, BufferedImage.class);
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.providePoint(result, x, y);
            }
        }
    }

    @Override
    public List<Class<?>> getInputTypes() {
        return List.of(BufferedImage.class);
    }
    
}
