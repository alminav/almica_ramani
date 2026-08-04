# Changelog

All notable changes to the **Ramani** project will be documented in this file.

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
