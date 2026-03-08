module pelemenguin.texturegen {
    requires java.base;
    requires com.google.gson;
    requires java.desktop;

    opens pelemenguin.texturegen.api.generator;
    opens pelemenguin.texturegen.api.builtin;
}
