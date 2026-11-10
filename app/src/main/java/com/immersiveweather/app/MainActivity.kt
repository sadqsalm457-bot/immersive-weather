package com.immersiveweather.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.immersiveweather.app.data.WeatherRepository
import com.immersiveweather.app.model.City
import com.immersiveweather.app.model.WeatherBundle
import com.immersiveweather.app.ui.WeatherApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository = WeatherRepository()
    private var permissionResult by mutableStateOf(false)
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResult = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            var data by remember { mutableStateOf<WeatherBundle?>(null) }
            var loading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }
            var currentCity by remember { mutableStateOf(City("البصرة", "العراق", 30.5085, 47.7804)) }
            var savedCities by remember { mutableStateOf(loadSavedCities()) }
            val preferences = getSharedPreferences("immersive_weather", Context.MODE_PRIVATE)
            var language by remember { mutableStateOf(preferences.getString("language", "ar") ?: "ar") }
            var celsius by remember { mutableStateOf(preferences.getBoolean("celsius", true)) }
            var kmh by remember { mutableStateOf(preferences.getBoolean("kmh", true)) }
            var westernNumerals by remember { mutableStateOf(preferences.getBoolean("western", false)) }
            var animations by remember { mutableStateOf(preferences.getBoolean("animations", true)) }
            var lowPower by remember { mutableStateOf(preferences.getBoolean("low_power", false)) }
            var sound by remember { mutableStateOf(preferences.getBoolean("sound", false)) }
            var theme by remember { mutableStateOf(preferences.getString("theme", "dark") ?: "dark") }
            val scope = rememberCoroutineScope()

            fun persist() {
                preferences.edit().putString("language", language).putBoolean("celsius", celsius).putBoolean("kmh", kmh)
                    .putBoolean("western", westernNumerals).putBoolean("animations", animations).putBoolean("low_power", lowPower)
                    .putBoolean("sound", sound).putString("theme", theme).apply()
            }
            fun load(city: City) {
                currentCity = city
                scope.launch {
                    loading = true
                    repository.load(city).onSuccess { data = it; error = null }
                        .onFailure { error = if (language == "ar") "تعذّر تحديث بيانات الطقس. تحقق من اتصال الإنترنت." else "Unable to update weather. Check your internet connection." }
                    loading = false
                }
            }
            fun search(query: String) {
                scope.launch {
                    loading = true
                    repository.searchCities(query, language).onSuccess { results ->
                        if (results.isNotEmpty()) load(results.first())
                        else { error = if (language == "ar") "لم نعثر على المدينة. جرّب اسماً آخر." else "City not found. Try another name."; loading = false }
                    }.onFailure { error = if (language == "ar") "تعذّر البحث عن المدينة." else "City search failed."; loading = false }
                }
            }
            fun saveCity(city: City) {
                if (savedCities.none { it.latitude == city.latitude && it.longitude == city.longitude }) {
                    savedCities = savedCities + city
                    saveCities(savedCities)
                }
            }
            fun removeCity(city: City) { savedCities = savedCities.filterNot { it.latitude == city.latitude && it.longitude == city.longitude }; saveCities(savedCities) }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val location = lastLocation()
                    if (location != null) load(City("موقعي الحالي", "", location.latitude, location.longitude)) else load(currentCity)
                } else {
                    locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    load(currentCity)
                }
            }
            LaunchedEffect(permissionResult) {
                if (permissionResult) {
                    val location = lastLocation()
                    if (location != null) load(City(if (language == "ar") "موقعي الحالي" else "My location", "", location.latitude, location.longitude))
                }
            }
            WeatherApp(
                data = data, loading = loading, error = error, language = language,
                celsius = celsius, kmh = kmh, westernNumerals = westernNumerals,
                animations = animations, lowPower = lowPower, sound = sound, theme = theme,
                savedCities = savedCities, currentCity = currentCity,
                onRefresh = { load(currentCity) }, onSearch = { search(it) }, onSelectCity = { load(it) },
                onSaveCity = { saveCity(it) }, onRemoveCity = { removeCity(it) },
                onLanguage = { language = it; persist(); data?.let { load(currentCity) } },
                onCelsius = { celsius = it; persist() }, onKmh = { kmh = it; persist() },
                onWestern = { westernNumerals = it; persist() }, onAnimations = { animations = it; persist() },
                onLowPower = { lowPower = it; persist() }, onSound = { sound = it; persist() },
                onTheme = { theme = it; persist() }
            )
        }
    }

    private fun lastLocation(): android.location.Location? {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        return try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = manager.getProviders(true)
            providers.mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (_: Exception) { null }
    }

    private fun loadSavedCities(): List<City> {
        val prefs = getSharedPreferences("immersive_weather", Context.MODE_PRIVATE)
        val raw = prefs.getString("saved_cities", "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size != 4) null else runCatching { City(p[0], p[1], p[2].toDouble(), p[3].toDouble()) }.getOrNull()
        }
    }
    private fun saveCities(cities: List<City>) {
        val raw = cities.joinToString("\n") { "${it.name.replace("|", " ")}|${it.country.replace("|", " ")}|${it.latitude}|${it.longitude}" }
        getSharedPreferences("immersive_weather", Context.MODE_PRIVATE).edit().putString("saved_cities", raw).apply()
    }
}
