package imgui;

import imgui.flag.ImGuiWindowFlags;

/**
 * CPU-only font-stack regression. Requires the matching regenerated binding and rebuilt JNI library.
 * Uses Dear ImGui's embedded vector font; creates no OS window, graphics context, renderer, or external asset.
 */
public final class FontApiRegression {
    private static final String SAMPLE = "MiWi 0123";
    private static final float BASE_SIZE = 16.0f;
    private static final float HEADING_SIZE = 20.25f;
    private static int checks;

    private FontApiRegression() {
    }

    public static void main(final String[] args) {
        run(false);
        run(true);
        System.out.println("FontApiRegression PASS: " + checks + " checks (stb and FreeType, CPU only)");
    }

    private static void run(final boolean freeType) {
        ImGui.createContext();
        try {
            final ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null);
            io.setLogFilename(null);
            io.setDisplaySize(1024.0f, 768.0f);
            io.setDeltaTime(1.0f / 60.0f);
            io.setConfigDpiScaleFonts(false);
            final ImFontAtlas atlas = io.getFonts();
            atlas.setFreeTypeRenderer(freeType);
            final ImFont regular = addEmbeddedFont(atlas, 0.0f);
            final ImFont wide = addEmbeddedFont(atlas, 4.0f);
            io.setFontDefault(regular);
            if (!atlas.build()) {
                throw new AssertionError("Embedded font atlas build failed: FreeType=" + freeType);
            }

            final float[][] scales = {{1.0f, 1.0f}, {1.25f, 1.5f}, {1.5f, 1.75f}};
            for (final float[] scale : scales) {
                final ImGuiStyle style = ImGui.getStyle();
                style.setFontScaleMain(scale[0]);
                style.setFontScaleDpi(scale[1]);
                ImGui.newFrame();
                try {
                    ImGui.setNextWindowPos(0.0f, 0.0f);
                    ImGui.setNextWindowSize(1024.0f, 768.0f);
                    ImGui.begin("Font API CPU regression", ImGuiWindowFlags.NoDecoration
                            | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoInputs);
                    try {
                        exerciseStack(regular, wide, scale[0], scale[1]);
                    } finally {
                        ImGui.end();
                    }
                } finally {
                    ImGui.endFrame();
                }
            }
        } finally {
            ImGui.destroyContext();
        }
    }

    private static ImFont addEmbeddedFont(final ImFontAtlas atlas, final float extraAdvance) {
        final ImFontConfig config = new ImFontConfig();
        try {
            config.setSizePixels(BASE_SIZE);
            config.setGlyphExtraAdvanceX(extraAdvance);
            final ImFont font = atlas.addFontDefaultVector(config);
            if (font.isNotValidPtr()) {
                throw new AssertionError("Embedded font registration failed");
            }
            return font;
        } finally {
            config.destroy();
        }
    }

    private static void exerciseStack(final ImFont regular, final ImFont wide, final float userScale, final float dpiScale) {
        int depth = 0;
        try {
            // This is deliberately the first changed API exercised: pre-fix Java dereferences null here.
            ImGui.pushFont(null, HEADING_SIZE);
            depth++;
            final float regularWidth = checkMetrics("null changes size", regular, HEADING_SIZE, userScale, dpiScale);

            ImGui.pushFont(null, 0.0f);
            depth++;
            checkMetrics("null/zero retains font and size", regular, HEADING_SIZE, userScale, dpiScale);

            ImGui.pushFont(wide, 0.0f);
            depth++;
            final float wideWidth = checkMetrics("font-only keeps base size", wide, HEADING_SIZE, userScale, dpiScale);
            if (!(wideWidth > regularWidth)) {
                throw new AssertionError("Font selection must change measured glyph advances");
            }
            checks++;

            final float enlargedBase = ImGui.getStyle().getFontSizeBase() * 1.5f;
            ImGui.pushFont(null, enlargedBase);
            depth++;
            checkMetrics("null retains non-default font", wide, HEADING_SIZE * 1.5f, userScale, dpiScale);

            ImGui.pushFont(regular, BASE_SIZE);
            depth++;
            checkMetrics("explicit font and size", regular, BASE_SIZE, userScale, dpiScale);

            ImGui.popFont();
            depth--;
            checkMetrics("restore non-default font and size", wide, HEADING_SIZE * 1.5f, userScale, dpiScale);
            ImGui.popFont();
            depth--;
            checkMetrics("restore non-default heading", wide, HEADING_SIZE, userScale, dpiScale);
            ImGui.popFont();
            depth--;
            checkMetrics("restore original font", regular, HEADING_SIZE, userScale, dpiScale);
            ImGui.popFont();
            depth--;
            checkMetrics("restore through null/zero", regular, HEADING_SIZE, userScale, dpiScale);
            ImGui.popFont();
            depth--;
            checkMetrics("restore frame base size", regular, BASE_SIZE, userScale, dpiScale);
        } finally {
            while (depth > 0) {
                ImGui.popFont();
                depth--;
            }
        }
    }

    private static float checkMetrics(final String label, final ImFont font, final float base,
                                     final float userScale, final float dpiScale) {
        if (ImGui.getFont().ptr != font.ptr) {
            throw new AssertionError(label + ": selected native font changed");
        }
        checks++;
        final ImGuiStyle style = ImGui.getStyle();
        near(label + " base", base, style.getFontSizeBase());
        // Pinned 1.92.7 rounds the final size, but retains the fractional unscaled base.
        final float scaledSize = (float) Math.floor(base * userScale * dpiScale + 0.5f);
        near(label + " scaled size", scaledSize, ImGui.getFontSize());
        near(label + " line height", scaledSize, ImGui.getTextLineHeight());
        final ImVec2 padding = style.getFramePadding();
        near(label + " frame height", scaledSize + padding.y * 2.0f, ImGui.getFrameHeight());

        // Independently measure the expected font, not the current-font wrapper (which is reused by getFont()).
        final ImVec2 expectedText = font.calcTextSizeA(scaledSize, Float.MAX_VALUE, 0.0f, SAMPLE);
        final float expectedWidth = (float) Math.floor(expectedText.x + 0.99999f);
        final ImVec2 text = ImGui.calcTextSize(SAMPLE);
        near(label + " text width", expectedWidth, text.x);
        near(label + " text height", scaledSize, text.y);

        // Submit an actual widget, without a renderer. Unique IDs avoid duplicate-ID diagnostics.
        ImGui.pushID(label);
        try {
            ImGui.setCursorPos(32.0f, 32.0f);
            ImGui.button(SAMPLE);
            final ImVec2 widget = ImGui.getItemRectSize();
            near(label + " button width", expectedWidth + padding.x * 2.0f, widget.x);
            near(label + " button height", scaledSize + padding.y * 2.0f, widget.y);
        } finally {
            ImGui.popID();
        }
        return text.x;
    }

    private static void near(final String label, final float expected, final float actual) {
        if (!(Math.abs(expected - actual) <= 0.001f)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        checks++;
    }
}
