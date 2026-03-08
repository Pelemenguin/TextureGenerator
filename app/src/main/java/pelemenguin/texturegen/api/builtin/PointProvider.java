package pelemenguin.texturegen.api.builtin;

import java.util.List;

import pelemenguin.texturegen.api.generator.GenerationExecutor;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.Point;

public interface PointProvider extends Processor {

    @Override
    default List<Class<?>> getOutputTypes() {
        return List.of(Point.class);
    }

    default void providePoint(GenerationExecutor.Result result, int x, int y) {
        result.push(0, new Point(x, y));
    }

}
