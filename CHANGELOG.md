# Changelog

All notable changes to the **Ramani** project will be documented in this file.

## [Unreleased] - 2026-08-22

### ✨ Features & Improvements
- **Google Maps Elevation Service Integration**:
    - **Route Refresh**: Added support for refreshing route elevations using the Google Maps Elevation API.
    - **POI Refresh**: Implemented elevation refresh for individual POIs in the database.
- **POI Database Overhaul**:
    - **Material 3 Migration**: Fully refactored the POI database screen using M3 `Scaffold`, `TopAppBar`, and `NavigationBar`.
    - **Advanced Filtering**: Added search-by-name and category-based filtering.
    - **Spatial Awareness**: Real-time distance and directional heading calculations for POIs relative to the current map center.
    - **Modern Feedback**: Integrated `MoboPoiSnack` (Material 3 bottom sheet snackbar) for transient UI feedback.
- **Route Monitoring Enhancements**:
    - **GMS Elevation Provider**: Added a new elevation provider option in the `RouteMonitorMenu`.
    - **Camera Control**: New `setCameraTarget` helper for smoother map transitions during location selection.

### 🏗️ Refactoring & Architecture
- **State Management**: Converted `PolygonState` to a `data class` to enable idiomatic Kotlin `copy()` functionality and improved immutability patterns.
- **Route Loading Unified**: Standardized route processing (`loadRouteFromFile`, `loadRouteFromLllh`) with consistent simplification and state mapping.
- **UI Interaction**: Cleaned up `MapOverlayManager` by removing redundant expressions and refining snackbar parameters.

### 🐛 Bug Fixes
- **Build Stability**: Resolved "Unresolved reference 'copy'" compilation error in `MapOverlayManager.kt`.
- **UI Logic**: Fixed stray code fragments and improper string formatting in map overlay notifications.

## [Unreleased] - 2026-08-21

### ✨ Features & Improvements
- **Route Tracking Optimization**:
    - **Reactive Map Updates**: Refactored `MainMapView` for route highlight tracking, improving performance during map movement.
- **Gradient Chart Refactoring**:
    - **Real-time Synchronization**: Updated `GradientChartMonitor` to use `GpsViewModel` for live location and time tracking, replacing legacy preference observers.
    - **Performance Optimization**: Implemented route simplification (`simplifyToTargetCount`) to reduce the number of points rendered in the elevation chart.
    - **UI Modernization**: Refactored chart containers to use Material 3 `Card` and `Surface` components.

### 🏗️ Refactoring & Architecture
- **State Consistency**: Improved main-thread safety in `MainMapView` by isolating heavy distance calculations to `Dispatchers.Default` while ensuring UI state updates remain atomic.
- **Code Cleanup**: Removed unused imports and redundant variables across `MainMapView` and `GradientChartMonitor`.

## [Unreleased] - 2026-08-20

### ✨ Features & Improvements
- **Chart Logic Refinement**:
    - **Simplified Tracking**: Updated `GradientChart` and `ElevationChart` to handle `routePointer` independently of device heading.
    - **Tolerance Checking**: Implemented route tolerance validation to notify users when they are off-path.
- **Gradient Chart Enhancements**: Improved the `GradientChart` UI and interactivity.
    - **Slider Integration**: Synced `sliderPosition` with `routePointer` for real-time elevation tracking during navigation.
    - **Visual Cleanup**: Removed black borders from bars in `BarChartAdjustableAnimation` for a cleaner aesthetic.
    - **Precision Labeling**: Optimized mid-point distance labeling in `GradientChartDataModel`.
- **Logging Optimization**: Reduced `Timber` log noise in chart rendering and animation components.

### 🏗️ Refactoring & Architecture
- **Monitor UI Cleanup**: Refactored `GradientChartMonitor`, `MonitorGraphLocations`, and their respective ViewModels (`GradientChartViewModel`, `MonitorViewModel`) for better separation of concerns and state management.

### ⚙️ Build & Configuration
- **R8/Proguard Hardening**: Added explicit keep rules for **GraphHopper** and **MapLibre** in `app/proguard-rules.pro` to prevent build issues in minified releases.
- **Repository Maintenance**: Updated `app/.gitignore` to exclude build artifacts.

### 🌐 Localization
- **String Resources**: Updated `strings.xml` with new labels for route status and chart feedback.

## [Unreleased] - 2026-08-19

### ✨ Features & Improvements
- **Elevation Chart Updates**: Integrated current position markers into the `ElevationChart` for real-time tracking.
- **Adaptive Bottom Sheet**: Migrated `MonitorGraphLocations` to a `BottomSheetScaffold`. Peek height now dynamically adjusts based on the active overlay and graph type.
- **Haircross Refinement**: Adjusted map center offset (`hairCrossOffsetFraction`) during location statistic viewing for better visibility.

### 🏗️ ViewModel Logic & Optimization
- **State Atomicity**: Refactored `MainViewModel#updatePolygon` to perform atomic state updates, reducing unnecessary UI recompositions.
- **Automatic Graph Switching**: Added logic to automatically toggle between `COMPASS` and `COMPASS_THUMBNAIL` modes based on route activity.
- **Overlay Management**: Improved reset logic for interaction states when switching between UI layers.

### 🐛 Bug Fixes
- **State Sync**: Fixed synchronization issues between graph types and loaded route data.
- **UI Layout**: Resolved button overlap issues in the bottom bar during route loading.

## [Unreleased] - 2026-08-06

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
*Generated on 2026-08-22*
