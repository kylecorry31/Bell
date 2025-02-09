package com.kylecorry.bell.infrastructure.errors

import android.content.Context
import com.kylecorry.andromeda.exceptions.IBugReportGenerator
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.bell.ui.MainActivity

class FragmentDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val fragment = if (context is AndromedaActivity) {
            context.getFragment()
        } else {
            null
        }
        return "Fragment: ${fragment?.javaClass?.simpleName ?: MainActivity.lastKnownFragment ?: "Unknown"}"
    }
}