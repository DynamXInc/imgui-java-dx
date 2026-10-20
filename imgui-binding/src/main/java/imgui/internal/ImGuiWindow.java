package imgui.internal;

import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.binding.ImGuiStruct;
import imgui.binding.annotation.BindingField;
import imgui.binding.annotation.BindingSource;

@BindingSource
public class ImGuiWindow extends ImGuiStruct {
    public ImGuiWindow(final long ptr) {
        super(ptr);
        ImGui.init();
    }

    /*JNI
        #include "_common.h"
        #include "_internal.h"
        #define THIS ((ImGuiWindow*)STRUCT_PTR)
     */

    /**
     * Window identifier. An owned secondary viewport has the same identifier as its owner window.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public int ID;

    @BindingField(accessors = BindingField.Accessor.GETTER)
    public String Name;

    @BindingField(accessors = BindingField.Accessor.GETTER)
    public int Flags;

    /** Last frame in which this window was submitted, excluding inactive viewport retirement. */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public int LastFrameActive;

    /**
     * Current visible window size, reduced to the title bar when collapsed.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public ImVec2 Size;

    /**
     * Full window size, including when collapsed.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public ImVec2 SizeFull;

    @BindingField(accessors = BindingField.Accessor.GETTER)
    public boolean Collapsed;

    /**
     * Current viewport. Its native pointer can be zero for inactive windows.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public ImGuiViewport Viewport;

    /**
     * Dock node, including hidden nodes. Its native pointer is zero for undocked windows.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public ImGuiDockNode DockNode;

    /**
     * Dock node hosted by this window. Its native pointer is zero when not hosting a node.
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public ImGuiDockNode DockNodeAsHost;

    /**
     * Is scrollbar visible?
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public boolean ScrollbarX;

    /**
     * Is scrollbar visible?
     */
    @BindingField(accessors = BindingField.Accessor.GETTER)
    public boolean ScrollbarY;

    /*JNI
        #undef THIS
     */
}
