package com.almica.ramani.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class WeatherInfo(
    val description: String,
    val icon: ImageVector
)

fun mapWmoCodeToWeather(code: Int): WeatherInfo {
    return when (code) {
        0 -> WeatherInfo("Klarer Himmel", Icons.Filled.WbSunny)
        1, 2, 3 -> WeatherInfo("Leicht bewölkt", Icons.Filled.CloudQueue)
        45, 48 -> WeatherInfo("Nebelig", Icons.Filled.Cloud)
        51, 53, 55, 56, 57 -> WeatherInfo("Nieselregen", Icons.Filled.WaterDrop)
        61, 63, 65, 66, 67 -> WeatherInfo("Regen", Icons.Filled.Grain)
        71, 73, 75, 77 -> WeatherInfo("Schneefall", Icons.Filled.AcUnit)
        80, 81, 82 -> WeatherInfo("Regenschauer", Icons.Filled.Thunderstorm) // Alternativ Icons.Filled.LocalAtmosphere oder Ähnliches
        85, 86 -> WeatherInfo("Schneeschauer", Icons.Filled.SevereCold)
        95, 96, 99 -> WeatherInfo("Gewitter", Icons.Filled.Thunderstorm)
        else -> WeatherInfo("Unbekannt", Icons.AutoMirrored.Filled.HelpOutline)
    }
}
