package com.kylecorry.bell.domain

enum class SourceSystem(private val department: String, private val source: String) {
    USGSEarthquake("USGS", "Earthquake"),
    BLSSummary("BLS", "Summary"),
    EIAGasolineDieselPrices("EIA", "Gasoline and Diesel Prices"),
    EIAHeatingOilPrices("EIA", "Heating and Oil Prices"),
    InciwebWildfires("Inciweb", "Wildfires"),
    CongressBills("Congress", "Bills"),
    WhiteHousePresidentialActions("White House", "Presidential Actions"),
    CDCHealthAlertNetwork("CDC", "Health Alert Network"),
    CDCUSOutbreaks("CDC", "US Outbreaks"),
    SWPCSpaceWeather("SWPC", "Space Weather"),
    NWSWeather("NWS", "Weather"),
    StateTravelAdvisories("State", "Travel Advisories"),
    USGSVolcano("USGS", "Volcano"),
    NOAATsunami("NOAA", "Tsunami"),
    USGSWater("USGS", "Water"),
    IC3InternetCrime("IC3", "Internet Crime"),
    Other("Other", "Other");
}