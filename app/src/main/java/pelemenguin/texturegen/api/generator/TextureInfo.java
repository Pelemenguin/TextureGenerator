package pelemenguin.texturegen.api.generator;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.annotations.SerializedName;

public class TextureInfo {

    Path path;
    @SerializedName("allow_animated")
    boolean allowAnimated = false;
    @SerializedName("skipVariants")
    boolean skipVariants = false;
    Set<String> types = new HashSet<>();

    public Path getPath() {
        return this.path;
    }

    public boolean allowAnimated() {
        return this.allowAnimated;
    }

    public boolean skipVariants() {
        return this.skipVariants;
    }

    public boolean supportTypes(Collection<String> types) {
        for (String type : types) {
            if (this.types.contains(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextureInfo[path=" + path + ", allowAnimated=" + allowAnimated + ", skipVariants=" + skipVariants
                + ", types=" + types + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + (allowAnimated ? 1231 : 1237);
        result = prime * result + (skipVariants ? 1231 : 1237);
        result = prime * result + ((types == null) ? 0 : types.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextureInfo other = (TextureInfo) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (allowAnimated != other.allowAnimated)
            return false;
        if (skipVariants != other.skipVariants)
            return false;
        if (types == null) {
            if (other.types != null)
                return false;
        } else if (!types.equals(other.types))
            return false;
        return true;
    }

    public File tryFallbacks(Path assetsRoot, Iterable<String> fallbacks) {
        // Try fallbacks as suffixes
        // For example, if last is "example.png" and fallbacks are ["wood", "metal"]
        // Then return "example_wood.png" if exists, or return "example_metal.png" if exists
        // If no matched found, return the original path
        // TODO: Write this into Javadoc
        Iterator<String> fallbackIterator = fallbacks.iterator();
        if (!fallbackIterator.hasNext()) return assetsRoot.resolve(this.path).toFile();
        String last = path.getFileName().toString();
        while (fallbackIterator.hasNext()) {
            String fallback = fallbackIterator.next();
            String fallbackFileName = last.replaceFirst("(\\.\\w+)$", "_" + fallback + "$1");
            File fallbackFile = assetsRoot.resolve(path.getParent()).resolve(fallbackFileName).toFile();
            if (fallbackFile.exists()) {
                return fallbackFile;
            }
        }
        return assetsRoot.resolve(this.path).toFile();
    }

}
