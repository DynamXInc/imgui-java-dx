package imgui.internal;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGuiPlatformMonitor;
import imgui.ImGuiStyle;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.binding.annotation.ArgValue;
import imgui.binding.annotation.BindingMethod;
import imgui.binding.annotation.BindingSource;
import imgui.binding.annotation.OptArg;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;

@BindingSource
public final class ImGui extends imgui.ImGui {
    /*JNI
        #include "_common.h"
        #include "_internal.h"
        #include "jni_style_scale.h"
     */

    static {
        nInit();
    }

    public static void init() {
    }

    private static native void nInit(); /*
        Jni::InitInternal(env);
    */

    /**
     * Applies the complete native size-metric set from an independent, unscaled baseline.
     * Capture that baseline before the first scale operation (native reference scale must be one).
     * Every call reapplies unoverridden metrics, even at the same scale; no separate force/reset is needed.
     * Direct size edits belong in the baseline, while scoped overrides must use PushStyleVar/PopStyleVar.
     * Do not assign or otherwise rescale the active style between calls.
     *
     * <p>The lowest backup of each dimensional style variable is replaced by the scaled baseline.
     * Nested backups and live overrides retain their values at the new/old scale ratio without integer
     * truncation. Native X/Y-only pushes store whole vectors and are rebased as such.
     * Baseline separator and docking-separator thicknesses retain their native one-pixel minimum;
     * this floor never overwrites a live scoped override.
     * Non-dimensional variables, colors and font configuration are not copied from the baseline.
     * The current font cache is refreshed when a current window exists, including viewport changes in End().
     *
     * @param unscaledBaseline independent native style with reference scale one
     * @param effectiveScale positive finite absolute size scale
     * @throws IllegalArgumentException if the baseline is null/invalid or scale is not positive and finite
     */
    public static void applyStyleScale(final ImGuiStyle unscaledBaseline, final float effectiveScale) {
        if (unscaledBaseline == null || unscaledBaseline.isNotValidPtr()) {
            throw new IllegalArgumentException("Style scaling requires a valid independent baseline");
        }
        if (!(effectiveScale > 0.0f) || !Float.isFinite(effectiveScale)) {
            throw new IllegalArgumentException("Style scale must be positive and finite");
        }
        nApplyStyleScale(unscaledBaseline.ptr, effectiveScale);
    }

    private static native void nApplyStyleScale(long baselinePtr, float effectiveScale); /*
        Jni::ApplyStyleScale(*reinterpret_cast<const ImGuiStyle*>(baselinePtr), effectiveScale);
    */

    // Windows
    // We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
    // If this ever crash because g.CurrentWindow is NULL it means that either
    // - ImGui::NewFrame() has never been called, which is illegal.
    // - You are calling ImGui functions after ImGui::EndFrame()/ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal.

    @BindingMethod
    public static native ImGuiWindow GetCurrentWindowRead();

    @BindingMethod
    public static native ImGuiWindow GetCurrentWindow();

    @BindingMethod
    public static native ImGuiWindow FindWindowByID(int id);

    @BindingMethod
    public static native ImGuiWindow FindWindowByName(String name);

    @BindingMethod
    public static native void UpdateWindowParentAndRootLinks(ImGuiWindow window, int flags, ImGuiWindow parentWindow);

    @BindingMethod
    public static native ImVec2 CalcWindowNextAutoFitSize(ImGuiWindow window);

    @BindingMethod
    public static native boolean IsWindowChildOf(ImGuiWindow window, ImGuiWindow potentialParent, boolean popupHierarchy, boolean dockHierarchy);

    @BindingMethod
    public static native boolean IsWindowWithinBeginStackOf(ImGuiWindow window, ImGuiWindow potentialParent);

    @BindingMethod
    public static native boolean IsWindowAbove(ImGuiWindow potentialAbove, ImGuiWindow potentialBelow);

    @BindingMethod
    public static native boolean IsWindowNavFocusable(ImGuiWindow window);

    @BindingMethod
    public static native void SetWindowPos(ImGuiWindow window, ImVec2 pos, @OptArg int cond);

    @BindingMethod
    public static native void SetWindowSize(ImGuiWindow window, ImVec2 size, @OptArg int cond);

    @BindingMethod
    public static native void SetWindowCollapsed(ImGuiWindow window, @ArgValue(staticCast = "bool") boolean collapsed, @OptArg int cond);

    @BindingMethod
    public static native void SetWindowHitTestHole(ImGuiWindow window, ImVec2 pos, ImVec2 size);

