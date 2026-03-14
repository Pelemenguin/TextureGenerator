module pelemenguin.texturegen {
    requires transitive java.desktop;
    requires transitive com.google.gson;

    exports pelemenguin.texturegen.api.generator;
    exports pelemenguin.texturegen.api.builtin;

    provides pelemenguin.texturegen.api.generator.Processor with
        pelemenguin.texturegen.api.builtin.EveryPointProvider,
        pelemenguin.texturegen.api.builtin.ImageRecolorer;

    uses pelemenguin.texturegen.api.generator.Processor;
}
