package com.immersiveweather.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.lazy.items
import com.immersiveweather.app.model.*
import kotlin.math.sin

private enum class Page { HOME, HOURLY, WEEK, RADAR, CITIES, SETTINGS }

private fun words(ar: String, en: String, lang: String) =
    if (lang == "ar") ar else en

fun weatherLabel(code: Int, lang: String = "ar"): String = when (code) {
    0 -> words("سماء صافية", "Clear sky", lang)
    1 -> words("غالباً صافٍ", "Mainly clear", lang)
    2 -> words("غائم جزئياً", "Partly cloudy", lang)
    3 -> words("غائم", "Overcast", lang)
    in 45..48 -> words("ضباب", "Fog", lang)
    in 51..57 -> words("رذاذ", "Drizzle", lang)
    in 61..67 -> words("أمطار", "Rain", lang)
    in 71..77 -> words("ثلوج", "Snow", lang)
    in 80..82 -> words("زخات مطر", "Rain showers", lang)
    in 85..86 -> words("زخات ثلج", "Snow showers", lang)
    in 95..99 -> words("عواصف رعدية", "Thunderstorm", lang)
    else -> words("حالة جوية متغيرة", "Changing conditions", lang)
}

private fun weatherGlyph(code: Int) = when (code) {
    0 -> "☀"
    1, 2 -> "🌤"
    3 -> "☁"
    in 45..48 -> "🌫"
    in 51..67, in 80..82 -> "🌧"
    in 71..77, in 85..86 -> "❄"
    in 95..99 -> "⛈"
    else -> "◌"
}

private fun fmt(value: Number, western: Boolean): String {
    val s = value.toString()
    if (western) return s
    val digits = "٠١٢٣٤٥٦٧٨٩"
    return s.map {
        if (it in '0'..'9') digits[it - '0'] else it
    }.joinToString("")
}

private fun temperature(
    value: Double,
    celsius: Boolean,
    western: Boolean
): String {
    val v = if (celsius) value else value * 9 / 5 + 32
    return "${fmt(v.toInt(), western)}°"
}

@Composable
fun WeatherAtmosphere(
    code: Int,
    isDay: Boolean,
    animations: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
    temperatureC: Double? = null
) {
    val transition = rememberInfiniteTransition(label = "sky")
    val drift by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(
            tween(
                if (lowPower) 18000 else 9500,
                easing = LinearEasing
            ),
            RepeatMode.Restart
        ),
        label = "drift"
    )

    val rainy = code in 51..67 || code in 80..82 || code in 95..99
    val cloudy = code in 1..3 || code in 45..48 || rainy
    val cold = temperatureC != null && temperatureC < 8.0
    val hot = temperatureC != null && temperatureC > 34.0

    val top = if (!isDay) Color(0xFF10182D) else when {
        rainy -> Color(0xFF293B52)
        cold -> Color(0xFF315A78)
        hot -> Color(0xFF8B573F)
        code in 1..3 -> Color(0xFF456B88)
        else -> Color(0xFF17638F)
    }

    val bottom = if (!isDay) Color(0xFF293D59) else when {
        rainy -> Color(0xFF68798A)
        cold -> Color(0xFF8AB6CB)
        hot -> Color(0xFFE9A15E)
        code in 1..3 -> Color(0xFF9EB9C8)
        else -> Color(0xFFF2B56D)
    }

    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(top, bottom), 0f, size.height))

        if (isDay && !rainy) {
            drawCircle(
                Color(0xFFFFE6A0).copy(alpha = .65f),
                size.width * .105f,
                Offset(size.width * .78f, size.height * .19f)
            )
            drawCircle(
                Color(0xFFFFD17A).copy(alpha = .16f),
                size.width * .21f,
                Offset(size.width * .78f, size.height * .19f)
            )
        }

        if (cloudy) repeat(if (lowPower) 3 else 6) { i ->
            val dx = if (animations) drift * size.width * .22f else 0f
            val x = ((i * size.width / 4.2f + dx) % (size.width + 220f)) - 100f
            val y = size.height * (.16f + (i % 3) * .115f)
            val alpha = if (code == 3) .23f else .15f

            drawCircle(Color.White.copy(alpha = alpha), size.width * .135f, Offset(x, y))
            drawCircle(Color.White.copy(alpha = alpha), size.width * .105f, Offset(x + 45f, y - 13f))
        }

        if (animations && rainy) repeat(if (lowPower) 25 else 90) { i ->
            val x = (i * 73f + drift * 70f) % size.width
            val y = (i * 47f + drift * size.height * 1.7f) % size.height
            drawLine(
                Color.White.copy(alpha = .30f),
                Offset(x, y),
                Offset(x - 6f, y + 20f),
                1.4f
            )
        }

        if (animations && (code in 71..77 || code in 85..86)) repeat(
            if (lowPower) 20 else 55
        ) { i ->
            val x = (i * 83f + sin(drift * 6.28f + i) * 24f) % size.width
            val y = (i * 59f + drift * size.height) % size.height
            drawCircle(Color.White.copy(alpha = .75f), 2.2f + (i % 3), Offset(x, y))
        }

        if (animations && code in 95..99 && sin(drift * 18.84f) > .96f) {
            val bolt = Path().apply {
                moveTo(size.width * .72f, size.height * .13f)
                lineTo(size.width * .65f, size.height * .27f)
                lineTo(size.width * .71f, size.height * .27f)
                lineTo(size.width * .62f, size.height * .42f)
            }
            drawPath(
                bolt,
                Color.White.copy(alpha = .85f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
            )
        }
    }
}

