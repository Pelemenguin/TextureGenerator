package pelemenguin.texturegen.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.api.generator.TConstructPartListReader;
import pelemenguin.texturegen.api.generator.TextureInfo;
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
    @SerializedName("generators_path")
    public Path generatorsPath = Path.of("./materials");
    public ArrayList<TextureInfo> textures = new ArrayList<>();

    public List<TextureInfo> getTextures() {
        return List.copyOf(textures);
    }

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

    public void readTextureInfos() throws IllegalStateException, FileNotFoundException, IOException {
        if (inPath == null) {
            throw new IllegalStateException("Input path must be set before reading texture infos");
        }
        File inFolder = inPath.toFile();
        if (!inFolder.exists() || !inFolder.isDirectory()) {
            throw new IllegalStateException("Input path must be an existing directory");
        }
        textures.clear();
        TConstructPartListReader.readFromAssets(inFolder).forEach(textures::add);
    }

}
