package pelemenguin.texturegen.api.client;

import java.util.function.UnaryOperator;

public abstract class AbstractProgressReporter {
    
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

}
