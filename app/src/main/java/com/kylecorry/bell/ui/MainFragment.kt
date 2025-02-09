package com.kylecorry.bell.ui

import com.kylecorry.andromeda.views.reactivity.VDOMNode
import com.kylecorry.bell.ui.components.Alerts

class MainFragment : ReactiveFragment() {
    override fun render(): VDOMNode<*, *> {
        return Alerts()
    }
}