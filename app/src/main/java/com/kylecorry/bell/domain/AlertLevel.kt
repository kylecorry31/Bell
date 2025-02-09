package com.kylecorry.bell.domain

enum class AlertLevel {
    Watch, // May occur
    Warning, // Will occur or is occurring
    Advisory, // Be aware, may not be severe
    Update, // Provides new information about an existing alert
    Event, // An event has already occurred
    Announcement, // An announcement
    Other, // Any other type of alert
    Noise // Not something to care about, will be hidden in the UI (added to the DB to prevent re-fetching)
}