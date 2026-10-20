package imgui;

import imgui.internal.ImGuiContext;
import imgui.type.ImInt;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.CRC32;

/** CPU-only JNI ownership regression; pass a TrueType font containing M, W and g. */
public final class FontMemoryRegression {
    private static byte[] source;
    private static int checks;
    private static int cases;

    private FontMemoryRegression() {
    }

    public static void main(String[] args) throws Exception {
        source = Files.readAllBytes(Paths.get(args[0]));
        for (boolean freeType : new boolean[]{false, true}) {
            // Distinct ownership boundaries: default config, caller config, and bounded prefix/ranges.
            for (int overload : new int[]{0, 1, 6}) {
                Snapshot control = registration(freeType, overload, false);
                Snapshot poisoned = registration(freeType, overload, true);
                same("overload " + overload + ", FreeType=" + freeType, control, poisoned);
                cases++;
            }
            configReuse(freeType);
            standaloneLifetime(freeType);
            sharedLifetime(freeType);
        }
        bounds();
        System.out.println("FontMemoryRegression PASS: " + checks + " checks / " + cases
                + " cases (stb and FreeType; byte/range poisoning, GC, config reuse, atlas/context lifetimes)");
    }

    private static Snapshot registration(boolean freeType, int overload, boolean poison) {
        ImGuiContext context = ImGui.createContext();
        try {
            ImFontAtlas atlas = ImGui.getIO().getFonts();
            atlas.setFreeTypeRenderer(freeType);
            ImFontConfig config = new ImFontConfig();
            byte[] bytes = Arrays.copyOf(source, source.length + (overload >= 4 ? 32 : 0));
            short[] ranges = {32, 127, 0};
            ImFont font;
            try {
                switch (overload) {
                    case 0: font = atlas.addFontFromMemoryTTF(bytes, 16.0f); break;
                    case 1: font = atlas.addFontFromMemoryTTF(bytes, 16.0f, config); break;
                    case 6: font = atlas.addFontFromMemoryTTF(bytes, source.length, 16.0f, config, ranges); break;
                    default: throw new AssertionError(overload);
                }
                if (poison) {
                    Arrays.fill(bytes, (byte) 0);
                    Arrays.fill(ranges, (short) 0);
                }
            } finally {
                config.destroy();
            }
            if (poison) System.gc();
            return snapshot(atlas, font);
        } finally {
            ImGui.destroyContext(context);
        }
    }

    private static void configReuse(boolean freeType) {
        ImGuiContext anchor = ImGui.createContext();
        ImFontAtlas first = new ImFontAtlas();
        ImFontAtlas second = new ImFontAtlas();
        ImFontConfig config = new ImFontConfig();
        try {
            first.setFreeTypeRenderer(freeType);
            second.setFreeTypeRenderer(freeType);
            byte[] bytes = source.clone();
            short[] ranges = {77, 77, 0};
            config.setFontData(bytes);
            config.setSizePixels(16.0f);
            config.setGlyphRanges(ranges);
            ImFont fontA = first.addFont(config);
            Snapshot control = snapshot(first, fontA);
            Arrays.fill(bytes, (byte) 0);
            Arrays.fill(ranges, (short) 0);
            byte[] retrievedBytes = config.getFontData();
            short[] retrievedRanges = config.getGlyphRanges();
            Arrays.fill(retrievedBytes, (byte) 0);
            Arrays.fill(retrievedRanges, (short) 0);
            ImFont fontB = second.addFont(config);
            // Replacement frees the config's old allocations; neither registered source may retain them.
            config.setFontData(source.clone());
            config.setGlyphRanges(null);
            config.destroy();
            System.gc();
            same("reused config, FreeType=" + freeType, control, snapshot(second, fontB));
            same("first source survives config replacement, FreeType=" + freeType, control, snapshot(first, fontA));
            cases += 2;
        } finally {
            config.destroy();
            first.destroy();
            second.destroy();
            ImGui.destroyContext(anchor);
        }
    }

    private static void standaloneLifetime(boolean freeType) {
        ImGuiContext anchor = ImGui.createContext();
        try {
            Snapshot control = null;
            for (int cycle = 0; cycle < 2; cycle++) {
                ImFontAtlas owned = new ImFontAtlas();
                try {
                    owned.setFreeTypeRenderer(freeType);
                    Snapshot initial = snapshot(owned, owned.addFontFromMemoryTTF(source, 16.0f));
                    if (control == null) control = initial;
                    else same("fresh owned atlas cycle " + cycle, control, initial);
                    owned.clear();
                    ImFont rebuilt = owned.addFontFromMemoryTTF(source, 16.0f);
                    same("owned atlas clear/rebuild " + cycle, control, snapshot(owned, rebuilt));
                    ImFontAtlas borrowedAlias = new ImFontAtlas(owned.ptr);
                    rejected("borrowed alias destroy", borrowedAlias::destroy);
                    same("owner survives rejected alias destruction", control,
                            snapshot(owned, rebuilt));
                } finally {
                    owned.destroy();
                    owned.destroy();
                }
                cases++;
            }
        } finally {
            ImGui.destroyContext(anchor);
        }
    }

