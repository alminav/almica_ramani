package com.almica.ramani.weather

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    latitude: Double? = null,
    longitude: Double? = null,
    viewModel: WeatherViewModel = viewModel()
) {
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            Timber.i("WeatherScreen: $latitude, $longitude")
            viewModel.loadWeather(latitude, longitude)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    WeatherScreenContent(
        uiState = uiState,
        modifier = modifier,
        onRefresh = { viewModel.loadWeather(latitude, longitude) }
    )
}

@Composable
fun WeatherScreenContent(
    uiState: WeatherUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    Box(
        modifier = modifier.padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator()
            is WeatherUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Fehler: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Timber.e("Error: ${state.message}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Erneut versuchen")
                    }
                }
            }
            is WeatherUiState.Success -> {
                WeatherDisplay(weather = state.weather, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
fun WeatherDisplay(weather: WeatherResponse, modifier: Modifier = Modifier, onRefresh: () -> Unit) {
    val current = weather.current
    // Holt das passende Icon und den Text basierend auf dem Code
    val weatherInfo = mapWmoCodeToWeather(current.weather_code)

    Card(
        modifier = modifier.fillMaxWidth().padding(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = "Aktuelles Wetter",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(5.dp))

            val locale = LocalLocale.current.platformLocale
            val formattedTime = remember(current.time, locale) {
                try {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                    val localDateTime = LocalDateTime.parse(current.time, formatter)
//                    val zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"))
//                        .withZoneSameInstant(ZoneId.systemDefault())
                    localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", locale))
                } catch (e: Exception) {
                    current.time.replace("T", " ")
                }
            }

            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wetter-Icon groß anzeigen
            Icon(
                imageVector = weatherInfo.icon,
                contentDescription = weatherInfo.description,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Textbeschreibung (z. B. "Leicht bewölkt")
            Text(
                text = weatherInfo.description,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${current.temperature_2m}°C",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetailItem(label = "Feuchtigkeit", value = "${current.humidity}%")
                WeatherDetailItem(label = "Wind", value = "${current.wind_speed_10m} km/h")
            }

            weather.daily?.let { daily ->
                if (daily.sunrise.isNotEmpty() && daily.sunset.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetailItem(
                            label = "Sonnenaufgang",
                            value = daily.sunrise[0].split("T").lastOrNull() ?: "--:--"
                        )
                        WeatherDetailItem(
                            label = "Sonnenuntergang",
                            value = daily.sunset[0].split("T").lastOrNull() ?: "--:--"
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WeatherScreenPreview() {
    RamaniTheme {
        WeatherScreenContent(
            uiState = WeatherUiState.Success(
                weather = WeatherResponse(
                    current = CurrentWeather(
                        time = "2024-03-21T12:00",
                        temperature_2m = 22.5,
                        wind_speed_10m = 12.0,
                        weather_code = 1, // Leicht bewölkt
                        humidity = 45
                    )
                )
            ),
            onRefresh = {}
        )
    }
}
