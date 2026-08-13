package com.almica.ramani.weather

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * [WeatherRepository]
 *        ▲
 *        │ (wird aufgerufen in)
 *  [WeatherViewModel]  ◄─── (hält den Zustand/UI-State)
 *        ▲
 *        │ (beobachtet den Zustand)
 *   [WeatherScreen] (Compose UI)
 */
// Der Aufruf geschieht in der Methode loadWeather() innerhalb der Datei WeatherViewModel.kt:
@Serializable
data class WeatherResponse(
    val current: CurrentWeather
)

@Serializable
data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val wind_speed_10m: Double,
    val weather_code: Int, // <-- Neu hinzugefügt für Icons
    @SerialName("relative_humidity_2m") val humidity: Int
)

class WeatherRepository {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    // Beispielkoordinaten für Berlin (Latitude: 52.52, Longitude: 13.41)
    suspend fun fetchCurrentWeather(lat: Double = 52.52, lon: Double = 13.41): WeatherResponse {
        val url = "https://api.open-meteo.com/v1/forecast"
        return httpClient.get(url) {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("current", "temperature_2m,wind_speed_10m,weather_code,relative_humidity_2m")
        }.body()
    }
}
