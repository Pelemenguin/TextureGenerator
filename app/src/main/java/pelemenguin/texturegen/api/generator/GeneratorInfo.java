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

public class GeneratorInfo {

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public String suffix;
    public String[] fallbacks = new String[0];

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

