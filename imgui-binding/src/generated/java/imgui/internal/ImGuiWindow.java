package imgui.internal;

import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.binding.ImGuiStruct;




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
    public int getID() {
        return nGetID();
    }

    private native int nGetID(); /*
        return THIS->ID;
    */

    public String getName() {
        return nGetName();
    }

    private native String nGetName(); /*
        return env->NewStringUTF(THIS->Name);
    */

    public int getFlags() {
        return nGetFlags();
    }

    private native int nGetFlags(); /*
        return THIS->Flags;
    */

     /**
     * Last frame in which this window was submitted, excluding inactive viewport retirement.
     */
    public int getLastFrameActive() {
        return nGetLastFrameActive();
    }

    private native int nGetLastFrameActive(); /*
        return THIS->LastFrameActive;
    */

     /**
     * Current visible window size, reduced to the title bar when collapsed.
     */
    public ImVec2 getSize() {
        final ImVec2 dst = new ImVec2();
        nGetSize(dst);
        return dst;
    }

    /**
     * Current visible window size, reduced to the title bar when collapsed.
     */
    public float getSizeX() {
        return nGetSizeX();
    }

    /**
     * Current visible window size, reduced to the title bar when collapsed.
     */
    public float getSizeY() {
        return nGetSizeY();
    }

    /**
     * Current visible window size, reduced to the title bar when collapsed.
     */
    public void getSize(final ImVec2 dst) {
        nGetSize(dst);
    }

    private native void nGetSize(ImVec2 dst); /*
        Jni::ImVec2Cpy(env, THIS->Size, dst);
    */

    private native float nGetSizeX(); /*
        return THIS->Size.x;
    */

    private native float nGetSizeY(); /*
        return THIS->Size.y;
    */

     /**
     * Full window size, including when collapsed.
     */
    public ImVec2 getSizeFull() {
        final ImVec2 dst = new ImVec2();
        nGetSizeFull(dst);
        return dst;
    }

    /**
     * Full window size, including when collapsed.
     */
    public float getSizeFullX() {
        return nGetSizeFullX();
    }

    /**
     * Full window size, including when collapsed.
     */
    public float getSizeFullY() {
        return nGetSizeFullY();
    }

    /**
     * Full window size, including when collapsed.
     */
    public void getSizeFull(final ImVec2 dst) {
        nGetSizeFull(dst);
    }

    private native void nGetSizeFull(ImVec2 dst); /*
        Jni::ImVec2Cpy(env, THIS->SizeFull, dst);
    */

    private native float nGetSizeFullX(); /*
        return THIS->SizeFull.x;
    */

    private native float nGetSizeFullY(); /*
        return THIS->SizeFull.y;
    */

    public boolean getCollapsed() {
        return nGetCollapsed();
    }

    private native boolean nGetCollapsed(); /*
        return THIS->Collapsed;
    */

     /**
     * Current viewport. Its native pointer can be zero for inactive windows.
     */
    public ImGuiViewport getViewport() {
        return new ImGuiViewport(nGetViewport());
    }

    private native long nGetViewport(); /*
        return (uintptr_t)THIS->Viewport;
    */

     /**
     * Dock node, including hidden nodes. Its native pointer is zero for undocked windows.
     */
    public ImGuiDockNode getDockNode() {
        return new ImGuiDockNode(nGetDockNode());
    }

    private native long nGetDockNode(); /*
        return (uintptr_t)THIS->DockNode;
    */

     /**
     * Dock node hosted by this window. Its native pointer is zero when not hosting a node.
     */
    public ImGuiDockNode getDockNodeAsHost() {
        return new ImGuiDockNode(nGetDockNodeAsHost());
    }

    private native long nGetDockNodeAsHost(); /*
        return (uintptr_t)THIS->DockNodeAsHost;
    */

     /**
     * Is scrollbar visible?
     */
    public boolean getScrollbarX() {
        return nGetScrollbarX();
    }

    private native boolean nGetScrollbarX(); /*
        return THIS->ScrollbarX;
    */

     /**
     * Is scrollbar visible?
     */
    public boolean getScrollbarY() {
        return nGetScrollbarY();
    }

    private native boolean nGetScrollbarY(); /*
        return THIS->ScrollbarY;
    */

    /*JNI
        #undef THIS
     */
}
