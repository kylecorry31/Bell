package com.kylecorry.preparedness_feed.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.views.reactivity.VDOM
import com.kylecorry.andromeda.views.reactivity.VDOMNode
import com.kylecorry.preparedness_feed.databinding.FragmentMainBinding
import com.kylecorry.preparedness_feed.ui.components.Alerts

class MainFragment : BoundFragment<FragmentMainBinding>() {

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(layoutInflater, container, false)
    }

    override fun onUpdate() {
        super.onUpdate()
        render(Alerts {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT;
        })
    }

    private fun render(node: VDOMNode<*, *>) {
        VDOM.render(binding.root, node, binding.root.children.firstOrNull())
    }
}