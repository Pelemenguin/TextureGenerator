package pelemenguin.texturegen.util;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ResourceLocationToPathTypeAdapter extends TypeAdapter<Path> {

    private Path root;
    private Path emitted;

    public ResourceLocationToPathTypeAdapter(Path root, Path emitted) {
        this.root = root;
        this.emitted = emitted;
    }

    @Override
    public Path read(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.STRING) {
            return null;
        }
        String[] s = in.nextString().split(":");
        String namespace;
        String path;
        if (s.length == 0) return null;
        else if (s.length == 1) {
            namespace = "minecraft";
            path = s[0];
        } else {
            namespace = s[0];
            path = s[1];
        }
        return root.resolve(namespace).resolve(emitted).resolve(path);
    }

    @Override
    public void write(JsonWriter out, Path value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        Path rel = root.relativize(value);
        Path namespace = rel.getName(0);
        Path path = namespace.relativize(rel);
        if (this.emitted != null) {
            path = this.emitted.relativize(path);
        }
        out.value(namespace.toString() + ":" + path.toString());
    }

}

