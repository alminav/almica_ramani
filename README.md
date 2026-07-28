# Ramani

Ramani is a powerful mapping and navigation application for Android, designed to provide comprehensive geospatial features with a focus on flexibility, offline capabilities, and data visualization.

## 🚀 Key Features

- **Hybrid Mapping Engines**: Seamlessly switch between **Google Maps** (for high-fidelity data) and **MapLibre** (for high-performance vector tiles and OpenGL rendering).
- **Advanced Overlays**: Support for custom tile providers, raster map overlays, and geojson visualizations.
- **Offline Routing**: Integrated with **GraphHopper** for robust routing and navigation without an internet connection.
- **Location Tracking**: Foreground and background location tracking with comprehensive GPS satellite status monitoring.
- **Data Visualization**: Rich charting capabilities for elevation profiles, speed statistics, and route gradients using custom chart libraries.
- **POI Management**: Efficient database for Points of Interest (POIs) with search and categorization.
- **PDF Export**: Generate professional reports and map snapshots directly from the app.

## 🛠️ Tech Stack

- **UI**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) for a modern, reactive interface.
- **Maps**: [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview) & [MapLibre Native SDK](https://maplibre.org/maplibre-native/).
- **Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) for caching routes, locations, and POIs.
- **Concurrency**: Kotlin Coroutines and Flows for efficient background processing.
- **Logging**: [Timber](https://github.com/JakeWharton/timber) for structured and robust logging.
- **Rendering**: Configurable OpenGL/Vulkan backend support for optimal performance across various hardware (e.g., Nokia 1 support).

## 📂 Project Structure

The project is organized into several modular components to ensure separation of concerns and reusability:

- **`:app`**: The main entry point, containing the UI, ViewModels, and activity orchestration.
- **`:graphhopper`**: Wrapper and integration for the GraphHopper offline routing engine.
- **`:room-locations`**: Data access layer for persistent location and route storage.
- **`:gpssatstatus`**: Module dedicated to monitoring and reporting GPS satellite signal quality.
- **`:YChartsLib` & `:composecharts`**: Specialized libraries for rendering geospatial and biometric data charts.
- **`:googlePlacesSearch`**: Integration with Google Places for location discovery.
- **`:live-preferences`**: A reactive wrapper for Android Shared Preferences.

## 🏗️ Getting Started

### Prerequisites

- Android Studio Koala (2024.1.1) or newer.
- Android SDK 28+ (Min SDK 28).
- A valid Google Maps API Key (configured in `AndroidManifest.xml`).

### Build Configuration

The project supports configurable rendering backends. By default, it uses OpenGL. You can configure this in `gradle.properties`:

```properties
ramani.render.useOpenGL=true
```

To build the debug APK:

```bash
./gradlew assembleDebug
```

## 📄 License

This project is proprietary. All rights reserved.
