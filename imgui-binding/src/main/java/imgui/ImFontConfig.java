package imgui;

import imgui.binding.ImGuiStructDestroyable;
import imgui.binding.annotation.BindingField;
import imgui.binding.annotation.BindingSource;

@BindingSource
public final class ImFontConfig extends ImGuiStructDestroyable {
    public ImFontConfig() {
        super();
    }

    ImFontConfig(final long ptr) {
        super(ptr);
    }

    @Override
    protected long create() {
        return nCreate();
    }

    /*JNI
        #include "_common.h"
        #include "imgui_internal.h"
        #define THIS ((ImFontConfig*)STRUCT_PTR)
     */

    private native long nCreate(); /*
        ImFontConfig* cfg = new ImFontConfig();
        cfg->FontDataOwnedByAtlas = false;
        return (uintptr_t)cfg;
    */

    /** Releases this configuration; registered atlases retain independent font data and range copies. */
    @Override
    public void destroy() {
        if (ptr == 0) {
            return;
        }
        nDestroyConfig();
        ptr = 0;
        glyphRanges = null;
        glyphExcludeRanges = null;
    }

    private native void nDestroyConfig(); /*
        IM_FREE(THIS->FontData);
        IM_FREE((void*)THIS->GlyphRanges);
        IM_FREE((void*)THIS->GlyphExcludeRanges);
        delete THIS;
    */

    /**
     * TTF/OTF data
     */
    public native byte[] getFontData(); /*
        int size = THIS->FontDataSize;
        jbyteArray jBuf = env->NewByteArray(size);
        env->SetByteArrayRegion(jBuf, 0, size, (jbyte*)THIS->FontData);
        return jBuf;
    */

    /** Copies TTF/OTF data into this configuration and derives its size. Null clears the data. */
    public void setFontData(final byte[] fontData) {
        if (ptr == 0) {
            throw new IllegalStateException("Font configuration has been destroyed");
        }
        nSetFontData(fontData);
    }

    private native void nSetFontData(byte[] obj_fontData); /*MANUAL
        const int size = obj_fontData == NULL ? 0 : env->GetArrayLength(obj_fontData);
        void* copy = size > 0 ? IM_ALLOC(size) : NULL;
        if (size > 0 && copy == NULL) {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Cannot allocate font data");
            return;
        }
        if (size > 0)
            env->GetByteArrayRegion(obj_fontData, 0, size, (jbyte*)copy);
        if (env->ExceptionCheck()) {
            IM_FREE(copy);
            return;
        }
        IM_FREE(THIS->FontData);
        THIS->FontData = copy;
        THIS->FontDataSize = size;
        THIS->FontDataOwnedByAtlas = false;
    */

    /** Size of the configuration's native TTF/OTF copy. */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public int FontDataSize;

    /**
     * Index of font within TTF/OTF file
     */
    @BindingField
    public int FontNo;

    /**
     * Size in pixels for rasterizer (more or less maps to the resulting font height).
     */
    @BindingField
    public float SizePixels;

    /**
     * Rasterize at higher quality for sub-pixel positioning.
     * Note the difference between 2 and 3 is minimal so you can reduce this to 2 to save memory.
     * Read https://github.com/nothings/stb/blob/master/tests/oversample/README.md for details.
     */
    @BindingField
    public int OversampleH;

    /**
     * Rasterize at higher quality for sub-pixel positioning.
     * This is not really useful as we don't use sub-pixel positions on the Y axis.
     */
    @BindingField
    public int OversampleV;

    /**
     * Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default font.
     * If enabled, you can set OversampleH/V to 1.
     */
    @BindingField
    public boolean PixelSnapH;

    /**
     * Extra spacing (in pixels) between glyphs (added to glyph AdvanceX). Since imgui 1.92 this is a
     * scalar X-only value; the previous {@code GlyphExtraSpacing} ImVec2 was removed as largely obsolete.
     */
    @BindingField
    public float GlyphExtraAdvanceX;

    /**
     * Offset all glyphs from this font input.
     */
    @BindingField
    public ImVec2 GlyphOffset;

    private short[] glyphRanges;

    /** Returns a snapshot of the configured preload ranges, or null for the default ranges. */
    public short[] getGlyphRanges() {
        return glyphRanges == null ? null : glyphRanges.clone();
    }

    /** Copies zero-terminated Unicode range pairs. Registration retains its own independent copy. */
    public void setGlyphRanges(final short[] ranges) {
        checkGlyphRanges(ranges, false);
        final short[] copy = ranges == null ? null : ranges.clone();
        nSetGlyphRanges(copy);
        glyphRanges = copy;
    }

