package com.kylecorry.preparedness_feed.ui.components

import android.view.View.OnClickListener
import android.view.ViewGroup
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Button
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.ViewAttributes

class UpdateButtonAttributes : ViewAttributes() {
    var onUpdate: (() -> Unit)? = null
}

fun UpdateButton(config: UpdateButtonAttributes.() -> Unit) = Component(config) { attrs ->
    val onClickListener = useMemo(attrs.onUpdate) {
        OnClickListener {
            attrs.onUpdate?.invoke()
        }
    }

    Button {
        text = "Update"
        onClick = onClickListener
        width = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}