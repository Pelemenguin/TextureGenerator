package pelemenguin.texturegen.client;

import java.util.function.UnaryOperator;

public abstract class AbstractProgressReporter {
    
    public abstract void registerCategory(String category);
    public abstract void registerCategory(String category, UnaryOperator<String> formatter);

    public abstract void update(String category, int newValue);
    public abstract void updateTotal(int newValue);

    public abstract void loop();
    public abstract void shutdown();

}