    @BindingMethod
    public static native ImRect WindowRectAbsToRel(ImGuiWindow window, ImRect r);

    @BindingMethod
    public static native ImRect WindowRectRelToAbs(ImGuiWindow window, ImRect r);


    // Windows: Display Order and Focus Order

    @BindingMethod
    public static native void FocusWindow(ImGuiWindow window, @OptArg @ArgValue(staticCast = "ImGuiFocusRequestFlags") int flags);

    @BindingMethod
    public static native void FocusTopMostWindowUnderOne(ImGuiWindow underThisWindow, ImGuiWindow ignoreWindow, ImGuiViewport filterViewport, @ArgValue(staticCast = "ImGuiFocusRequestFlags") int flags);

    @BindingMethod
    public static native void BringWindowToFocusFront(ImGuiWindow window);

    @BindingMethod
    public static native void BringWindowToDisplayFront(ImGuiWindow window);

    @BindingMethod
    public static native void BringWindowToDisplayBack(ImGuiWindow window);

    @BindingMethod
    public static native void BringWindowToDisplayBehind(ImGuiWindow window, ImGuiWindow aboveWindow);

    @BindingMethod
    public static native int FindWindowDisplayIndex(ImGuiWindow window);

    @BindingMethod
    public static native ImGuiWindow FindBottomMostVisibleWindowWithinBeginStack(ImGuiWindow window);

    // Fonts, drawing

    @BindingMethod
    public static native void SetCurrentFont(ImFont font, float fontSizeBeforeScaling, float fontSizeAfterScaling);

    @BindingMethod
    public static native ImFont GetDefaultFont();

    @BindingMethod
    public static native ImDrawList GetForegroundDrawList(ImGuiWindow window);

    // Init

    @BindingMethod
    public static native void Initialize();

    @BindingMethod
    public static native void Shutdown();

    // NewFrame

    @BindingMethod
    public static native void UpdateInputEvents(boolean trickleFastInputs);

    @BindingMethod
    public static native void UpdateHoveredWindowAndCaptureFlags(ImVec2 mousePos);

    @BindingMethod
    public static native void StartMouseMovingWindow(ImGuiWindow window);

    @BindingMethod
    public static native void StartMouseMovingWindowOrNode(ImGuiWindow window, ImGuiDockNode node, boolean undockFloatingNode);

    @BindingMethod
    public static native void UpdateMouseMovingWindowNewFrame();

    @BindingMethod
    public static native void UpdateMouseMovingWindowEndFrame();

    // TODO: Generic context hooks

    // Viewports

    @BindingMethod
    public static native ImGuiPlatformMonitor GetViewportPlatformMonitor(ImGuiViewport viewport);

    // TODO: Settings

    // Scrolling

    @BindingMethod
    public static native void SetNextWindowScroll(ImVec2 scroll);

    @BindingMethod
    public static native void SetScrollX(ImGuiWindow window, float scrollX);

    @BindingMethod
    public static native void SetScrollY(ImGuiWindow window, float scrollY);

    @BindingMethod
    public static native void SetScrollFromPosX(ImGuiWindow window, float localX, float centerXRatio);

    @BindingMethod
    public static native void SetScrollFromPosY(ImGuiWindow window, float localY, float centerYRatio);

    // Early work-in-progress API (ScrollToItem() will become public)

    @BindingMethod
    public static native void ScrollToItem(@OptArg int flags);

    @BindingMethod
    public static native void ScrollToRect(ImGuiWindow window, ImRect rect, @OptArg int flags);

    @BindingMethod
    public static native ImVec2 ScrollToRectEx(ImGuiWindow window, ImRect rect, @OptArg int flags);

    // Basic Accessors

    @BindingMethod
    public static native int GetItemStatusFlags();

    @BindingMethod
    public static native int GetItemFlags();

    @BindingMethod
    public static native int GetActiveID();

    @BindingMethod
    public static native int GetFocusID();

    @BindingMethod
    public static native void SetActiveID(int id, ImGuiWindow window);

    @BindingMethod
    public static native void SetFocusID(int id, ImGuiWindow window);

    @BindingMethod
    public static native void ClearActiveID();

    @BindingMethod
    public static native int GetHoveredID();

    @BindingMethod
    public static native void SetHoveredID(int id);

    @BindingMethod
    public static native void KeepAliveID(int id);

    @BindingMethod
    public static native void MarkItemEdited(int id);

    @BindingMethod
    public static native void PushOverrideID(int id);

    @BindingMethod
    public static native int GetIDWithSeed(String strIdBegin, String strIdEnd, int seed);

