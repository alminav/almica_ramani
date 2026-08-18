# Changelog

All notable changes to the **Ramani** project will be documented in this file.

## [Unreleased] - 2026-08-18

### ✨ Features & Improvements
- **Location Monitoring**: Enhanced `MonitorGraphLocations` with multi-mode visualization:
    - **Graphs**: Interactive Altitude and Speed graphs with inverse X-axis support.
    - **Speedometer**: Real-time speed display with adaptive scales based on locomotion type.
    - **Compass**: Enhanced direction view integrated with route tracking and nearest POI discovery.
- **Route Visualization**: Integrated `KiThumbnailer` for dynamic route thumbnail generation with POI icons and current position markers.

### 🏗️ Refactoring & UI
- **Map Controls**: Optimized Latitude/Longitude grid coordinate display in `MapControlsLayer`.
    - **Performance**: Cached layer visibility checks to reduce map engine overhead during navigation.
    - **Layout**: Simplified positioning logic using `Modifier.align` and extracted reusable `GridCoordinateLabel` component.
- **Theming**: Centralized semi-transparent grid label colors in `Color.kt`.

## [Unreleased] - 2026-08-06
    ...

### 🏗️ Refactoring & Architecture
- **DocumentViewer Refactor**: Major architectural overhaul of the document viewing system.
    - **ViewModel Implementation**: Extracted business logic, file I/O, and state management into `DocumentViewerViewModel`.
    - **Type Safety**: Introduced `RouteInfo` data class to replace generic `Triple` types for route data handling.
    - **Concurrency Optimization**: Migrated heavy parsing and file operations to background threads using Kotlin Coroutines (`Dispatchers.IO` and `Dispatchers.Default`).
    - **Material 3 UI**: Updated `DocumentViewer` to use modern M3 components including `Scaffold`, `SnackbarHost`, and `CenterAlignedTopAppBar`.
- **Navigation & Integration**: Updated `NavGraph`, `MapOverlayManager`, and `ListRouteFoldersActivity` to support the new `DocumentViewer` architecture and data models.

## [Unreleased] - 2026-08-04

### ✨ Features & Improvements
- **GeoJSON Routes**: Significant improvements and refactoring of the GeoJSON routes management in `RoutesGeojsonScreen`.
- **Thumbnail Management**: Refined route thumbnail logic and directory handling.

### 🐛 Bug Fixes
- **File System**: Fixed `FileNotFoundException` (ENOENT) when saving thumbnails by ensuring the `thumbnails` directory is created before file operations in `DocumentViewer` and `ListRouteFoldersActivity`.

### ⚙️ Build & Configuration
- **Gradle Updates**: Updated build configurations and dependency management in `app/build.gradle.kts`.

## [Unreleased] - 2026-08-03

### 🚀 Build & Release Optimization
- **APK Signing**: Configured release signing with automated credential loading from `local.properties`.
- **R8 Minification**: Enabled R8 obfuscation and resource shrinking, reducing APK size from 204MB to 134MB.
- **Proguard Rules**: Added custom keep rules for **GraphHopper** and **MapLibre** to ensure stability in minified builds.

### 🛡️ Security
- **API Key Protection**: Migrated the Google Maps API Key from `AndroidManifest.xml` to a secure `secrets.xml` resource.
- **Git Hardening**: Added `secrets.xml`, `local.properties`, and `keystore/` to `.gitignore` to prevent accidental credential leakage.

### ✨ Features & Improvements
- **Documentation**: Overhauled `README.md` with updated security guidelines and rendering backend configuration.
- **UI Refinement**: Improved layout for side-by-side picture viewing.
- **Logging cleanup**: Reduced noise in production by removing excessive `Timber` logs.

### 🐛 Bug Fixes
- **Speed Display**: Corrected calculation error in speed display (applied 3.6 conversion factor).
- **GeoJSON Interactions**: Suppressed unnecessary EXIF alerts when interacting with GeoJSON map features.
- **Project Structure**: Excluded `backups` and `pictures` directories from the IDEA project view to declutter the workspace.

---
*Generated on 2026-08-03*
