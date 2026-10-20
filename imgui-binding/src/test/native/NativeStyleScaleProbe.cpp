#include "jni_style_scale.h"
#include <cmath>
#include <cstdio>
#include <cstring>
#include <new>
#include <type_traits>
#include <initializer_list>

// CPU-only: no platform backend, renderer, JNI runtime, files, or native windows.
// Compile all translation units against include/imgui/imconfig.h, not the JNI imconfig.h.
namespace {
int assertions = 0, failures = 0, cases = 0;
char caseName[160] = "setup";

void Case(const char* name, int iteration = 0, float scale = 1.0f) {
    ++cases;
    std::snprintf(caseName, sizeof(caseName), "%s[%d] scale=%.6g", name, iteration, scale);
}
void Check(bool ok, const char* what) {
    ++assertions;
    if (!ok) {
        ++failures;
        std::fprintf(stderr, "FAIL %s: %s\n", caseName, what);
    }
}
void Near(float actual, float expected, const char* what) {
    ++assertions;
    if (!std::isfinite(actual) || std::fabs(actual - expected) > 0.0001f) {
        ++failures;
        std::fprintf(stderr, "FAIL %s: %s actual=%.9g expected=%.9g\n",
                     caseName, what, actual, expected);
    }
}
void Near(ImVec2 actual, ImVec2 expected, const char* what) {
    char label[128];
    std::snprintf(label, sizeof(label), "%s.x", what);
    Near(actual.x, expected.x, label);
    std::snprintf(label, sizeof(label), "%s.y", what);
    Near(actual.y, expected.y, label);
}

// Independent input coverage, not an implementation of the scaling oracle.
#define SCALARS(X) \
    X(WindowRounding) X(WindowBorderSize) X(WindowBorderHoverPadding) \
    X(ChildRounding) X(ChildBorderSize) X(PopupRounding) X(PopupBorderSize) \
    X(FrameRounding) X(FrameBorderSize) X(IndentSpacing) X(ColumnsMinSpacing) \
    X(ScrollbarSize) X(ScrollbarRounding) X(ScrollbarPadding) X(GrabMinSize) \
    X(GrabRounding) X(LogSliderDeadzone) X(ImageRounding) X(ImageBorderSize) \
    X(TabRounding) X(TabBorderSize) X(TabMinWidthBase) X(TabMinWidthShrink) \
    X(TabCloseButtonMinWidthSelected) X(TabCloseButtonMinWidthUnselected) \
    X(TabBarBorderSize) X(TabBarOverlineSize) X(TreeLinesSize) X(TreeLinesRounding) \
    X(DragDropTargetRounding) X(DragDropTargetBorderSize) X(DragDropTargetPadding) \
    X(ColorMarkerSize) X(SeparatorSize) X(SeparatorTextBorderSize) \
    X(DockingSeparatorSize) X(MouseCursorScale)
#define VECTORS(X) \
    X(WindowPadding) X(WindowMinSize) X(FramePadding) X(ItemSpacing) \
    X(ItemInnerSpacing) X(CellPadding) X(TouchExtraPadding) \
    X(SeparatorTextPadding) X(DisplayWindowPadding) X(DisplaySafeAreaPadding)

void CopyStyle(ImGuiStyle& to, const ImGuiStyle& from) {
    static_assert(std::is_trivially_copyable<ImGuiStyle>::value, "Style byte comparison requires trivial copies");
    std::memcpy(&to, &from, sizeof(to));
}
void Oracle(ImGuiStyle& expected, const ImGuiStyle& baseline, float scale) {
    CopyStyle(expected, baseline);
    expected.ScaleAllSizes(scale); // Execute the pinned upstream implementation.
    if (expected.SeparatorSize < 1.0f) expected.SeparatorSize = 1.0f;
    if (expected.DockingSeparatorSize < 1.0f) expected.DockingSeparatorSize = 1.0f;
}
void Parity(const ImGuiStyle& baseline, float scale) {
    ImGuiStyle expected;
    Oracle(expected, baseline, scale);
    Jni::ApplyStyleScale(baseline, scale);
    const ImGuiStyle& actual = ImGui::GetStyle();
#define COMPARE(Name) Near(actual.Name, expected.Name, #Name);
    SCALARS(COMPARE)
    VECTORS(COMPARE)
#undef COMPARE
    // Baseline padding starts zero; both branches retain it via memcpy. This also
    // checks every non-dimensional field/color, including fields outside the lists.
    Check(std::memcmp(&actual, &expected, sizeof(expected)) == 0,
          "complete style differs from native ScaleAllSizes oracle (except separator floors)");
}

void AbsoluteScales(ImGuiStyle& baseline) {
    const float scales[] = {0.25f, 1.0f, 1.25f, 1.75f, 2.0f, 1.75f, 1.25f, 1.0f, 0.25f, 2.0f, 1.0f};
    for (int input = 0; input < 2; ++input) {
        if (input == 1) {
            float n = 1.25f;
#define SEED_SCALAR(Name) baseline.Name = n; n += 0.75f;
#define SEED_VECTOR(Name) baseline.Name = ImVec2(n, n + 0.5f); n += 0.75f;
            SCALARS(SEED_SCALAR)
            VECTORS(SEED_VECTOR)
#undef SEED_SCALAR
#undef SEED_VECTOR
            baseline.SeparatorSize = baseline.DockingSeparatorSize = 1.0f;
            baseline.Alpha = 0.73f;
            baseline.ButtonTextAlign = ImVec2(0.27f, 0.61f);
            baseline.Colors[ImGuiCol_Text] = ImVec4(0.23f, 0.41f, 0.67f, 0.83f);
        }
        CopyStyle(ImGui::GetStyle(), baseline);
        for (int round = 0; round < 3; ++round)
            for (int i = 0; i < IM_COUNTOF(scales); ++i) {
                Case(input ? "dense absolute roundtrip" : "default absolute roundtrip",
                     round * IM_COUNTOF(scales) + i, scales[i]);
                Parity(baseline, scales[i]);
            }
    }
    const float widths[] = {-1.0f, 0.0f, FLT_MAX, 13.75f};
    for (int i = 0; i < IM_COUNTOF(widths); ++i) {
        baseline.TabCloseButtonMinWidthSelected = widths[i];
        baseline.TabCloseButtonMinWidthUnselected = widths[i];
        CopyStyle(ImGui::GetStyle(), baseline);
        for (float scale : scales) {
            Case("tab-close sentinel/finite", i, scale);
            Parity(baseline, scale);
        }
    }
    Case("same-scale baseline edit", 0, 1.75f);
    Jni::ApplyStyleScale(baseline, 1.75f);
    baseline.FramePadding = ImVec2(19.75f, 7.5f);
    baseline.DragDropTargetPadding = 9.75f;
    Parity(baseline, 1.75f);
}
#undef SCALARS
#undef VECTORS

const ImGuiStyle* callbackBaseline = nullptr;
int callbackCalls = 0;
void ViewportChanged(ImGuiViewport*) {
    ++callbackCalls;
    Jni::ApplyStyleScale(*callbackBaseline, 1.75f);
}

void Overrides(const ImGuiStyle& baseline) {
    ImGuiStyle& s = ImGui::GetStyle();
    Case("zero padding consumed by Begin callback", 0, 1.75f);
    callbackBaseline = &baseline;
    callbackCalls = 0;
    ImGui::GetPlatformIO().Platform_OnChangedViewport = ViewportChanged;
    ImGui::SetCurrentViewport(nullptr, nullptr); // Force a real Begin viewport transition, no OS window.
    ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0, 0));
    ImGui::SetNextWindowPos(ImVec2(40, 40));
    ImGui::SetNextWindowSize(ImVec2(640, 400));
    const bool visible = ImGui::Begin("native-style-zero-padding", nullptr,
        ImGuiWindowFlags_NoTitleBar | ImGuiWindowFlags_NoResize | ImGuiWindowFlags_NoScrollbar |
        ImGuiWindowFlags_NoSavedSettings);
    ImGui::GetPlatformIO().Platform_OnChangedViewport = nullptr;
    Check(visible, "Begin provides an active content surface");
    Check(callbackCalls == 1, "Begin invokes the actual viewport callback exactly once");
    Near(s.WindowPadding, ImVec2(0, 0), "live zero WindowPadding");
    Near(ImGui::GetCurrentWindow()->WindowPadding, ImVec2(0, 0), "Begin-latched padding");
    ImVec2 cursor = ImGui::GetCursorScreenPos(), pos = ImGui::GetWindowPos();
    Near(ImVec2(cursor.x - pos.x, cursor.y - pos.y), ImVec2(0, 0), "consumed content origin");

    Jni::ApplyStyleScale(baseline, 1.0f);
    const ImVec4 originalColor = s.Colors[ImGuiCol_Text];
    const ImVec4 color(0.125f, 0.25f, 0.5f, 0.75f);
    ImGui::PushStyleColor(ImGuiCol_Text, color);
    ImGui::PushStyleVar(ImGuiStyleVar_Alpha, 0.375f);
    ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 2.75f);
    ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(3.25f, 5.5f));
    Case("outer scalar/vector override", 0, 2.0f);
    Jni::ApplyStyleScale(baseline, 2.0f);
    Near(s.FrameRounding, 5.5f, "outer scalar");
    Near(s.FramePadding, ImVec2(6.5f, 11.0f), "outer vector");
    ImGui::PushStyleVar(ImGuiStyleVar_FrameRounding, 7.25f);
    ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(7.75f, 9.25f));
    Case("nested scalar/vector override", 0, 1.25f);
    Jni::ApplyStyleScale(baseline, 1.25f);
    Near(s.FrameRounding, 4.53125f, "inner scalar, no truncation");
    Near(s.FramePadding, ImVec2(4.84375f, 5.78125f), "inner vector, no truncation");
    Near(s.Alpha, 0.375f, "Alpha override remains dimensionless");
    Check(std::memcmp(&s.Colors[ImGuiCol_Text], &color, sizeof(color)) == 0, "color override unchanged");
    ImGui::PopStyleVar(2);
    Near(s.FrameRounding, 3.4375f, "inner pop restores rebased outer scalar");
    Near(s.FramePadding, ImVec2(4.0625f, 6.875f), "inner pop restores rebased outer vector");
    Case("outer cross-scale pop", 0, 0.25f);
    Jni::ApplyStyleScale(baseline, 0.25f);
    Near(s.FrameRounding, 0.6875f, "fractional scalar survives shrink");
    Near(s.FramePadding, ImVec2(0.8125f, 1.375f), "fractional vector survives shrink");
    ImGui::PopStyleVar(2);
    ImGuiStyle expected;
    Oracle(expected, baseline, 0.25f);
    Near(s.FrameRounding, expected.FrameRounding, "outer pop restores canonical scalar");
    Near(s.FramePadding, expected.FramePadding, "outer pop restores canonical vector");
    ImGui::PopStyleVar();
    ImGui::PopStyleColor();
    Near(s.Alpha, baseline.Alpha, "Alpha pop restores unscaled backup");
    Check(std::memcmp(&s.Colors[ImGuiCol_Text], &originalColor, sizeof(originalColor)) == 0,
          "color pop restores original backup");

    Case("X/Y-only nested pushes", 0, 1.25f);
    Jni::ApplyStyleScale(baseline, 1.0f);
    const float baselineY = s.FramePadding.y;
    ImGui::PushStyleVarX(ImGuiStyleVar_FramePadding, 3.25f);
    ImGui::PushStyleVarY(ImGuiStyleVar_FramePadding, 5.75f);
    Jni::ApplyStyleScale(baseline, 1.25f);
    Near(s.FramePadding, ImVec2(4.0625f, 7.1875f), "X/Y top layer");
    ImGui::PopStyleVar();
    Near(s.FramePadding, ImVec2(4.0625f, baselineY * 1.25f), "Y pop restores rebased X layer");
    Jni::ApplyStyleScale(baseline, 1.75f);
    ImGui::PopStyleVar();
    Oracle(expected, baseline, 1.75f);
    Near(s.FramePadding, expected.FramePadding, "X pop restores new canonical baseline");

    Case("live separator overrides have no baseline floor", 0, 0.25f);
    Jni::ApplyStyleScale(baseline, 1.0f);
    ImGui::PushStyleVar(ImGuiStyleVar_SeparatorSize, 0.5f);
    ImGui::PushStyleVar(ImGuiStyleVar_DockingSeparatorSize, 0.0f);
    Jni::ApplyStyleScale(baseline, 0.25f);
    Near(s.SeparatorSize, 0.125f, "fractional live separator");
    Near(s.DockingSeparatorSize, 0.0f, "zero live docking separator");
    ImGui::PopStyleVar(2);
    Near(s.SeparatorSize, 1.0f, "separator pop restores floored baseline");
    Near(s.DockingSeparatorSize, 1.0f, "docking separator pop restores floored baseline");
    ImGui::End();
    ImGui::PopStyleVar(); // WindowPadding crossed Begin, all scale changes, and End.
    Oracle(expected, baseline, 0.25f);
    Near(s.WindowPadding, expected.WindowPadding, "window padding cross-Begin pop");
}