    // Basic Helpers for widget code

    @BindingMethod
    public static native void ItemSize(ImVec2 size, @OptArg float textBaselineY);

    @BindingMethod
    public static native void ItemSize(ImRect bb, @OptArg float textBaselineY);

    // TODO: ItemAdd

    @BindingMethod
    public static native boolean ItemHoverable(ImRect bb, int id, @ArgValue(staticCast = "ImGuiItemFlags") int itemFlags);

    @BindingMethod
    public static native boolean IsWindowContentHoverable(ImGuiWindow window, @ArgValue(staticCast = "ImGuiHoveredFlags") int flags);

    @BindingMethod
    public static native boolean IsClippedEx(ImRect bb, int id);

    @BindingMethod
    public static native void SetLastItemData(int itemId, int inFlags, int statusFlags, ImRect itemRect);

    @BindingMethod
    public static native ImVec2 CalcItemSize(ImVec2 size, float defaultW, float defaultH);

    @BindingMethod
    public static native float CalcWrapWidthForPos(ImVec2 pos, float wrapPosX);

    @BindingMethod
    public static native void PushMultiItemsWidths(int components, float widthFull);

    @BindingMethod
    public static native boolean IsItemToggledSelection();

    // GetContentRegionMaxAbs was removed from imgui_internal in imgui 1.92; the equivalent
    // is now '(window->Pos - window->Scroll + window->ContentRegionRect.Max)'.

    // TODO: ShrinkWidths

    // Parameter stacks

    @BindingMethod
    public static native void PushItemFlag(int option, boolean enabled);

    @BindingMethod
    public static native void PopItemFlag();

    // Docking - Builder function needs to be generally called before the node is used/submitted.
    // - The DockBuilderXXX functions are designed to _eventually_ become a public API, but it is too early to expose it and guarantee stability.
    // - Do not hold on ImGuiDockNode* pointers! They may be invalidated by any split/merge/remove operation and every frame.
    // - To create a DockSpace() node, make sure to set the ImGuiDockNodeFlags_DockSpace flag when calling DockBuilderAddNode().
    //   You can create dockspace nodes (attached to a window) _or_ floating nodes (carry its own window) with this API.
    // - DockBuilderSplitNode() create 2 child nodes within 1 node. The initial node becomes a parent node.
    // - If you intend to split the node immediately after creation using DockBuilderSplitNode(), make sure
    //   to call DockBuilderSetNodeSize() beforehand. If you don't, the resulting split sizes may not be reliable.
    // - Call DockBuilderFinish() after you are done.

    @BindingMethod
    public static native void DockBuilderDockWindow(String windowName, int nodeId);

    @BindingMethod
    public static native ImGuiDockNode DockBuilderGetNode(int nodeId);

    @BindingMethod
    public static native ImGuiDockNode DockBuilderGetCentralNode(int nodeId);

    @BindingMethod
    public static native int DockBuilderAddNode(@OptArg int nodeId, @OptArg int flags);

    /**
     * Remove node and all its child, undock all windows.
     */
    @BindingMethod
    public static native void DockBuilderRemoveNode(int nodeId);

    @BindingMethod
    public static native void DockBuilderRemoveNodeDockedWindows(int nodeId, @OptArg boolean clearSettingsRefs);

    /**
     * Remove all split/hierarchy. All remaining docked windows will be re-docked to the remaining root node (node_id).
     */
    @BindingMethod
    public static native void DockBuilderRemoveNodeChildNodes(int nodeId);

    @BindingMethod
    public static native void DockBuilderSetNodePos(int nodeId, ImVec2 pos);

    @BindingMethod
    public static native void DockBuilderSetNodeSize(int nodeId, ImVec2 size);

    /**
     * Create 2 child nodes in this parent node.
     */
    @BindingMethod
    public static native int DockBuilderSplitNode(int nodeId, @ArgValue(staticCast = "ImGuiDir") int splitDir, float sizeRatioForNodeAtDir, @ArgValue(reinterpretCast = "ImGuiID*") ImInt outIdAtDir, @ArgValue(reinterpretCast = "ImGuiID*") ImInt outIdAtOppositeDir);

    // TODO DockBuilderCopyDockSpace, DockBuilderCopyNode

    @BindingMethod
    public static native void DockBuilderCopyWindowSettings(String srcName, String dstName);

    @BindingMethod
    public static native void DockBuilderFinish(int nodeId);

    // Tables: Candidates for public API

    @BindingMethod
    public static native void TableOpenContextMenu(@OptArg int columnN);

