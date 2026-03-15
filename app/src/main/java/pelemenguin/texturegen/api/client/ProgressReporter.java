package pelemenguin.texturegen.api.client;

import java.util.HashMap;
import java.util.function.UnaryOperator;

public abstract class ProgressReporter {
    
    public abstract void registerCategory(String category);
    public abstract void registerCategory(String category, UnaryOperator<String> formatter);

    public abstract void update(String category, int newValue);
    public abstract void updateTotal(int newValue);
    public abstract void increase(String category, int delta);
    public void increase(String category) {
        this.increase(category, 1);
    }

    public abstract int getData(String category);

    public abstract void loop();
    public abstract void shutdown();

    public static ProgressReporter newNoOutputReporter() {
        return new NoOutput();
    }

    private static class NoOutput extends ProgressReporter {

        // As we don't even have an output
        // So we don't need to care about concurrency, we can just use HashMap and Integer
        private HashMap<String, Integer> data = new HashMap<>();

        @Override
        public void registerCategory(String category) {
            this.data.put(category, 0);
        }

        @Override
        public void registerCategory(String category, UnaryOperator<String> formatter) {
            this.registerCategory(category);
        }

        @Override
        public void update(String category, int newValue) {
            this.data.put(category, newValue);
        }

        @Override
        public void updateTotal(int newValue) {
            // We don't need to care about total in this reporter, so we can just ignore it
        }

        @Override
        public void increase(String category, int delta) {
            this.data.put(category, this.data.getOrDefault(category, 0) + delta);
        }

        @Override
        public int getData(String category) {
            return this.data.getOrDefault(category, 0);
        }

        @Override
        public void loop() {
            // We don't need to loop in this reporter, so we can just ignore it
        }

        @Override
        public void shutdown() {
            // We don't need to shutdown in this reporter, so we can just ignore it
        }
        
    }

}
