package pelemenguin.texturegen.api.generator;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class GenerationExecutor {

    public static BufferedImage run(BufferedImage image, GenerationContext context, List<Processor> processors) throws IllegalArgumentException, IllegalStateException {
        ArrayDeque<List<?>> stack = new ArrayDeque<>();
        ArrayDeque<Class<?>> types = new ArrayDeque<>();

        // Push input image
        stack.add(List.of(image));
        types.add(BufferedImage.class);

        // Type check
        for (Processor processor : processors) {
            List<Class<?>> inputTypes = processor.getInputTypes();
            List<Class<?>> outputTypes = processor.getOutputTypes();

            if (stack.size() < inputTypes.size()) {
                throw new IllegalArgumentException("Not enough parameters for processor " + processor
                    + "\nCurrent stack: " + types + "\nRequired types" + inputTypes);
            }

            for (int i = 0; i < inputTypes.size(); i++) {
                Class<?> expectedType = inputTypes.get(i);
                Class<?> actualType = types.pollFirst();

                if (!expectedType.isAssignableFrom(actualType)) {
                    throw new IllegalArgumentException("Expected type " + expectedType.getName() + " but got " + actualType.getName() + " for processor " + processor.getClass().getName());
                }
            }

            // Push output types
            for (Class<?> outputType : outputTypes) {
                stack.add(new ArrayList<>());
                types.add(outputType);
            }
        }

        // Iterate processors and process
        for (Processor processor : processors) {
            List<Class<?>> inputTypes = processor.getInputTypes();
            List<Class<?>> outputTypes = processor.getOutputTypes();

            // Run with parameters iter through each Cartesian product of argument lists
            Parameter parameter = new Parameter(new Object[inputTypes.size()]);
            List<?>[] inputLists = new List[inputTypes.size()];
            for (int i = inputLists.length - 1; i > 0; i--) {
                inputLists[i] = stack.pop();
            }
            // Implement loop equivalent to Cartesian product inline
            int[] indices = new int[inputLists.length];
            for (List<?> list : inputLists) {
                if (list.isEmpty()) {
                    continue;
                }
            }
            Result result = new Result(outputTypes.toArray(new Class[0]));
            while (true) {
                processor.process(context, parameter, result);
                int pos = indices.length - 1;
                while (pos >= 0) {
                    indices[pos] ++;
                    if (indices[pos] < inputLists[pos].size()) {
                        parameter.updateData(pos, inputLists[pos].get(indices[pos]));
                        break;
                    } else {
                        indices[pos] = 0;
                        parameter.updateData(pos, inputLists[pos].get(0));
                        pos --;
                    }
                }
                if (pos < 0) {
                    break;
                }
            }

            // Push results to stack
            for (int i = outputTypes.size() - 1; i >= 0; i--) {
                stack.add(result.data[i]);
                types.add(result.types[i]);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Expected exactly one output but got " + stack.size());
        }
        if (!BufferedImage.class.isAssignableFrom(types.peek())) {
            throw new IllegalStateException("Expected output type " + BufferedImage.class.getName() + " but got " + types.peek().getName());
        }
        List<?> finalResult = stack.pop();
        if (finalResult.size() != 1) {
            throw new IllegalStateException("Expected exactly one output but got " + finalResult.size());
        }
        return (BufferedImage) finalResult.get(0);
    }

    public static record Parameter(Object[] data) {
        public <T> T load(int index, Class<T> type) throws IllegalArgumentException {
            try {
                return type.cast(data[index]);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Parameter at index " + index + " is not of type " + type.getName());
            }
        }
        private void updateData(int index, Object data) {
            this.data[index] = data;
        }
    }

    public static record Result(Class<?>[] types, List<Object>[] data) {
        public Result(Class<?>[] types) {
            this(types, new List[types.length]);
            for (int i = 0; i < types.length; i++) {
                data[i] = new ArrayList<>();
            }
        }
        public <T> void push(int index, T object) throws IllegalArgumentException {
            if (object == null) return;
            try {
                if (object == data[data.length - 1]) return;
                data[index].add(types[index].cast(object));
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Result at index " + index + " is not of type " + types[index].getName());
            }
        }
    }

}
