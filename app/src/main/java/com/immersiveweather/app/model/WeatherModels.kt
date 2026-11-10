package com.immersiveweather.app.model

data class CurrentWeather(
    val temperature: Double, val feelsLike: Double, val humidity: Int,
    val windSpeed: Double, val windDirection: Int, val windGusts: Double,
    val pressure: Double, val visibility: Double, val dewPoint: Double,
    val uv: Double, val cloudCover: Int, val precipitation: Double,
    val weatherCode: Int, val isDay: Boolean, val time: String
)
data class HourForecast(
    val time: String, val temperature: Double, val feelsLike: Double,
    val code: Int, val precipitation: Int, val wind: Double,
    val windDirection: Int, val humidity: Int, val visibility: Double
)
data class DayForecast(
    val date: String, val min: Double, val max: Double, val code: Int,
    val precipitation: Int, val sunrise: String, val sunset: String, val uvMax: Double
)
data class AirQuality(val usAqi: Int?, val europeanAqi: Int?, val pm25: Double?, val pm10: Double?)
data class City(val name: String, val country: String, val latitude: Double, val longitude: Double)
data class WeatherBundle(
    val city: String, val country: String, val latitude: Double, val longitude: Double,
    val timezone: String, val current: CurrentWeather, val hours: List<HourForecast>,
    val days: List<DayForecast>, val airQuality: AirQuality? = null
)
