package com.kylecorry.preparedness_feed.ui

import android.content.Context
import com.kylecorry.andromeda.exceptions.AggregateBugReportGenerator
import com.kylecorry.andromeda.exceptions.AndroidDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.AppDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.BaseExceptionHandler
import com.kylecorry.andromeda.exceptions.DeviceDetailsBugReportGenerator
import com.kylecorry.andromeda.exceptions.StackTraceBugReportGenerator
import com.kylecorry.preparedness_feed.R
import com.kylecorry.preparedness_feed.infrastructure.errors.FragmentDetailsBugReportGenerator

class CustomExceptionHandler(
    context: Context
) : BaseExceptionHandler(
    context.applicationContext,
    AggregateBugReportGenerator(
        listOf(
            AppDetailsBugReportGenerator(context.getString(R.string.app_name)),
            AndroidDetailsBugReportGenerator(),
            DeviceDetailsBugReportGenerator(),
            FragmentDetailsBugReportGenerator(),
            StackTraceBugReportGenerator()
        )
    ),
    "errors/error.txt",
    shouldRestartApp = false,
    shouldWrapSystemExceptionHandler = true
) {

    override fun handleBugReport(log: String) {
        error = log
    }

    companion object {
        var error: String? = null
    }
}