void FontCase(const ImGuiStyle& baseline, ImFont* font, float base, float zoom, float dpi,
              float metricScale, const char* label, bool dynamic) {
    Case(label, dynamic ? 1 : 0, metricScale);
    ImGuiStyle& s = ImGui::GetStyle();
    s.FontScaleMain = zoom;
    s.FontScaleDpi = dpi;
    Jni::ApplyStyleScale(baseline, metricScale);
    const float expected = std::floor(base * zoom * dpi + 0.5f);
    Near(ImGui::GetFontSize(), expected, "public font size refreshed immediately");
    Near(ImGui::GetTextLineHeight(), expected, "layout line height refreshed immediately");
    Check(ImGui::GetFont() == font, "active font identity preserved");
    ImFontBaked* actualBaked = ImGui::GetFontBaked(); // Capture before the independent lookup.
    ImFontBaked* expectedBaked = font->GetFontBaked(expected);
    Check(actualBaked == expectedBaked, "current baked cache matches requested font/size");
    if (dynamic) Near(actualBaked ? actualBaked->Size : -1.0f, expected, "dynamic baked size refreshed");
    const char* text = "DPI zoom MW 012345";
    ImVec2 actualText = ImGui::CalcTextSize(text);
    ImVec2 expectedText = font->CalcTextSizeA(expected, FLT_MAX, 0.0f, text);
    expectedText.x = std::floor(expectedText.x + 0.99999f); // Public CalcTextSize pixel rounding.
    Near(actualText, expectedText, "consumer text measurement");
    Near(ImGui::GetFrameHeight(), expected + s.FramePadding.y * 2.0f, "consumer frame height");

    // The implicit AddText overload consumes the shared font cache, unlike
    // CalcTextSize. Compare its actual geometry to an explicit-size native draw.
    ImDrawList* draw = ImGui::GetWindowDrawList();
    const ImVec2 origin(100, 120);
    const int first = draw->VtxBuffer.Size;
    draw->AddText(origin, IM_COL32_WHITE, "MW");
    const int middle = draw->VtxBuffer.Size;
    draw->AddText(font, expected, origin, IM_COL32_WHITE, "MW");
    const int last = draw->VtxBuffer.Size;
    Check(middle - first == 8, "implicit text emits two glyph quads");
    Check(last - middle == 8, "explicit-size text emits two glyph quads");
    bool sameGeometry = middle - first == 8 && last - middle == 8;
    for (int i = 0; sameGeometry && i < 8; ++i) {
        const ImDrawVert& a = draw->VtxBuffer[first + i];
        const ImDrawVert& b = draw->VtxBuffer[middle + i];
        sameGeometry = a.pos.x == b.pos.x && a.pos.y == b.pos.y &&
                       a.uv.x == b.uv.x && a.uv.y == b.uv.y && a.col == b.col;
    }
    Check(sameGeometry, "shared font cache produces explicit-size glyph geometry");
}

