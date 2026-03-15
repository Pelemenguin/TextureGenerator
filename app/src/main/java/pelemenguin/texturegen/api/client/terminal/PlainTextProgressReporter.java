package pelemenguin.texturegen.api.client.terminal;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import pelemenguin.texturegen.api.client.ProgressReporter;

/**
 * A simple implementation of {@link ProgressReporter} that outputs progress as plain text.
 * This is useful for environments where terminal control characters are not supported, such as log files or certain IDE consoles.
 * 
 * <p>
 * Categories will <b>NOT</b> be displayed by this reporter, as it is designed for simplicity and compatibility rather than visual appeal.
 * Instead, it will simply output the progress for the total and ignore category-specific updates.
 * 
 * <p>
 * A sequence of dots will be printed to represent progress.
 * 
 * <p>
 * Similar to the download progress of Gradle wrapper :)
 */
public class PlainTextProgressReporter extends ProgressReporter {

    private ConcurrentHashMap<String, AtomicInteger> data = new ConcurrentHashMap<>();

    private AtomicInteger printedDots = new AtomicInteger();
    private AtomicInteger total = new AtomicInteger();

    @Override
    public void registerCategory(String category) {
        this.data.put(category, new AtomicInteger());
    }

    @Override
    public void registerCategory(String category, UnaryOperator<String> formatter) {
        this.registerCategory(category);
        // formatter is ignored as we do not display categories in this reporter
    }

    @Override
    public void update(String category, int newValue) {
        // Update for each category is still needed
        // As we hava `getData` method to get the progress of each category, we need to update the data for each category
        if (this.data.get(category) == null) {
            this.data.put(category, new AtomicInteger(newValue));
        } else {
            this.data.get(category).set(newValue);
        }
    }

    @Override
    public void updateTotal(int newValue) {
        this.total.set(newValue);
    }

    @Override
    public void increase(String category, int delta) {
        if (this.data.get(category) == null) {
            this.data.put(category, new AtomicInteger(delta));
        } else {
            this.data.get(category).addAndGet(delta);
        }
    }

    @Override
    public int getData(String category) {
        if (this.data.get(category) == null) {
            return 0;
        } else {
            return this.data.get(category).get();
        }
    }

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    @Override
    public void loop() {
        this.loop(System.out);
    }

    public void loop(PrintStream out) {
        this.service.scheduleAtFixedRate(() -> {
            int currentTotal = this.total.get();
            int progress = 0;
            for (String category : this.data.keySet()) {
                progress += this.data.get(category).get();
            }
            int dotsToPrint = (int) ((double) progress / currentTotal * 100);
            int dotsAlreadyPrinted = this.printedDots.get();
            for (int i = dotsAlreadyPrinted; i < dotsToPrint; i++) {
                if (i % 10 == 0) {
                    // Print percentage
                    out.print(i + "%");
                }
                out.print(".");
            }
            dotsAlreadyPrinted = this.printedDots.addAndGet(dotsToPrint - dotsAlreadyPrinted);
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        this.service.shutdown();
        try {
            this.service.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
    }
    
}
