import pelemenguin.texturegen.api.builtin.ImageRecolorer;
import pelemenguin.texturegen.api.builtin.ImageSplitter;
import pelemenguin.texturegen.api.builtin.MinRectPointFilter;
import pelemenguin.texturegen.api.builtin.NoiseRenderer;
import pelemenguin.texturegen.api.builtin.PerlinNoiseProvider;
import pelemenguin.texturegen.api.builtin.FillColorProcessor;
import pelemenguin.texturegen.api.builtin.HSVPointFilter;
import pelemenguin.texturegen.api.builtin.ImageCloner;
import pelemenguin.texturegen.api.builtin.ImageMerger;
import pelemenguin.texturegen.api.builtin.RGBAPointFilter;
import pelemenguin.texturegen.api.builtin.RandomPointFilter;
import pelemenguin.texturegen.api.builtin.StackDuplicator;
import pelemenguin.texturegen.api.builtin.StackPopper;
import pelemenguin.texturegen.api.client.terminal.TerminalNoiseProviderEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalPointFilterEditorProvider;
import pelemenguin.texturegen.api.client.terminal.TerminalProcessorEditorProvider;
import pelemenguin.texturegen.api.generator.Processor;
import pelemenguin.texturegen.api.util.NoiseProvider;
import pelemenguin.texturegen.api.util.PointFilter;

module pelemenguin.texturegen {
    requires transitive java.desktop;
    requires transitive com.google.gson;
    requires java.datatransfer;

    exports pelemenguin.texturegen.api.builtin;
    exports pelemenguin.texturegen.api.client;
    exports pelemenguin.texturegen.api.client.terminal;
    exports pelemenguin.texturegen.api.generator;
    exports pelemenguin.texturegen.api.util;
    exports pelemenguin.texturegen.util;

    provides Processor with
        Processor.ErrorProcessor,
        ImageCloner,
        ImageRecolorer,
        ImageSplitter,
        ImageMerger,
        NoiseRenderer,
        FillColorProcessor,
        StackPopper,
        StackDuplicator;

    provides TerminalProcessorEditorProvider with
        ImageRecolorer.Editor,
        ImageSplitter.TerminalEditor,
        ImageMerger.TerminalEditor,
        NoiseRenderer.TerminalEditor,
        FillColorProcessor.TerminalEditor,
        StackPopper.TerminalEditor,
        StackDuplicator.TerminalEditor;

    provides PointFilter with
        PointFilter.ErrorPointFilter,
        PointFilter.AlwaysPass,
        PointFilter.AlwaysFail,
        PointFilter.And,
        PointFilter.Or,
        PointFilter.Not,
        HSVPointFilter.Hue,
        HSVPointFilter.Saturation,
        HSVPointFilter.Value,
        RGBAPointFilter.Red,
        RGBAPointFilter.Green,
        RGBAPointFilter.Blue,
        RGBAPointFilter.Alpha,
        MinRectPointFilter,
        RandomPointFilter;

    provides TerminalPointFilterEditorProvider with
        PointFilter.And.TerminalEditor,
        PointFilter.Or.TerminalEditor,
        PointFilter.Not.TerminalEditor,
        HSVPointFilter.Hue.TerminalEditor,
        HSVPointFilter.Saturation.TerminalEditor,
        HSVPointFilter.Value.TerminalEditor,
        RGBAPointFilter.Red.TerminalEditor,
        RGBAPointFilter.Green.TerminalEditor,
        RGBAPointFilter.Blue.TerminalEditor,
        RGBAPointFilter.Alpha.TerminalEditor,
        RandomPointFilter.TerminalEditor;

    provides NoiseProvider with
        NoiseProvider.Constant,
        NoiseProvider.Add,
        NoiseProvider.Multiply,
        NoiseProvider.Minimum,
        NoiseProvider.Maximum,
        NoiseProvider.Threshold,
        PerlinNoiseProvider;

    provides TerminalNoiseProviderEditorProvider with
        NoiseProvider.Constant.TerminalEditor,
        NoiseProvider.Add.TerminalEditor,
        NoiseProvider.Multiply.TerminalEditor,
        NoiseProvider.Minimum.TerminalEditor,
        NoiseProvider.Maximum.TerminalEditor,
        NoiseProvider.Threshold.TerminalEditor,
        PerlinNoiseProvider.TerminalEditor;

    uses Processor;
}
