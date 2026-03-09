module pelemenguin.texturegen {
    requires java.base;
    requires java.desktop;
    requires com.google.gson;

    opens pelemenguin.texturegen.api.generator;
    opens pelemenguin.texturegen.api.builtin;
}
