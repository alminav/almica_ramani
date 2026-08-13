package com.almica.ramani.weather

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber

@Composable
fun WeatherScreen(
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
        onRefresh = { viewModel.loadWeather(latitude, longitude) }
    )
}

@Composable
fun WeatherScreenContent(
    uiState: WeatherUiState,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> CircularProgressIndicator()
            is WeatherUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Fehler: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Timber.i("Error: ${state.message}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Erneut versuchen")
                    }
                }
            }
            is WeatherUiState.Success -> {
                WeatherDisplay(current = state.weather.current, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
fun WeatherDisplay(current: CurrentWeather, onRefresh: () -> Unit) {
    // Holt das passende Icon und den Text basierend auf dem Code
    val weatherInfo = mapWmoCodeToWeather(current.weather_code)

    Card(
        modifier = Modifier.fillMaxWidth().padding(6.dp),
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
