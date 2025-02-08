package com.kylecorry.preparedness_feed.ui

import com.kylecorry.andromeda.views.reactivity.VDOMNode
import com.kylecorry.preparedness_feed.ui.components.Alerts

class MainFragment : ReactiveFragment() {
    override fun render(): VDOMNode<*, *> {
        return Alerts()
    }
}