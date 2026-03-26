package pelemenguin.texturegen.api.generator;

import java.nio.file.Path;

public record GenerationContext(
    Path rawPath,
    Path suffixedPath,
    String fallbackUsed,
    Path assetsRootFolder,
    Path outputsRootFolder
) {
}
