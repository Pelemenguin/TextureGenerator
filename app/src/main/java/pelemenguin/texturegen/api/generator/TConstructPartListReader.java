package pelemenguin.texturegen.api.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.util.ResourceLocationToPathTypeAdapter;

public class TConstructPartListReader {

    public static Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Path.class, new ResourceLocationToPathTypeAdapter(Path.of("assets"), Path.of("textures")))
        .setPrettyPrinting()
        .create();

    private TConstructPartInfo[] parts = new TConstructPartInfo[0];
    @SuppressWarnings("unused")
    private boolean replace = false; // Never replace as no body would do that I think

    private static class TConstructPartInfo {
        private Path path;
        @SerializedName("stat_type")
        private String statType;
        @SerializedName("allow_animated")
        private boolean allowAnimated;
        @SerializedName("skip_variants")
        private boolean skipVariants;
    }

    public List<TextureInfo> toTextureInfos() {
        // Transform TConstruct part infos into ourselves' texture infos
        return List.of(this.parts).stream().map(part -> {
            TextureInfo textureInfo = new TextureInfo();
            Path filePath = part.path.resolveSibling(part.path.getFileName().toString() + ".png");
            textureInfo.path = filePath;
            textureInfo.allowAnimated = part.allowAnimated;
            textureInfo.skipVariants = part.skipVariants;
            textureInfo.types.add(part.statType);
            return textureInfo;
        }).toList();
    }

    public static List<TextureInfo> readFromFile(File file) throws FileNotFoundException, IOException, JsonSyntaxException, JsonIOException {
        try (var reader = new FileReader(file)) {
            return GSON.fromJson(reader, TConstructPartListReader.class).toTextureInfos();
        }
    }

    public static List<TextureInfo> readFromAssets(File assetsFolder) throws SecurityException, FileNotFoundException, IOException, JsonSyntaxException, JsonIOException {
        if (!assetsFolder.exists()) {
            throw new FileNotFoundException("Assets folder not found: " + assetsFolder.getAbsolutePath());
        }
        List<TextureInfo> textureInfos = new ArrayList<>();
        File assetsRoot = assetsFolder.toPath().resolve("assets").toFile();
        File[] files = assetsRoot.listFiles(File::isDirectory);
        if (files == null) {
            return textureInfos;
        }
        for (File namespaceFolder : files) {
            File partListFile = namespaceFolder.toPath().resolve("tinkering/generator_part_textures.json").toFile();
            if (partListFile.exists()) {
                textureInfos.addAll(readFromFile(partListFile));
            }
        }
        return textureInfos;
    }

}