@Composable
fun WeatherApp(
    data: WeatherBundle?,
    loading: Boolean,
    error: String?,
    language: String,
    celsius: Boolean,
    kmh: Boolean,
    westernNumerals: Boolean,
    animations: Boolean,
    lowPower: Boolean,
    sound: Boolean,
    theme: String,
    savedCities: List<City>,
    currentCity: City,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectCity: (City) -> Unit,
    onSaveCity: (City) -> Unit,
    onRemoveCity: (City) -> Unit,
    onLanguage: (String) -> Unit,
    onCelsius: (Boolean) -> Unit,
    onKmh: (Boolean) -> Unit,
    onWestern: (Boolean) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onLowPower: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onTheme: (String) -> Unit
) {
    var page by remember { mutableStateOf(Page.HOME) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val rtl = language == "ar"
    val dark = when (theme) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }

    val textColor = if (dark) Color.White else Color(0xFF182B3B)
    val backgroundColor = if (dark) Color(0xFF101A2A) else Color(0xFFEAF2F6)

    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = if (dark)
                darkColorScheme(
                    primary = Color(0xFF9EDCFF),
                    surface = Color(0xFF1C3044),
                    background = backgroundColor
                )
            else
                lightColorScheme(
                    primary = Color(0xFF176B91),
                    surface = Color.White,
                    background = backgroundColor
                )
        ) {
            Scaffold(
                containerColor = backgroundColor,
                bottomBar = {
                    NavigationBar(
                        containerColor = if (dark)
                            Color(0xFF101A2A).copy(.96f)
                        else Color.White
                    ) {
                        val entries = listOf(
                            Triple(Page.HOME, Icons.Default.Home, words("الرئيسية", "Home", language)),
                            Triple(Page.HOURLY, Icons.Default.Schedule, words("الساعات", "Hourly", language)),
                            Triple(Page.WEEK, Icons.Default.CalendarMonth, words("الأسبوع", "7 days", language)),
                            Triple(Page.RADAR, Icons.Default.Map, words("الرادار", "Radar", language)),
                            Triple(Page.CITIES, Icons.Default.LocationCity, words("المدن", "Cities", language)),
                            Triple(Page.SETTINGS, Icons.Default.Settings, words("الإعدادات", "Settings", language))
                        )

                        entries.forEach { (target, icon, label) ->
                            NavigationBarItem(
                                selected = page == target,
                                onClick = { page = target },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 9.sp) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (page == Page.HOME || page == Page.HOURLY || page == Page.WEEK) {
                        WeatherAtmosphere(
                            data?.current?.weatherCode ?: 0,
                            data?.current?.isDay ?: true,
                            animations,
                            lowPower,
                            Modifier.fillMaxSize(),
                            data?.current?.temperature
                        )
                    }

                    when (page) {
                        Page.HOME -> HomePage(
                            data, loading, error, language, celsius, kmh,
                            westernNumerals, textColor, currentCity,
                            onRefresh,
                            { searchOpen = true },
                            { data?.let { onSaveCity(currentCity) } }
                        )

                        Page.HOURLY -> HourlyPage(data, language, celsius, westernNumerals, textColor)
                        Page.WEEK -> WeekPage(data, language, celsius, westernNumerals, textColor)
                        Page.RADAR -> RadarPage(data, language, textColor)

                        Page.CITIES -> CitiesPage(
                            savedCities, currentCity, language, textColor,
                            onSelectCity, onRemoveCity, { searchOpen = true }
                        )

                        Page.SETTINGS -> SettingsPage(
                            language, celsius, kmh, westernNumerals,
                            animations, lowPower, sound, theme, textColor,
                            onLanguage, onCelsius, onKmh, onWestern,
                            onAnimations, onLowPower, onSound, onTheme
                        )
                    }

                    if (loading && data != null) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = Color(0xFFB5E7FF)
                        )
                    }

                    if (searchOpen) {
                        AlertDialog(
                            onDismissRequest = { searchOpen = false },
                            title = {
                                Text(words("ابحث عن مدينة", "Search for a city", language))
                            },
                            text = {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    label = {
                                        Text(words("اسم المدينة", "City name", language))
                                    },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (query.isNotBlank()) onSearch(query)
                                    searchOpen = false
                                    query = ""
                                }) {
                                    Text(words("بحث", "Search", language))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { searchOpen = false }) {
                                    Text(words("إلغاء", "Cancel", language))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    data: WeatherBundle?,
    loading: Boolean,
    error: String?,
    lang: String,
    celsius: Boolean,
    kmh: Boolean,
    western: Boolean,
    color: Color,
    city: City,
    refresh: () -> Unit,
    search: () -> Unit,
    save: () -> Unit
) {
    if (data == null) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("IMMERSIVE WEATHER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(22.dp))
            if (loading) CircularProgressIndicator(color = Color.White)
            Text(
                error ?: words("جارٍ تحميل بيانات الطقس…", "Loading live weather…", lang),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = refresh) {
                Text(words("إعادة المحاولة", "Try again", lang))
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White)
                        Text(
                            "${data.city}${if (data.country.isNotBlank()) ", ${data.country}" else ""}",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        "${data.current.time.replace('T', ' ')}  •  ${data.timezone}",
                        color = Color.White.copy(.7f),
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = search) {
                    Icon(Icons.Default.Search, words("بحث", "Search", lang), tint = Color.White)
                }
                IconButton(onClick = save) {
                    Icon(Icons.Default.BookmarkAdd, words("حفظ المدينة", "Save city", lang), tint = Color.White)
                }
                IconButton(onClick = refresh) {
                    Icon(Icons.Default.Refresh, words("تحديث", "Refresh", lang), tint = Color.White)
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(weatherLabel(data.current.weatherCode, lang), color = Color.White.copy(.95f), fontSize = 20.sp)
                Text(weatherGlyph(data.current.weatherCode), fontSize = 48.sp)
                Text(
                    temperature(data.current.temperature, celsius, western),
                    color = Color.White,
                    fontSize = 104.sp,
                    fontWeight = FontWeight.ExtraLight,
                    lineHeight = 108.sp
                )
                Text(
                    "${words("المحسوسة", "Feels like", lang)} ${temperature(data.current.feelsLike, celsius, western)}",
                    color = Color.White.copy(.9f),
                    fontSize = 14.sp
                )
                val today = data.days.firstOrNull()
                if (today != null) {
                    Text(
                        "${words("العظمى", "High", lang)} ${temperature(today.max, celsius, western)}   •   ${words("الصغرى", "Low", lang)} ${temperature(today.min, celsius, western)}",
                        color = Color.White.copy(.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            GlassPanel {
                Column(Modifier.padding(16.dp)) {
                    Text(words("توقعات الساعات القادمة", "Next hours", lang), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        data.hours.take(12).forEach { hour ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(hour.time.substringAfter('T').take(5), color = Color.White.copy(.8f), fontSize = 11.sp)
                                Text(weatherGlyph(hour.code), fontSize = 21.sp)
                                Text(temperature(hour.temperature, celsius, western), color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("${fmt(hour.precipitation, western)}%", color = Color(0xFFB9DDFB), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(words("تفاصيل الطقس", "Weather details", lang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(words("الرطوبة", "Humidity", lang), "${fmt(data.current.humidity, western)}%", Modifier.weight(1f))
                Metric(
                    words("الرياح", "Wind", lang),
                    "${fmt(if (kmh) data.current.windSpeed else data.current.windSpeed / 3.6, western)} ${if (kmh) "km/h" else "m/s"}",
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    words("الحرارة المحسوسة", "Feels like", lang),
                    temperature(data.current.feelsLike, celsius, western),
                    Modifier.weight(1f)
                )
                Metric(
                    w