    private static void sharedLifetime(boolean freeType) {
        ImFontAtlas owned = new ImFontAtlas();
        owned.setFreeTypeRenderer(freeType);
        ImGuiContext first = ImGui.createContext(owned);
        ImFont font = owned.addFontFromMemoryTTF(source, 16.0f);
        Snapshot control = snapshot(owned, font);
        rejected("attached owner destroy", owned::destroy);
        ImGuiContext second = ImGui.createContext(new ImFontAtlas(owned.ptr));
        ImGui.setCurrentContext(second);
        rejected("shared owner destroy", owned::destroy);
        ImGui.destroyContext(first); // Destroy a non-current context; the other still owns the atlas.
        same("remaining shared context", control, snapshot(owned, font));
        ImGuiContext third = ImGui.createContext(owned);
        ImGui.setCurrentContext(third);
        ImGui.destroyContext(second);
        same("shared owner survives another handoff", control, snapshot(owned, font));
        ImGui.destroyContext();
        if (owned.isValidPtr()) throw new AssertionError("Last context left an owning handle to deleted native memory");
        checks++;
        owned.destroy(); // Idempotent: native context teardown already deleted this atlas.
        ImGui.destroyContext(); // No current context remains.
        cases++;
    }

    private static void bounds() {
        ImGuiContext context = ImGui.createContext();
        ImFontConfig config = new ImFontConfig();
        try {
            ImFontAtlas atlas = ImGui.getIO().getFonts();
            rejected("oversized byte prefix", () -> atlas.addFontFromMemoryTTF(source, source.length + 1, 16.0f));
            rejected("negative byte prefix", () -> atlas.addFontFromMemoryTTF(source, -1, 16.0f));
            rejected("empty byte prefix", () -> atlas.addFontFromMemoryTTF(source, 0, 16.0f));
            rejected("missing range terminator", () -> config.setGlyphRanges(new short[]{65, 65}));
            rejected("descending range", () -> config.setGlyphRanges(new short[]{66, 65, 0}));
            short[] manyRanges = new short[67];
            for (int i = 0; i < 33; i++) manyRanges[i * 2] = manyRanges[i * 2 + 1] = (short) (65 + i);
            config.setGlyphRanges(manyRanges); // The 32-pair exclusion cap does not apply to ordinary ranges.
            config.setFontData(source);
            config.setSizePixels(16.0f);
            ImFont font = atlas.addFont(config);
            config.destroy();
            Snapshot actual = snapshot(atlas, font);
            if (!(actual.width16 > 0.0f && actual.width31 > actual.width16)) {
                throw new AssertionError("Font using >32 configured ranges cannot measure text at both sizes");
            }
            checks++;
            cases++;
        } finally {
            config.destroy();
            ImGui.destroyContext(context);
        }
    }

    private static Snapshot snapshot(ImFontAtlas atlas, ImFont font) {
        if (!atlas.build()) throw new AssertionError("Font atlas build failed");
        float width16 = font.calcTextSizeAX(16.0f, Float.MAX_VALUE, 0.0f, "MWg");
        float width31 = font.calcTextSizeAX(31.25f, Float.MAX_VALUE, 0.0f, "MWg\u03a9");
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = atlas.getTexDataAsRGBA32(width, height).duplicate();
        CRC32 crc = new CRC32();
        pixels.position(0);
        while (pixels.hasRemaining()) crc.update(pixels.get() & 255);
        return new Snapshot(width16, width31, width.get(), height.get(), crc.getValue());
    }

    private static void same(String label, Snapshot expected, Snapshot actual) {
        near(label + " text16", expected.width16, actual.width16);
        near(label + " late text31", expected.width31, actual.width31);
        if (expected.width != actual.width || expected.height != actual.height || expected.crc != actual.crc) {
            throw new AssertionError(label + ": atlas pixels changed (" + expected.width + "x" + expected.height
                    + ", CRC " + expected.crc + " -> " + actual.width + "x" + actual.height + ", CRC " + actual.crc + ")");
        }
        checks++;
    }

    private static void near(String label, float expected, float actual) {
        if (!(Math.abs(expected - actual) < 0.0001f)) throw new AssertionError(label + ": " + expected + " -> " + actual);
        checks++;
    }

    private static void rejected(String label, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError(label + ": unsafe operation was accepted");
    }

    private static final class Snapshot {
        final float width16;
        final float width31;
        final int width;
        final int height;
        final long crc;

        Snapshot(float width16, float width31, int width, int height, long crc) {
            this.width16 = width16;
            this.width31 = width31;
            this.width = width;
            this.height = height;
            this.crc = crc;
        }
    }
}
