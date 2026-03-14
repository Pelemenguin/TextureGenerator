package pelemenguin.texturegen.api.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import pelemenguin.texturegen.api.builtin.ImageRecolorer;

public class GeneratorInfo {

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public String suffix;
    // TODO: Remove this test
    // public String[] fallbacks = new String[0];
    // public Processor[] processors = new Processor[0];
    public String[] fallbacks = new String[] {
        "metal"
    };
    public transient Processor[] processors = new Processor[] {
        ImageRecolorer.builder()
            // Use colors below as test
            .putColor(0, 0xFF000000)
            .putColor(63, 0xFF2D5646)
            .putColor(102, 0xFF396E59)
            .putColor(140, 0xFF43897A)
            .putColor(178, 0xFF48966D)
            .putColor(216, 0xFF4FAB90)
            .putColor(255, 0xFF73CEA6)
            .build()
    };

    public static GeneratorInfo openFromFile(File file) throws FileNotFoundException, IOException, JsonIOException, JsonSyntaxException {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, GeneratorInfo.class);
        }
    }

    public void saveToFile(File file) throws IOException, JsonIOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        }
    }

}