    private native void nSetGlyphRanges(short[] obj_ranges); /*MANUAL
        static_assert(sizeof(ImWchar) == sizeof(jshort), "This binding uses 16-bit glyph ranges");
        const int count = obj_ranges == NULL ? 0 : env->GetArrayLength(obj_ranges);
        ImWchar* copy = count > 0 ? (ImWchar*)IM_ALLOC(sizeof(ImWchar) * count) : NULL;
        if (count > 0 && copy == NULL) {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Cannot allocate glyph ranges");
            return;
        }
        if (copy != NULL)
            env->GetShortArrayRegion(obj_ranges, 0, count, (jshort*)copy);
        if (env->ExceptionCheck()) {
            IM_FREE(copy);
            return;
        }
        IM_FREE((void*)THIS->GlyphRanges);
        THIS->GlyphRanges = copy;
    */

    private short[] glyphExcludeRanges;
    private static final short[] NO_RANGES = {0};

    /** Returns a snapshot of the configured exclusion ranges, or null for none. */
    public short[] getGlyphExcludeRanges() {
        return glyphExcludeRanges == null ? null : glyphExcludeRanges.clone();
    }

    /**
     * Copies zero-terminated Unicode range pairs into this configuration's native storage.
     * Registration copies them again into the atlas, so the array and configuration may be released
     * after AddFont. At most 32 pairs are supported by the pinned native font API.
     */
    public void setGlyphExcludeRanges(final short[] ranges) {
        checkGlyphRanges(ranges, true);
        final short[] copy = ranges == null ? null : ranges.clone();
        nSetGlyphExcludeRanges(copy == null ? NO_RANGES : copy);
        glyphExcludeRanges = copy;
    }

    private void checkGlyphRanges(final short[] ranges, final boolean exclusions) {
        if (ptr == 0) {
            throw new IllegalStateException("Font configuration has been destroyed");
        }
        validateGlyphRanges(ranges, exclusions);
    }

    static void validateGlyphRanges(final short[] ranges, final boolean exclusions) {
        if (ranges == null) {
            return;
        }
        if (ranges.length == 0 || (ranges.length & 1) == 0 || ranges[ranges.length - 1] != 0
                || (exclusions && ranges.length > 65)) {
            throw new IllegalArgumentException(exclusions
                    ? "Expected at most 32 zero-terminated Unicode range pairs"
                    : "Expected zero-terminated Unicode range pairs");
        }
        for (int i = 0; i < ranges.length - 1; i += 2) {
            if ((ranges[i] & 0xffff) == 0 || (ranges[i] & 0xffff) > (ranges[i + 1] & 0xffff)) {
                throw new IllegalArgumentException("Invalid Unicode range");
            }
        }
    }

    private native void nSetGlyphExcludeRanges(short[] obj_ranges); /*MANUAL
        static_assert(sizeof(ImWchar) == sizeof(jshort), "This binding uses 16-bit glyph ranges");
        const int count = env->GetArrayLength(obj_ranges);
        ImWchar* copy = count > 1 ? (ImWchar*)IM_ALLOC(sizeof(ImWchar) * count) : NULL;
        if (count > 1 && copy == NULL) {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Cannot allocate glyph exclusions");
            return;
        }
        if (copy != NULL)
            env->GetShortArrayRegion(obj_ranges, 0, count, (jshort*)copy);
        if (env->ExceptionCheck()) {
            IM_FREE(copy);
            return;
        }
        IM_FREE((void*)THIS->GlyphExcludeRanges);
        THIS->GlyphExcludeRanges = copy;
    */

    /**
     * Minimum AdvanceX for glyphs, set Min to align font icons, set both Min/Max to enforce mono-space font
     */
    @BindingField
    public float GlyphMinAdvanceX;

    /**
     * Maximum AdvanceX for glyphs
     */
    @BindingField
    public float GlyphMaxAdvanceX;

    /**
     * Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons + Japanese glyphs).
     * You may want to use GlyphOffset.y when merge font of different heights.
     */
    @BindingField
    public boolean MergeMode;

    /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    @BindingField(isFlag = true)
    public int FontLoaderFlags;

    /**
     * Brighten ({@code >}1.0f) or darken ({@code <}1.0f) font output. Brightening small fonts may be a good workaround to make them more readable.
     */
    @BindingField
    public float RasterizerMultiply;

    /**
     * DPI scale for rasterization, not altering other font metrics: make it easy to swap between e.g. a 100% and a 400% fonts for a zooming display. IMPORTANT: If you increase this it is expected that you increase font scale accordingly, otherwise quality may look lowered.
     */
    @BindingField
    public float RasterizerDensity;

    /**
     * Explicitly specify unicode codepoint of ellipsis character. When fonts are being merged first specified ellipsis will be used.
     */
    @BindingField
    public short EllipsisChar;

    // [Internal]

    /**
     * Name (strictly to ease debugging)
     */
    public native void setName(String name); /*
        strcpy(THIS->Name, name);
    */

    @BindingField
    public ImFont DstFont;

    /*JNI
        #undef THIS
     */
}
