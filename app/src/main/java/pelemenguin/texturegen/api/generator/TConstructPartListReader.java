package pelemenguin.texturegen.api.generator;

import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import pelemenguin.texturegen.util.ResourceLocationToPathTypeAdapter;

public class TConstructPartListReader {

    public static Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(Path.class, new ResourceLocationToPathTypeAdapter(Path.of("assets"), Path.of("textures")))
        .setPrettyPrinting()
        .create();

    private TConstructPartInfo[] parts = new TConstructPartInfo[0];
    private boolean replace = false;

    private static class TConstructPartInfo {
        private Path path;
        @SerializedName("stat_type")
        private String statType;
        @SerializedName("allow_animated")
        private boolean allowAnimated;
        @SerializedName("skip_variants")
        private boolean skipVariants;
    }

}

