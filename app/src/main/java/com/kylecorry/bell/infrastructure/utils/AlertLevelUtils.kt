package com.kylecorry.bell.infrastructure.utils

import com.kylecorry.bell.domain.AlertLevel

object AlertLevelUtils {

    private const val extreme = "extreme"
    private const val severe = "severe"
    private const val moderate = "moderate"
    private const val minor = "minor"

    private const val watch = "watch"
    private const val warning = "warning"

    fun getLevel(severity: String?, messageType: String?): AlertLevel {
        val lowerSeverity = severity?.lowercase()
        val lowerMessageType = messageType?.lowercase()
        return when {
            lowerSeverity == extreme || lowerSeverity == severe -> AlertLevel.High
            lowerSeverity == moderate -> AlertLevel.Medium
            lowerSeverity == minor -> AlertLevel.Low
            lowerMessageType == watch -> AlertLevel.Medium
            lowerMessageType == warning -> AlertLevel.High
            else -> AlertLevel.Low
        }
    }

}