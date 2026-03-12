package pelemenguin.texturegen.client.terminal;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import pelemenguin.texturegen.client.AbstractProgressReporter;

public class TerminalProgressReporter extends AbstractProgressReporter {

    private HashMap<String, UnaryOperator<String>> formatters = new HashMap<>();
    private HashMap<String, Character> characters = new HashMap<>(); // This should only be modified before loop

    private ConcurrentHashMap<String, Integer> data = new ConcurrentHashMap<>();

    private int maxCategoryLength = 0;
    private volatile int total = 0;

    @Override
    public void registerCategory(String category) {
        this.registerCategory(category, s -> s, '#');
    }

    @Override
    public void registerCategory(String category, UnaryOperator<String> formatter) {
        this.registerCategory(category, formatter, '#');
    }

    public void registerCategory(String category, char character) {
        this.registerCategory(category, s -> s, character);
    }

    public void registerCategory(String category, UnaryOperator<String> formatter, char character) {
        this.characters.put(category, character);
        this.formatters.put(category, formatter);

        if (category.length() > maxCategoryLength) {
            maxCategoryLength = category.length();
        }
    }

    @Override
    public void update(String category, int newValue) {
        this.data.put(category, newValue);
    }

    @Override
    public void updateTotal(int newValue) {
        this.total = newValue;
    }

    private ScheduledExecutorService executor;
    @Override
    public void loop() {
        this.loop(System.out);
    }

    public void loop(PrintStream out) {
        if (this.executor != null) {
            throw new IllegalStateException("Progress reporter loop already started");
        }

        this.executor = Executors.newSingleThreadScheduledExecutor();

        this.executor.scheduleAtFixedRate(() -> {
            ANSIHelper.moveTo(0, 0, out);
            int completedValue = 0;
            for (String category : this.characters.keySet()) {
                int value = this.data.getOrDefault(category, 0);
                completedValue += value;
                String formattedValue = this.formatters.get(category).apply(String.valueOf(value));
                out.println(String.format("%-" + maxCategoryLength + "s | %s", category, formattedValue));
            }
            out.println("Total: " + total);

            // Progress bar
            int total = this.total;
            if (total > 0) {
                StringBuilder content = new StringBuilder();

                int width = 50;
                int taken = 0;
                for (String category : this.characters.keySet()) {
                    int lengthForThisCategory = (int) Math.round((this.data.getOrDefault(category, 0) / (double) total) * width);
                    taken += lengthForThisCategory;
                    String repr = String.valueOf(this.characters.get(category)).repeat(lengthForThisCategory);
                    content.append(this.formatters.get(category).apply(repr));
                }
                content.append(" ".repeat(Math.max(0, width - taken)));

                out.println("[" + content + "] " + String.format("%.2f", (completedValue / (double) total) * 100) + "%");
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if (this.executor == null) {
            throw new IllegalStateException("Progress reporter loop not started");
        }
        this.executor.shutdown();
        ANSIHelper.moveTo(0, 0, System.out);
        ANSIHelper.clear(System.out);
        this.executor = null;
    }

}
