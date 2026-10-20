#pragma once

#include "imgui_internal.h"

// Keep the complete metric set and stack representation in sync with the pinned native implementation.
static_assert(IMGUI_VERSION_NUM == 19270, "Review style scaling against ImGuiStyle::ScaleAllSizes before upgrading Dear ImGui");

#define JNI_SCALED_STYLE_VARS(X) \
    X(WindowPadding) \
    X(WindowRounding) \
    X(WindowBorderSize) \
    X(WindowMinSize) \
    X(ChildRounding) \
    X(ChildBorderSize) \
    X(PopupRounding) \
    X(PopupBorderSize) \
    X(FramePadding) \
    X(FrameBorderSize) \
    X(FrameRounding) \
    X(ItemSpacing) \
    X(ItemInnerSpacing) \
    X(CellPadding) \
    X(IndentSpacing) \
    X(ScrollbarSize) \
    X(ScrollbarRounding) \
    X(ScrollbarPadding) \
    X(GrabMinSize) \
    X(GrabRounding) \
    X(ImageRounding) \
    X(ImageBorderSize) \
    X(TabRounding) \
    X(TabBorderSize) \
    X(TabMinWidthBase) \
    X(TabMinWidthShrink) \
    X(TabBarBorderSize) \
    X(TabBarOverlineSize) \
    X(TreeLinesSize) \
    X(TreeLinesRounding) \
    X(SeparatorSize) \
    X(SeparatorTextBorderSize) \
    X(SeparatorTextPadding) \
    X(DockingSeparatorSize)

namespace Jni
{
    static inline float ScaledStyleMetric(float baseline, float scale, ImGuiStyleVar var = ImGuiStyleVar_COUNT) {
        const float value = ImTrunc(baseline * scale);
        // These baseline metrics must remain positive for native separators/splitters.
        return var == ImGuiStyleVar_SeparatorSize || var == ImGuiStyleVar_DockingSeparatorSize ? ImMax(1.0f, value) : value;
    }

    static inline ImVec2 ScaledStyleMetric(const ImVec2& baseline, float scale, ImGuiStyleVar = ImGuiStyleVar_COUNT) {
        return ImVec2(ImTrunc(baseline.x * scale), ImTrunc(baseline.y * scale));
    }

    static inline void RebaseStyleOverride(ImGuiStyleMod& backup, float& current, float baseline, float scale, float ratio, bool nested) {
        if (nested) {
            backup.BackupFloat[0] *= ratio;
        } else {
            backup.BackupFloat[0] = ScaledStyleMetric(baseline, scale, backup.VarIdx);
            current *= ratio;
        }
    }

    static inline void RebaseStyleOverride(ImGuiStyleMod& backup, ImVec2& current, const ImVec2& baseline,
                                          float scale, float ratio, bool nested) {
        if (nested) {
            backup.BackupFloat[0] *= ratio;
            backup.BackupFloat[1] *= ratio;
        } else {
            const ImVec2 target = ScaledStyleMetric(baseline, scale);
            backup.BackupFloat[0] = target.x;
            backup.BackupFloat[1] = target.y;
            current.x *= ratio;
            current.y *= ratio;
        }
    }

    static inline float ScaledTabCloseWidth(float baseline, float scale) {
        return baseline > 0.0f && baseline != FLT_MAX ? ScaledStyleMetric(baseline, scale) : baseline;
    }

    static inline void ApplyStyleScale(const ImGuiStyle& baseline, float scale) {
        if (GImGui == NULL) {
            IM_ASSERT(GImGui != NULL && "Style scaling requires a current context");
            return;
        }
        ImGuiContext& g = *GImGui;
        ImGuiStyle& style = g.Style;
        if (&baseline == &style || baseline._MainScale != 1.0f) {
            IM_ASSERT(&baseline != &style && baseline._MainScale == 1.0f && "Style baseline must be independent and unscaled");
            return;
        }
        if (!(scale > 0.0f && scale <= FLT_MAX && style._MainScale > 0.0f && style._MainScale <= FLT_MAX)) {
            IM_ASSERT(false && "Style scales must be positive and finite");
            return;
        }
        const float ratio = scale / style._MainScale;
        if (!(ratio > 0.0f && ratio <= FLT_MAX)) {
            IM_ASSERT(false && "Style scale ratio must be representable");
            return;
        }

        // Walk once, bottom to top. The first backup belongs to the baseline; later backups belong to overrides.
        bool overridden[ImGuiStyleVar_COUNT] = {};
        for (int i = 0; i < g.StyleVarStack.Size; i++) {
            ImGuiStyleMod& backup = g.StyleVarStack[i];
            switch (backup.VarIdx) {
#define JNI_REBASE_STYLE_VAR(Name) \
            case ImGuiStyleVar_##Name: \
                RebaseStyleOverride(backup, style.Name, baseline.Name, scale, ratio, overridden[ImGuiStyleVar_##Name]); \
                overridden[ImGuiStyleVar_##Name] = true; \
                break;
                JNI_SCALED_STYLE_VARS(JNI_REBASE_STYLE_VAR)
#undef JNI_REBASE_STYLE_VAR
            default:
                break;
            }
        }

#define JNI_RESTORE_STYLE_VAR(Name) \
        if (!overridden[ImGuiStyleVar_##Name]) { style.Name = ScaledStyleMetric(baseline.Name, scale, ImGuiStyleVar_##Name); }
        JNI_SCALED_STYLE_VARS(JNI_RESTORE_STYLE_VAR)
#undef JNI_RESTORE_STYLE_VAR

        // ScaleAllSizes metrics without a StyleVar: always restore from the canonical baseline.
        style.WindowBorderHoverPadding = ScaledStyleMetric(baseline.WindowBorderHoverPadding, scale);
        style.TouchExtraPadding = ScaledStyleMetric(baseline.TouchExtraPadding, scale);
        style.ColumnsMinSpacing = ScaledStyleMetric(baseline.ColumnsMinSpacing, scale);
        style.LogSliderDeadzone = ScaledStyleMetric(baseline.LogSliderDeadzone, scale);
        style.TabCloseButtonMinWidthSelected = ScaledTabCloseWidth(baseline.TabCloseButtonMinWidthSelected, scale);
        style.TabCloseButtonMinWidthUnselected = ScaledTabCloseWidth(baseline.TabCloseButtonMinWidthUnselected, scale);
        style.DragDropTargetRounding = ScaledStyleMetric(baseline.DragDropTargetRounding, scale);
        style.DragDropTargetBorderSize = ScaledStyleMetric(baseline.DragDropTargetBorderSize, scale);
        style.DragDropTargetPadding = ScaledStyleMetric(baseline.DragDropTargetPadding, scale);
        style.ColorMarkerSize = ScaledStyleMetric(baseline.ColorMarkerSize, scale);
        style.DisplayWindowPadding = ScaledStyleMetric(baseline.DisplayWindowPadding, scale);
        style.DisplaySafeAreaPadding = ScaledStyleMetric(baseline.DisplaySafeAreaPadding, scale);
        style.MouseCursorScale = ScaledStyleMetric(baseline.MouseCursorScale, scale);
        style._MainScale = scale;

        // Begin changes viewport before SetCurrentWindow; End restores the parent window before changing viewport.
        // Do this even at the same metric scale: font DPI or FontScaleMain may have changed independently.
        if (g.CurrentWindow != NULL) {
            ImGui::UpdateCurrentFontSize(0.0f);
        }
    }
}

#undef JNI_SCALED_STYLE_VARS
