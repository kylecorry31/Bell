package com.kylecorry.bell.infrastructure.utils

// TODO: Add US territories
object StateUtils {

    private val stateCodeMap = mapOf(
        "Alabama" to "AL",
        "Alaska" to "AK",
        "Arizona" to "AZ",
        "Arkansas" to "AR",
        "California" to "CA",
        "Colorado" to "CO",
        "Connecticut" to "CT",
        "Delaware" to "DE",
        "Florida" to "FL",
        "Georgia" to "GA",
        "Hawaii" to "HI",
        "Idaho" to "ID",
        "Illinois" to "IL",
        "Indiana" to "IN",
        "Iowa" to "IA",
        "Kansas" to "KS",
        "Kentucky" to "KY",
        "Louisiana" to "LA",
        "Maine" to "ME",
        "Maryland" to "MD",
        "Massachusetts" to "MA",
        "Michigan" to "MI",
        "Minnesota" to "MN",
        "Mississippi" to "MS",
        "Missouri" to "MO",
        "Montana" to "MT",
        "Nebraska" to "NE",
        "Nevada" to "NV",
        "New Hampshire" to "NH",
        "New Jersey" to "NJ",
        "New Mexico" to "NM",
        "New York" to "NY",
        "North Carolina" to "NC",
        "North Dakota" to "ND",
        "Ohio" to "OH",
        "Oklahoma" to "OK",
        "Oregon" to "OR",
        "Pennsylvania" to "PA",
        "Rhode Island" to "RI",
        "South Carolina" to "SC",
        "South Dakota" to "SD",
        "Tennessee" to "TN",
        "Texas" to "TX",
        "Utah" to "UT",
        "Vermont" to "VT",
        "Virginia" to "VA",
        "Washington" to "WA",
        "West Virginia" to "WV",
        "Wisconsin" to "WI",
        "Wyoming" to "WY"
    ).mapKeys { it.key.uppercase() }

    // TODO: Verify this
    private val stateBorders = mapOf(
        "AL" to setOf("FL", "GA", "MS", "TN"),
        "AK" to emptySet(), // Alaska has no bordering states
        "AZ" to setOf("CA", "NV", "UT", "CO", "NM"),
        "AR" to setOf("MO", "TN", "MS", "LA", "TX", "OK"),
        "CA" to setOf("OR", "NV", "AZ"),
        "CO" to setOf("WY", "NE", "KS", "OK", "NM", "AZ", "UT"),
        "CT" to setOf("NY", "MA", "RI"),
        "DE" to setOf("MD", "PA", "NJ"),
        "FL" to setOf("GA", "AL"),
        "GA" to setOf("FL", "AL", "TN", "NC", "SC"),
        "HI" to emptySet(), // Hawaii has no bordering states
        "ID" to setOf("MT", "WY", "UT", "NV", "OR", "WA"),
        "IL" to setOf("WI", "IA", "MO", "KY", "IN"),
        "IN" to setOf("IL", "KY", "OH", "MI"),
        "IA" to setOf("MN", "WI", "IL", "MO", "NE", "SD"),
        "KS" to setOf("NE", "MO", "OK", "CO"),
        "KY" to setOf("IL", "IN", "OH", "WV", "VA", "TN", "MO"),
        "LA" to setOf("TX", "AR", "MS"),
        "ME" to setOf("NH"),
        "MD" to setOf("PA", "DE", "VA", "WV"),
        "MA" to setOf("NH", "VT", "NY", "CT", "RI"),
        "MI" to setOf("OH", "IN", "WI"),
        "MN" to setOf("ND", "SD", "IA", "WI"),
        "MS" to setOf("LA", "AR", "TN", "AL"),
        "MO" to setOf("IA", "NE", "KS", "OK", "AR", "TN", "KY", "IL"),
        "MT" to setOf("ND", "SD", "WY", "ID"),
        "NE" to setOf("SD", "IA", "MO", "KS", "CO", "WY"),
        "NV" to setOf("OR", "ID", "UT", "AZ", "CA"),
        "NH" to setOf("ME", "MA", "VT"),
        "NJ" to setOf("NY", "PA", "DE"),
        "NM" to setOf("CO", "OK", "TX", "AZ"),
        "NY" to setOf("VT", "MA", "CT", "NJ", "PA"),
        "NC" to setOf("VA", "SC", "GA", "TN"),
        "ND" to setOf("MN", "SD", "MT"),
        "OH" to setOf("MI", "IN", "KY", "WV", "PA"),
        "OK" to setOf("KS", "MO", "AR", "TX", "NM", "CO"),
        "OR" to setOf("WA", "ID", "NV", "CA"),
        "PA" to setOf("NY", "NJ", "DE", "MD", "WV", "OH"),
        "RI" to setOf("CT", "MA"),
        "SC" to setOf("NC", "GA"),
        "SD" to setOf("ND", "MN", "IA", "NE", "WY", "MT"),
        "TN" to setOf("KY", "VA", "NC", "GA", "AL", "MS", "AR", "MO"),
        "TX" to setOf("NM", "OK", "AR", "LA"),
        "UT" to setOf("ID", "WY", "CO", "NM", "AZ", "NV"),
        "VT" to setOf("NY", "NH", "MA"),
        "VA" to setOf("MD", "NC", "TN", "KY", "WV"),
        "WA" to setOf("ID", "OR"),
        "WV" to setOf("OH", "PA", "MD", "VA", "KY"),
        "WI" to setOf("MN", "IA", "IL", "MI"),
        "WY" to setOf("MT", "SD", "NE", "CO", "UT", "ID")
    )


    fun isSelectedState(
        selectedState: String,
        state: String,
        includeBorderingStates: Boolean = false
    ): Boolean {
        val selectedStates = selectedState.split(",").map { it.trim() }.toMutableSet()

        if (includeBorderingStates) {
            val borderingStates =
                selectedStates.flatMap { stateCode -> stateBorders[stateCode] ?: emptySet() }
            selectedStates.addAll(borderingStates)
        }

        // If there are no selected states, or the selected states contains a US entry, then all states are selected
        if (selectedStates.isEmpty() || selectedStates.contains("US")) {
            return true
        }

        val states = state.split(",").map { it.trim().uppercase() }.map { stateCodeMap[it] ?: it }

        return states.any { selectedStates.contains(it) }
    }

}