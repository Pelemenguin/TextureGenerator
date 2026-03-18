import pelemenguin.texturegen.api.builtin.AlwaysPassPointFilter;
import pelemenguin.texturegen.api.builtin.ImageRecolorer;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.spi.PointFilter;

module pelemenguin.texturegen {
    requires transitive java.desktop;
    requires transitive com.google.gson;
    requires java.datatransfer;

    exports pelemenguin.texturegen.api.builtin;
    exports pelemenguin.texturegen.api.client;
    exports pelemenguin.texturegen.api.client.terminal;
    exports pelemenguin.texturegen.api.generator;
    exports pelemenguin.texturegen.api.util;
    exports pelemenguin.texturegen.spi;
    exports pelemenguin.texturegen.util;

    provides Processor with
        ImageRecolorer;

    provides PointFilter with
        AlwaysPassPointFilter;

    uses Processor;
}
