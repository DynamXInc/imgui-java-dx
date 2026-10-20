package imgui;

import imgui.binding.ImGuiStructDestroyable;




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

     /**
     * Size of the configuration's native TTF/OTF copy.
     */
    public int getFontDataSize() {
        return nGetFontDataSize();
    }

    private native int nGetFontDataSize(); /*
        return THIS->FontDataSize;
    */

     /**
     * Index of font within TTF/OTF file
     */
    public int getFontNo() {
        return nGetFontNo();
    }

    /**
     * Index of font within TTF/OTF file
     */
    public void setFontNo(final int value) {
        nSetFontNo(value);
    }

    private native int nGetFontNo(); /*
        return THIS->FontNo;
    */

    private native void nSetFontNo(int value); /*
        THIS->FontNo = value;
    */

     /**
     * Size in pixels for rasterizer (more or less maps to the resulting font height).
     */
    public float getSizePixels() {
        return nGetSizePixels();
    }

    /**
     * Size in pixels for rasterizer (more or less maps to the resulting font height).
     */
    public void setSizePixels(final float value) {
        nSetSizePixels(value);
    }

    private native float nGetSizePixels(); /*
        return THIS->SizePixels;
    */

    private native void nSetSizePixels(float value); /*
        THIS->SizePixels = value;
    */

     /**
     * Rasterize at higher quality for sub-pixel positioning.
     * Note the difference between 2 and 3 is minimal so you can reduce this to 2 to save memory.
     * Read https://github.com/nothings/stb/blob/master/tests/oversample/README.md for details.
     */
    public int getOversampleH() {
        return nGetOversampleH();
    }

    /**
     * Rasterize at higher quality for sub-pixel positioning.
     * Note the difference between 2 and 3 is minimal so you can reduce this to 2 to save memory.
     * Read https://github.com/nothings/stb/blob/master/tests/oversample/README.md for details.
     */
    public void setOversampleH(final int value) {
        nSetOversampleH(value);
    }

    private native int nGetOversampleH(); /*
        return THIS->OversampleH;
    */

    private native void nSetOversampleH(int value); /*
        THIS->OversampleH = value;
    */

     /**
     * Rasterize at higher quality for sub-pixel positioning.
     * This is not really useful as we don't use sub-pixel positions on the Y axis.
     */
    public int getOversampleV() {
        return nGetOversampleV();
    }

    /**
     * Rasterize at higher quality for sub-pixel positioning.
     * This is not really useful as we don't use sub-pixel positions on the Y axis.
     */
    public void setOversampleV(final int value) {
        nSetOversampleV(value);
    }

    private native int nGetOversampleV(); /*
        return THIS->OversampleV;
    */

    private native void nSetOversampleV(int value); /*
        THIS->OversampleV = value;
    */

     /**
     * Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default font.
     * If enabled, you can set OversampleH/V to 1.
     */
    public boolean getPixelSnapH() {
        return nGetPixelSnapH();
    }

    /**
     * Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default font.
     * If enabled, you can set OversampleH/V to 1.
     */
    public void setPixelSnapH(final boolean value) {
        nSetPixelSnapH(value);
    }

    private native boolean nGetPixelSnapH(); /*
        return THIS->PixelSnapH;
    */

    private native void nSetPixelSnapH(boolean value); /*
        THIS->PixelSnapH = value;
    */

     /**
     * Extra spacing (in pixels) between glyphs (added to glyph AdvanceX). Since imgui 1.92 this is a
     * scalar X-only value; the previous {@code GlyphExtraSpacing} ImVec2 was removed as largely obsolete.
     */
    public float getGlyphExtraAdvanceX() {
        return nGetGlyphExtraAdvanceX();
    }

    /**
     * Extra spacing (in pixels) between glyphs (added to glyph AdvanceX). Since imgui 1.92 this is a
     * scalar X-only value; the previous {@code GlyphExtraSpacing} ImVec2 was removed as largely obsolete.
     */
    public void setGlyphExtraAdvanceX(final float value) {
        nSetGlyphExtraAdvanceX(value);
    }

    private native float nGetGlyphExtraAdvanceX(); /*
        return THIS->GlyphExtraAdvanceX;
    */

    private native void nSetGlyphExtraAdvanceX(float value); /*
        THIS->GlyphExtraAdvanceX = value;
    */

     /**
     * Offset all glyphs from this font input.
     */
    public ImVec2 getGlyphOffset() {
        final ImVec2 dst = new ImVec2();
        nGetGlyphOffset(dst);
        return dst;
    }

    /**
     * Offset all glyphs from this font input.
     */
    public float getGlyphOffsetX() {
        return nGetGlyphOffsetX();
    }

    /**
     * Offset all glyphs from this font input.
     */
    public float getGlyphOffsetY() {
        return nGetGlyphOffsetY();
    }

    /**
     * Offset all glyphs from this font input.
     */
    public void getGlyphOffset(final ImVec2 dst) {
        nGetGlyphOffset(dst);
    }

    /**
     * Offset all glyphs from this font input.
     */
    public void setGlyphOffset(final ImVec2 value) {
        nSetGlyphOffset(value.x, value.y);
    }

    /**
     * Offset all glyphs from this font input.
     */
    public void setGlyphOffset(final float valueX, final float valueY) {
        nSetGlyphOffset(valueX, valueY);
    }

    private native void nGetGlyphOffset(ImVec2 dst); /*
        Jni::ImVec2Cpy(env, THIS->GlyphOffset, dst);
    */

    private native float nGetGlyphOffsetX(); /*
        return THIS->GlyphOffset.x;
    */

    private native float nGetGlyphOffsetY(); /*
        return THIS->GlyphOffset.y;
    */

    private native void nSetGlyphOffset(float valueX, float valueY); /*MANUAL
        ImVec2 value = ImVec2(valueX, valueY);
        THIS->GlyphOffset = value;
    */

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
    public float getGlyphMinAdvanceX() {
        return nGetGlyphMinAdvanceX();
    }

    /**
     * Minimum AdvanceX for glyphs, set Min to align font icons, set both Min/Max to enforce mono-space font
     */
    public void setGlyphMinAdvanceX(final float value) {
        nSetGlyphMinAdvanceX(value);
    }

    private native float nGetGlyphMinAdvanceX(); /*
        return THIS->GlyphMinAdvanceX;
    */

    private native void nSetGlyphMinAdvanceX(float value); /*
        THIS->GlyphMinAdvanceX = value;
    */

     /**
     * Maximum AdvanceX for glyphs
     */
    public float getGlyphMaxAdvanceX() {
        return nGetGlyphMaxAdvanceX();
    }

    /**
     * Maximum AdvanceX for glyphs
     */
    public void setGlyphMaxAdvanceX(final float value) {
        nSetGlyphMaxAdvanceX(value);
    }

    private native float nGetGlyphMaxAdvanceX(); /*
        return THIS->GlyphMaxAdvanceX;
    */

    private native void nSetGlyphMaxAdvanceX(float value); /*
        THIS->GlyphMaxAdvanceX = value;
    */

     /**
     * Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons + Japanese glyphs).
     * You may want to use GlyphOffset.y when merge font of different heights.
     */
    public boolean getMergeMode() {
        return nGetMergeMode();
    }

    /**
     * Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons + Japanese glyphs).
     * You may want to use GlyphOffset.y when merge font of different heights.
     */
    public void setMergeMode(final boolean value) {
        nSetMergeMode(value);
    }

    private native boolean nGetMergeMode(); /*
        return THIS->MergeMode;
    */

    private native void nSetMergeMode(boolean value); /*
        THIS->MergeMode = value;
    */

     /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    public int getFontLoaderFlags() {
        return nGetFontLoaderFlags();
    }

    /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    public void setFontLoaderFlags(final int value) {
        nSetFontLoaderFlags(value);
    }

    /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    public void addFontLoaderFlags(final int flags) {
        setFontLoaderFlags(getFontLoaderFlags() | flags);
    }

    /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    public void removeFontLoaderFlags(final int flags) {
        setFontLoaderFlags(getFontLoaderFlags() & ~(flags));
    }

    /**
     * Settings for custom font loader. THIS IS LOADER IMPLEMENTATION DEPENDENT. Leave as zero if unsure.
     * (Renamed from {@code FontBuilderFlags} in imgui 1.92.)
     */
    public boolean hasFontLoaderFlags(final int flags) {
        return (getFontLoaderFlags() & flags) != 0;
    }

    private native int nGetFontLoaderFlags(); /*
        return THIS->FontLoaderFlags;
    */

    private native void nSetFontLoaderFlags(int value); /*
        THIS->FontLoaderFlags = value;
    */

     /**
     * Brighten ({@code >}1.0f) or darken ({@code <}1.0f) font output. Brightening small fonts may be a good workaround to make them more readable.
     */
    public float getRasterizerMultiply() {
        return nGetRasterizerMultiply();
    }

    /**
     * Brighten ({@code >}1.0f) or darken ({@code <}1.0f) font output. Brightening small fonts may be a good workaround to make them more readable.
     */
    public void setRasterizerMultiply(final float value) {
        nSetRasterizerMultiply(value);
    }

    private native float nGetRasterizerMultiply(); /*
        return THIS->RasterizerMultiply;
    */

    private native void nSetRasterizerMultiply(float value); /*
        THIS->RasterizerMultiply = value;
    */

     /**
     * DPI scale for rasterization, not altering other font metrics: make it easy to swap between e.g. a 100% and a 400% fonts for a zooming display. IMPORTANT: If you increase this it is expected that you increase font scale accordingly, otherwise quality may look lowered.
     */
    public float getRasterizerDensity() {
        return nGetRasterizerDensity();
    }

    /**
     * DPI scale for rasterization, not altering other font metrics: make it easy to swap between e.g. a 100% and a 400% fonts for a zooming display. IMPORTANT: If you increase this it is expected that you increase font scale accordingly, otherwise quality may look lowered.
     */
    public void setRasterizerDensity(final float value) {
        nSetRasterizerDensity(value);
    }

    private native float nGetRasterizerDensity(); /*
        return THIS->RasterizerDensity;
    */

    private native void nSetRasterizerDensity(float value); /*
        THIS->RasterizerDensity = value;
    */

     /**
     * Explicitly specify unicode codepoint of ellipsis character. When fonts are being merged first specified ellipsis will be used.
     */
    public short getEllipsisChar() {
        return nGetEllipsisChar();
    }

    /**
     * Explicitly specify unicode codepoint of ellipsis character. When fonts are being merged first specified ellipsis will be used.
     */
    public void setEllipsisChar(final short value) {
        nSetEllipsisChar(value);
    }

    private native short nGetEllipsisChar(); /*
        return THIS->EllipsisChar;
    */

    private native void nSetEllipsisChar(short value); /*
        THIS->EllipsisChar = value;
    */

    // [Internal]

    /**
     * Name (strictly to ease debugging)
     */
    public native void setName(String name); /*
        strcpy(THIS->Name, name);
    */

    public ImFont getDstFont() {
        return new ImFont(nGetDstFont());
    }

    public void setDstFont(final ImFont value) {
        nSetDstFont(value.ptr);
    }

    private native long nGetDstFont(); /*
        return (uintptr_t)THIS->DstFont;
    */

    private native void nSetDstFont(long value); /*
        THIS->DstFont = reinterpret_cast<ImFont*>(value);
    */

    /*JNI
        #undef THIS
     */
}
