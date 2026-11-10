package com.immersiveweather.app.data

import com.immersiveweather.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class WeatherRepository {
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "ImmersiveWeather/1.1 (Android)")
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) error("Weather service returned HTTP $code")
            body
        } finally { connection.disconnect() }
    }

    suspend fun searchCities(name: String, language: String): Result<List<City>> = runCatching {
        val q = URLEncoder.encode(name.trim(), "UTF-8")
        val json = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=10&language=$language&format=json"))
        val results = json.optJSONArray("results") ?: return@runCatching emptyList()
        (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            City(item.optString("name"), item.optString("country"), item.getDouble("latitude"), item.getDouble("longitude"))
        }
    }

    suspend fun load(city: City): Result<WeatherBundle> = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,dew_point_2m,uv_index" +
            "&hourly=temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m,wind_direction_10m,relative_humidity_2m,visibility" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max" +
            "&forecast_days=7&timezone=auto&wind_speed_unit=kmh"
        val root = JSONObject(get(url))
        val c = root.getJSONObject("current")
        val h = root.getJSONObject("hourly")
        val d = root.getJSONObject("daily")
        val current = CurrentWeather(
            c.optDouble("temperature_2m"), c.optDouble("apparent_temperature"), c.optInt("relative_humidity_2m"),
            c.optDouble("wind_speed_10m"), c.optInt("wind_direction_10m"), c.optDouble("wind_gusts_10m"),
            c.optDouble("pressure_msl"), c.optDouble("visibility"), c.optDouble("dew_point_2m"),
            c.optDouble("uv_index"), c.optInt("cloud_cover"), c.optDouble("precipitation"),
            c.optInt("weather_code"), c.optInt("is_day") == 1, c.optString("time")
        )
        val hours = (0 until minOf(24, h.getJSONArray("time").length())).map { i ->
            HourForecast(h.getJSONArray("time").getString(i), h.getJSONArray("temperature_2m").optDouble(i),
                h.getJSONArray("apparent_temperature").optDouble(i), h.getJSONArray("weather_code").optInt(i),
                h.getJSONArray("precipitation_probability").optInt(i), h.getJSONArray("wind_speed_10m").optDouble(i),
                h.getJSONArray("wind_direction_10m").optInt(i), h.getJSONArray("relative_humidity_2m").optInt(i),
                h.getJSONArray("visibility").optDouble(i))
        }
        val days = (0 until d.getJSONArray("time").length()).map { i ->
            DayForecast(d.getJSONArray("time").getString(i), d.getJSONArray("temperature_2m_min").optDouble(i),
                d.getJSONArray("temperature_2m_max").optDouble(i), d.getJSONArray("weather_code").optInt(i),
                d.getJSONArray("precipitation_probability_max").optInt(i), d.getJSONArray("sunrise").getString(i),
                d.getJSONArray("sunset").getString(i), d.getJSONArray("uv_index_max").optDouble(i))
        }
        val air = runCatching {
            val aq = JSONObject(get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${city.latitude}&longitude=${city.longitude}&current=us_aqi,european_aqi,pm2_5,pm10&timezone=auto"))
                .getJSONObject("current")
            AirQuality(aq.optIntOrNull("us_aqi"), aq.optIntOrNull("european_aqi"), aq.optDoubleOrNull("pm2_5"), aq.optDoubleOrNull("pm10"))
        }.getOrNull()
        WeatherBundle(city.name, city.country, city.latitude, city.longitude, root.optString("timezone"), current, hours, days, air)
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else optInt(key)
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)