void FrameCases(const ImGuiStyle& baseline, bool dynamic) {
    ImGuiContext* context = ImGui::CreateContext();
    CopyStyle(ImGui::GetStyle(), baseline);
    ImGuiIO& io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.LogFilename = nullptr;
    io.DisplaySize = ImVec2(1000, 800);
    io.DeltaTime = 1.0f / 60.0f;
    ImFontConfig config;
    config.SizePixels = 16.0f;
    ImFont* font = io.Fonts->AddFontDefaultVector(&config);
    ImFont* secondFont = io.Fonts->AddFontDefaultBitmap(&config);
    Case(dynamic ? "dynamic CPU atlas setup" : "legacy CPU atlas setup");
    Check(font != nullptr && secondFont != nullptr, "both real embedded fonts loaded");
    if (!font || !secondFont) { ImGui::DestroyContext(context); return; }
    io.FontDefault = font;
    if (dynamic) {
        // Enable native demand baking only; no Render(), GPU upload, or texture IDs.
        io.BackendFlags |= ImGuiBackendFlags_RendererHasTextures;
    } else {
        Check(io.Fonts->Build(), "legacy atlas built before NewFrame");
        if (!io.Fonts->IsBuilt()) { ImGui::DestroyContext(context); return; }
    }
    ImGui::NewFrame();
    if (!dynamic) Overrides(baseline);
    Jni::ApplyStyleScale(baseline, 1.0f);
    ImGui::SetNextWindowPos(ImVec2(20, 20));
    ImGui::SetNextWindowSize(ImVec2(800, 600));
    Case("current-window font fixture", dynamic ? 1 : 0);
    Check(ImGui::Begin("native-style-font-cache", nullptr, ImGuiWindowFlags_NoSavedSettings),
          "font fixture content is not skipped");
    ImGui::PushFont(font, 16.0f);
    FontCase(baseline, font, 16, 1, 1, 1, "font initial", dynamic);
    FontCase(baseline, font, 16, 1.25f, 1.75f, 2, "font zoom and DPI changed", dynamic);
    FontCase(baseline, font, 16, 1.25f, 1.25f, 2, "same metric scale, DPI changed", dynamic);
    FontCase(baseline, font, 16, 2, 1.25f, 2, "same metric scale, zoom changed", dynamic);
    ImGui::PushFont(secondFont, 18.0f);
    FontCase(baseline, secondFont, 18, 1, 1.75f, 1.75f, "alternate active font preserved", dynamic);
    ImGui::PopFont();
    FontCase(baseline, font, 16, 1, 1, 1, "font stack return refreshed", dynamic);
    ImGui::PopFont();
    ImGui::End();
    ImGui::EndFrame();
    ImGui::DestroyContext(context);
}

