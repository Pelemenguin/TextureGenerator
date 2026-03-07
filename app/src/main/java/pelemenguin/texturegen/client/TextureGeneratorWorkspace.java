package pelemenguin.texturegen.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.util.PathTypeAdapter;

public class TextureGeneratorWorkspace {

    private static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
        .setPrettyPrinting()
        .create();

    @SerializedName("in_path")
    public Path inPath;
    @SerializedName("out_path")
    public Path outPath;

    public static TextureGeneratorWorkspace openFromFile(File file) throws FileNotFoundException, IOException, JsonSyntaxException, JsonIOException {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, TextureGeneratorWorkspace.class);
        }
    }

    public void saveToFile(File file) throws IOException, JsonIOException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        }
    }

}
