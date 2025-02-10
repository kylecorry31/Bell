package com.kylecorry.bell.domain

enum class AlertType(val importance: Int) {
    Weather(1),
    Government(3),
    Earthquake(1),
    Water(1),
    SpaceWeather(2),
    Health(2),
    Volcano(1),
    Fire(1),
    Travel(3),
    Economy(3),
    Other(3)
}