    @BindingMethod
    public static native void TableSetColumnWidth(int columnN, float width);

    @BindingMethod
    public static native void TableSetColumnSortDirection(int columnN, @ArgValue(staticCast = "ImGuiSortDirection") int sortDirection, boolean appendToSortSpecs);

    /**
     * May use {@code (TableGetColumnFlags() & ImGuiTableColumnFlags_IsHovered)} instead. Return hovered column.
     * Return -1 when table is not hovered. return columns_count if the unused space at the right of visible columns is hovered.
     */
    @BindingMethod
    public static native int TableGetHoveredColumn();

    /**
     * Retrieve *PREVIOUS FRAME* hovered row. This difference with TableGetHoveredColumn() is the reason why this is not public yet.
     */
    @BindingMethod
    public static native int TableGetHoveredRow();

    @BindingMethod
    public static native float TableGetHeaderRowHeight();

    @BindingMethod
    public static native void TablePushBackgroundChannel();

    @BindingMethod
    public static native void TablePopBackgroundChannel();

    // Widgets

    @BindingMethod
    public static native void TextEx(String beginText, @OptArg String endText, @OptArg @ArgValue(staticCast = "ImGuiTextFlags") int flags);

    @BindingMethod
    public static native boolean ButtonEx(String label, @OptArg(callValue = "ImVec2(0,0)") ImVec2 size, @OptArg @ArgValue(staticCast = "ImGuiButtonFlags") int flags);

    @BindingMethod
    public static native boolean ArrowButtonEx(String strId,
                                               @ArgValue(staticCast = "ImGuiDir") int dir,
                                               ImVec2 size,
                                               @OptArg @ArgValue(staticCast = "ImGuiButtonFlags") int flags);

    @BindingMethod
    public static native boolean ImageButtonEx(@ArgValue(callPrefix = "(ImGuiID)") int id,
                                               @ArgValue(callPrefix = "(ImTextureID)(uintptr_t)") long textureId,
                                               ImVec2 size,
                                               ImVec2 uv0,
                                               ImVec2 uv1,
                                               ImVec4 bgCol,
                                               ImVec4 tintCol,
                                               @ArgValue(staticCast = "ImGuiButtonFlags") int flags);

    @BindingMethod
    public static native void SeparatorEx(@ArgValue(staticCast = "ImGuiSeparatorFlags") int flags, @OptArg float thickness);

    @BindingMethod
    public static native void SeparatorTextEx(@ArgValue(callPrefix = "(ImGuiID)") int id, String label, String labelEnd, float extraWidth);

    // Widgets: Window Decorations

    @BindingMethod
    public static native boolean CloseButton(@ArgValue(callPrefix = "(ImGuiID)") int id, ImVec2 pos);

    @BindingMethod
    public static native boolean CollapseButton(@ArgValue(callPrefix = "(ImGuiID)") int id, ImVec2 pos, ImGuiDockNode dockNode);

    @BindingMethod
    public static native void Scrollbar(@ArgValue(staticCast = "ImGuiAxis") int axis);

    @BindingMethod
    public static native ImRect GetWindowScrollbarRect(ImGuiWindow imGuiWindow, @ArgValue(staticCast = "ImGuiAxis") int axis);

    @BindingMethod
    public static native int GetWindowScrollbarID(ImGuiWindow window, @ArgValue(staticCast = "ImGuiAxis") int axis);

    @BindingMethod
    public static native int GetWindowResizeCornerID(ImGuiWindow window, int n);

    @BindingMethod
    public static native int GetWindowResizeBorderID(ImGuiWindow window, @ArgValue(staticCast = "ImGuiDir") int dir);

    // Widgets low-level behaviors

    @BindingMethod
    public static native boolean ButtonBehavior(ImRect bb, @ArgValue(callPrefix = "(ImGuiID)") int id, ImBoolean outHovered, ImBoolean outHeld, @OptArg @ArgValue(staticCast = "ImGuiButtonFlags") int imGuiButtonFlags);

    @BindingMethod
    public static native boolean SplitterBehavior(ImRect bb, int id, @ArgValue(staticCast = "ImGuiAxis") int axis, ImFloat size1, ImFloat size2, float minSize1, float minSize2, @OptArg float hoverExtend, @OptArg float hoverVisibilityDelay, @OptArg int bgCol);

    // Shade functions (write over already created vertices)

    @BindingMethod
    public static native void ShadeVertsTransformPos(ImDrawList drawList, int vertStartIdx, int vertEndIdx, ImVec2 pivotIn, float cosA, float sinA, ImVec2 pivotOut);
}
