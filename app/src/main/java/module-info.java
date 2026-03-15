module pelemenguin.texturegen {
    requires transitive java.desktop;
    requires transitive com.google.gson;
    requires java.datatransfer;

    exports pelemenguin.texturegen.api.builtin;
    exports pelemenguin.texturegen.api.client.terminal;
    exports pelemenguin.texturegen.api.generator;
    exports pelemenguin.texturegen.api.util;
    exports pelemenguin.texturegen.util;

    provides pelemenguin.texturegen.api.generator.Processor with
        pelemenguin.texturegen.api.builtin.EveryPointProvider,
        pelemenguin.texturegen.api.builtin.ImageRecolorer;

    uses pelemenguin.texturegen.api.generator.Processor;
}
