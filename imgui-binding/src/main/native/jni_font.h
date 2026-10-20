#pragma once

#include "imgui_internal.h"
#include "jni.h"

namespace Jni
{
    // JNI elements are borrowed only for this call. Never retain them in a font source.
    struct FontInput
    {
        JNIEnv* Env;
        jshortArray RangeArray;
        jshort* Ranges;
        jbyteArray ByteArray;
        jbyte* Bytes;
        jstring String;
        const char* Text;

        FontInput(JNIEnv* env, jshortArray ranges, jbyteArray bytes = NULL, jstring text = NULL)
            : Env(env), RangeArray(ranges), Ranges(NULL), ByteArray(bytes), Bytes(NULL), String(text), Text(NULL)
        {
            if (ranges != NULL)
                Ranges = env->GetShortArrayElements(ranges, NULL);
            if (!env->ExceptionCheck() && bytes != NULL)
                Bytes = env->GetByteArrayElements(bytes, NULL);
            if (!env->ExceptionCheck() && text != NULL)
                Text = env->GetStringUTFChars(text, NULL);
        }

        ~FontInput()
        {
            if (Text != NULL) Env->ReleaseStringUTFChars(String, Text);
            if (Bytes != NULL) Env->ReleaseByteArrayElements(ByteArray, Bytes, JNI_ABORT);
            if (Ranges != NULL) Env->ReleaseShortArrayElements(RangeArray, Ranges, JNI_ABORT);
        }

        FontInput(const FontInput&) = delete;
        FontInput& operator=(const FontInput&) = delete;
    };

    // Templates never transfer their storage. Each registration owns its own source data.
    inline bool PrepareFontConfig(JNIEnv* env, ImFontAtlas* atlas, const ImFontConfig* source, ImFontConfig& config)
    {
        if (atlas == NULL || atlas->Locked)
        {
            env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Font atlas is destroyed or locked");
            return false;
        }
        if (source != NULL)
            config = *source;
        if (config.MergeMode && (atlas->Fonts.Size == 0 || (config.DstFont != NULL && !atlas->Fonts.contains(config.DstFont))))
        {
            env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Merge destination must belong to this atlas");
            return false;
        }
        if (!config.MergeMode && config.DstFont != NULL)
        {
            env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "A destination font requires merge mode");
            return false;
        }
        config.FontData = NULL;
        config.FontDataSize = 0;
        config.FontDataOwnedByAtlas = true;
        return true;
    }

    inline void* CopyFontData(JNIEnv* env, const void* data, int size)
    {
        void* copy = IM_ALLOC(size);
        if (copy == NULL)
        {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Cannot allocate font data");
            return NULL;
        }
        memcpy(copy, data, size);
        return copy;
    }

    inline void* CopyJavaFontData(JNIEnv* env, jbyteArray data, int size)
    {
        void* copy = IM_ALLOC(size);
        if (copy == NULL)
        {
            env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "Cannot allocate font data");
            return NULL;
        }
        env->GetByteArrayRegion(data, 0, size, (jbyte*)copy);
        if (env->ExceptionCheck())
        {
            IM_FREE(copy);
            return NULL;
        }
        return copy;
    }
}
