package com.kylecorry.bell.domain

enum class AlertLevel(val importance: Int) {
    High(4), // High importance
    Medium(3), // Medium importance
    Low(2), // Low importance
    Information(1), // Informational only
    Ignored(0) // Not something to care about, will be hidden in the UI (added to the DB to prevent re-fetching)
}