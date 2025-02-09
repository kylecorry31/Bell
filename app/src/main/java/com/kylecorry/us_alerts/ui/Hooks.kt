package com.kylecorry.bell.ui

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import com.kylecorry.andromeda.core.ui.ReactiveComponent

object Hooks {

    fun ReactiveComponent.useIcon(resourceId: Int): Icon {
        val context = useAndroidContext()
        return useMemo(resourceId, context) {
            Icon.createWithResource(context, resourceId)
        }
    }

    fun ReactiveComponent.useIcon(bitmap: Bitmap): Icon {
        val context = useAndroidContext()
        return useMemo(bitmap, context) {
            Icon.createWithBitmap(bitmap)
        }
    }

    fun ReactiveComponent.useIcon(path: String): Icon {
        val context = useAndroidContext()
        return useMemo(path, context) {
            if (path.contains("://")) {
                Icon.createWithContentUri(Uri.parse(path))
            } else {
                Icon.createWithFilePath(path)
            }
        }
    }

    fun ReactiveComponent.useIcon(uri: Uri): Icon {
        val context = useAndroidContext()
        return useMemo(uri, context) {
            Icon.createWithContentUri(uri)
        }
    }

}