ImVec2 RasterBounds(float size, float density) {
    ImFontAtlas atlas;
    ImFontConfig config;
    config.SizePixels = size;
    config.RasterizerDensity = density;
    config.OversampleH = config.OversampleV = 2;
    ImFont* font = atlas.AddFontDefaultVector(&config);
    Check(atlas.Build(), "legacy density atlas builds");
    const ImFontGlyph* glyph = font->GetFontBaked(font->LegacySize)->FindGlyph('M');
    return ImVec2((glyph->U1 - glyph->U0) * atlas.TexData->Width,
                  (glyph->V1 - glyph->V0) * atlas.TexData->Height);
}

void LegacyDensityCases() {
    for (float density : {1.0f, 1.25f, 1.75f, 2.0f, 2.1875f, 3.0f, 4.0f}) {
        Case("legacy density equals same physical font size", 0, density);
        Near(RasterBounds(16.0f, density), RasterBounds(16.0f * density, 1.0f),
             "rasterized glyph dimensions apply requested density exactly once");
    }
}

void LegacyRangeLifetimeCase() {
    Case("registered glyph ranges survive caller mutation");
    ImFontAtlas atlas;
    ImFontConfig config;
    ImWchar ranges[] = {'M', 'M', 0};
    config.GlyphRanges = ranges;
    ImFont* font = atlas.AddFontDefaultVector(&config);
    ranges[0] = ranges[1] = 'X';
    Check(atlas.Build(), "range lifetime atlas builds");
    ImFontBaked* baked = font->GetFontBaked(font->LegacySize);
    Check(baked->IsGlyphLoaded('M'), "registered M is preloaded before legacy texture upload");
}
} // namespace

int main() {
    if (!IMGUI_CHECKVERSION()) return 2;
    // Placement construction preserves known-zero padding, then all style copies
    // use memcpy. No byte offsets or undocumented field access are involved.
    alignas(ImGuiStyle) unsigned char baselineStorage[sizeof(ImGuiStyle)] = {};
    ImGuiStyle& baseline = *new (baselineStorage) ImGuiStyle();
    ImGuiContext* context = ImGui::CreateContext();
    ImGui::GetIO().IniFilename = nullptr;
    ImGui::GetIO().LogFilename = nullptr;
    AbsoluteScales(baseline);
    ImGui::DestroyContext(context);
    baseline.~ImGuiStyle();
    std::memset(baselineStorage, 0, sizeof(baselineStorage));
    new (baselineStorage) ImGuiStyle();
    FrameCases(baseline, false);
    FrameCases(baseline, true);
    LegacyDensityCases();
    LegacyRangeLifetimeCase();
    baseline.~ImGuiStyle();
    std::printf("NativeStyleScaleProbe %s: %d/%d assertions passed; %d cases; %d failures\n",
                failures ? "FAIL" : "PASS", assertions - failures, assertions, cases, failures);
    return failures ? 1 : 0;
}
