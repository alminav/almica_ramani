# Almica-Ramani

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
- A valid Google Maps API Key.

### Build Configuration

#### API Key Security
This project uses a `secrets.xml` file to store sensitive keys. This file is excluded from Git.
1. Create `app/src/main/res/values/secrets.xml` if it doesn't exist.
2. Add your key:
   ```xml
   <resources>
       <string name="google_maps_key" translatable="false">YOUR_API_KEY_HERE</string>
   </resources>
   ```

#### Rendering Backend
The project supports configurable rendering backends for MapLibre (critical for older devices like Nokia 1). Configure this in `gradle.properties`:

```properties
ramani.render.useOpenGL=false # Set to true for OpenGL, false for Vulkan
```

To build the debug APK:

```bash
./gradlew assembleDebug
```

## 📄 License
This project is proprietary. All rights reserved.

## 📥 Download Apk

<p align="center">
 <a href="https://github.com/alminav/almica_ramani/releases"><img alt="GitHub" src="https://user-images.githubusercontent.com/69304392/148696068-0cfea65d-b18f-4685-82b5-329a330b1c0d.png" height="40"/></a>
</p>>
## 📸 Screenshots

<p align="center">
      <img width="25%" alt="Screenshot_20260730-143821" src="https://github.com/user-attachments/assets/380e453d-d530-45de-b3b7-eb1264d5b728" />
&nbsp; &nbsp; &nbsp; &nbsp;
      <img width="25%" alt="Screenshot_20260801-181922" src="https://github.com/alminav/almica_ramani/blob/master/pictures/Screenshot_20260801-181922.png" />  
</p>