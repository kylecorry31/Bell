package com.kylecorry.bell.domain

import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import java.time.Instant
import kotlin.enums.EnumEntries

// Based on https://docs.oasis-open.org/emergency/cap/v1.2/CAP-v1.2-os.pdf
data class Alert(
    val id: Long,
    // Alert
    val identifier: String,
    val sender: String,
    val sent: Instant,
    val source: String,
    // Info
    val category: Category,
    val event: String,
    val urgency: Urgency,
    val severity: Severity,
    val certainty: Certainty,
    val responseType: ResponseType? = null,
    val effective: Instant? = null,
    val onset: Instant? = null,
    val expires: Instant? = null,
    val headline: String? = event,
    val description: String? = null,
    val instruction: String? = null,
    val link: String? = null,
    val area: Area? = null,
    val parameters: Map<String, String>? = null,
    // Additional info created by Bell
    val fullText: String? = null,
    val llmSummary: String? = null,
    val created: Instant = Instant.now(),
    val updated: Instant = Instant.now(),
    /**
     * Used to hide alerts that are not actively tracked
     */
    val isTracked: Boolean = true,
    val isDownloadRequired: Boolean = false,
    val redownloadIntervalDays: Long? = null,
) {
    fun isActive(time: Instant = Instant.now()): Boolean {
        return isValid(time) && (effective ?: Instant.MIN).isBefore(time)
    }

    fun isValid(time: Instant = Instant.now()): Boolean {
        return isTracked && (expires ?: Instant.MAX).isAfter(time)
    }

    fun shouldDownload(time: Instant = Instant.now()): Boolean {
        val shouldRedownload =
            redownloadIntervalDays != null && updated.plusSeconds(redownloadIntervalDays)
                .isBefore(time)

        return (isDownloadRequired || shouldRedownload) && isValid(time)
    }
}

data class Area(
    val states: List<String>,
    val areaDescription: String = states.joinToString(", "),
    val polygons: List<List<Coordinate>>? = null,
    val circles: List<Geofence>? = null
)

interface CAPEnum {
    val codes: List<String>
}

// Not used on the alert model
enum class MessageType(override val codes: List<String>) : CAPEnum {
    Alert(listOf("Alert")),
    Update(listOf("Update")),
    Cancel(listOf("Cancel")),
    Acknowledge(listOf("Ack")),
    Error(listOf("Error")),
}

enum class Category(override val codes: List<String>) : CAPEnum {
    /**
     * Natural hazards related to Earth's physical processes.
     * - Earthquake
     * - Tsunami
     * - Volcano
     */
    Geophysical(listOf("Geo")),

    /**
     * Weather-related hazards.
     * - Hurricane
     * - Tornado
     * - Flood
     * - Blizzard
     * - Heat
     */
    Meteorological(listOf("Met")),

    /**
     * Non-criminal public safety threats.
     * - Evacuation
     * - Building collapse
     * - Civil disturbances
     * - Dam failure
     */
    Safety(listOf("Safety")),

    /**
     * Criminal public safety threats.
     * - Shooting
     * - Bomb threat
     * - Cyber attack
     * - Terrorism
     */
    Security(listOf("Security")),

    /**
     * Operations involving search, rescue, and recovery efforts.
     * - Missing person search
     */
    Rescue(listOf("Rescue")),

    /**
     * Fire.
     * - Wildfire
     * - Building/house fire
     */
    Fire(listOf("Fire")),

    /**
     * Public health threats.
     * - Pandemics
     * - Foodborne illness outbreaks
     * - Medical facility overloads
     */
    Health(listOf("Health")),

    /**
     * Environment threats that don't fall under Geo or Met.
     * - Industrial chemical spills
     * - Oil spills
     * - Air quality hazards
     * - Algae blooms
     */
    Environmental(listOf("Env")),

    /**
     * Disruptions to transportation systems.
     * - Major traffic accidents
     * - Airplane crash
     * - Train derailment
     * - Transit disruptions
     */
    Transport(listOf("Transport")),

    /**
     * Disruptions to infrastructure, utilities, and essential services.
     * - Power outage
     * - Water supply disruption
     * - Telecommunications failure
     * - Gas leak
     * - Structural damage to critical infrastructure
     */
    Infrastructure(listOf("Infra")),

    /**
     * Chemical, Biological, Radiological, Nuclear, or High-Yield Explosive threats.
     * - Chemical weapon
     * - Bioterrorism
     * - Dirty bomb
     * - Nuclear accident
     * - Unexploded ordnance
     */
    CBRNE(listOf("CBRNE")), // Chemical, Biological, Radiological, Nuclear, or High-Yield Explosive

    /**
     * All other events.
     */
    Other(listOf("Other"))
}

enum class ResponseType(override val codes: List<String>) : CAPEnum {
    Shelter(listOf("Shelter")),
    Evacuate(listOf("Evacuate")),
    Prepare(listOf("Prepare")),
    Execute(listOf("Execute")),
    Avoid(listOf("Avoid")),
    Monitor(listOf("Monitor")),
    Assess(listOf("Assess")),
    AllClear(listOf("AllClear")),
    None(listOf("None")),
}

enum class Urgency(override val codes: List<String>) : CAPEnum {
    /**
     * Responsive action should be taken immediately
     */
    Immediate(listOf("Immediate")),

    /**
     * Responsive action should be taken soon (within next hour)
     */
    Expected(listOf("Expected")),

    /**
     * Responsive action should be taken in the near future
     */
    Future(listOf("Future")),

    /**
     * Responsive action is no longer required
     */
    Past(listOf("Past")),

    /**
     * Urgency unknown
     */
    Unknown(listOf("Unknown")),
}

enum class Severity(override val codes: List<String>) : CAPEnum {
    /**
     * Extraordinary threat to life or property
     */
    Extreme(listOf("Extreme")),

    /**
     * Significant threat to life or property
     */
    Severe(listOf("Severe")),

    /**
     * Possible threat to life or property
     */
    Moderate(listOf("Moderate")),

    /**
     * Minimal to no known threat to life or property
     */
    Minor(listOf("Minor")),

    /**
     * Severity unknown
     */
    Unknown(listOf("Unknown")),
}

enum class Certainty(override val codes: List<String>) : CAPEnum {
    /**
     * Determined to have occurred or to be ongoing
     */
    Observed(listOf("Observed")),

    /**
     * Likely (p > ~50%)
     */
    Likely(listOf("Likely", "Very Likely")),

    /**
     * Possible but not likely (p <= ~50%)
     */
    Possible(listOf("Possible")),

    /**
     * Not expected to occur (p ~ 0)
     */
    Unlikely(listOf("Unlikely")),

    /**
     * Certainty unknown
     */
    Unknown(listOf("Unknown")),
}

fun <T> EnumEntries<T>.getByCode(code: String): T? where T : CAPEnum, T : Enum<T> {
    return this.firstOrNull { it.codes.any { it.equals(code, true) } }
}