package pelemenguin.texturegen.api.builtin;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.client.terminal.ANSIHelper;
import pelemenguin.texturegen.api.client.terminal.StringInput;
import pelemenguin.texturegen.api.client.terminal.TerminalMenu;
import pelemenguin.texturegen.api.client.terminal.TerminalMenuContext;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.util.CommonRegistry;
import pelemenguin.texturegen.api.util.JsonRegistry;
import pelemenguin.texturegen.api.util.PointFilter;

public class RandomPointFilter implements PointFilter {

    // Used for a initial random seed
    private static Random initialSeedRandom = new Random();

    @SerializedName("pass_rate")
    public double passRate = 0.5;
    public long seed = initialSeedRandom.nextLong();

    @Override
    public void register(JsonRegistry<PointFilter> registry) {
        registry.register("texturegen.random", RandomPointFilter.class);
    }

    @Override
    public void filter(BufferedImage image, BufferedImage maskResult) {
        WritableRaster raster = maskResult.getRaster();

        Random randomSource = new Random(getRandomSeed(image, seed));
        for (int x = 0; x < maskResult.getWidth(); x++) {
            for (int y = 0; y < maskResult.getHeight(); y++) {
                if (randomSource.nextDouble() < passRate) {
                    raster.setSample(x, y, 0, 1);
                }
            }
        }
    }

    public static long getRandomSeed(BufferedImage image, long userSelectedSeed) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        long seedFromImage = (long) Arrays.hashCode(pixels) & 0xFFFFFFFFL;
        return userSelectedSeed * 31L + seedFromImage;
    }

    @Override
    public String getPointFilterName() {
        return "Random Filter";
    }

    @Override
    public String getPointFilterTitle() {
        return "Random Filter (" + Math.round(passRate * 100) + "%)";
    }

    public static class TerminalEditor implements TerminalPointFilterEditorProvider, TerminalPointFilterEditorProvider.Editor<RandomPointFilter> {

        @Override
        public void register(CommonRegistry<TerminalPointFilterEditorProvider> registry) {
            registry.register("texturegen.random", this);
        }

        @Override
        public void editorLoop(RandomPointFilter processor, Consumer<RandomPointFilter> setter,
                TerminalMenuContext context) {
            TerminalMenu menu = new TerminalMenu("Random Filter").autoUppercase();
            menu.addKey('-', "Back");
            menu.addKey('R', () -> {
                new StringInput("Enter pass rate (0 ~ 100) (Leave empty to cancel. Original: " + ANSIHelper.blue(processor.passRate * 100 + "%") + "):")
                    .allowEmpty()
                    .scanAndRun(context, (result, error) -> {
                        if (result.isBlank()) return;
                        try {
                            double r = Double.parseDouble(result) * 0.01;
                            if (r < 0.0 || r > 1.0) {
                                error.accept(ANSIHelper.red("Pass rate must be between 0 and 100."));
                            } else {
                                processor.passRate = r;
                            }
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid number format. Please enter a valid decimal number."));
                        }
                    });
            });
            menu.addKey('S', () -> {
                new StringInput("Enter seed (Leave empty to cancecl. Original: " + ANSIHelper.blue(String.valueOf(processor.seed)) + "):")
                    .allowEmpty()
                    .scanAndRun(context, (result, error) -> {
                        if (result.isBlank()) return;
                        try {
                            long s = Long.parseLong(result);
                            processor.seed = s;
                        } catch (NumberFormatException e) {
                            error.accept(ANSIHelper.red("Invalid number format. Please enter a valid integer."));
                        }
                    });
            });
            while (true) {
                menu.updateKeyDescription('R', "Pass Rate: " + ANSIHelper.blue((processor.passRate * 100) + "%"));
                menu.updateKeyDescription('S', "Seed: " + ANSIHelper.blue(String.valueOf(processor.seed)));
                if (menu.scan(context) == '-') {
                    break;
                }
            }
            setter.accept(processor);
        }

        @Override
        public Editor<? extends PointFilter> getEditor() {
            return this;
        }

    }

}
