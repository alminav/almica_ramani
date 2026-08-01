# Walkthrough - Preference Observation Improvement

I have improved the way the `PREF_GMS_NORTH_UP` preference is observed and managed in the `GmsTileOverlayActivity`.

## Changes Made

### 1. Updated `GmsMapViewModel`
- Added `northUp` state to `GmsMapUiState`.
- Implemented `OnSharedPreferenceChangeListener` within the ViewModel to observe changes to `Const.PREF_GMS_NORTH_UP` and update the UI state automatically.
- Ensured proper lifecycle management by unregistering the listener in `onCleared()`.

### 2. Refactored `GmsTileOverlayActivity`
- Removed the local `northup` state and the direct `LiveSharedPreferences` observation from the `GmsContent` Composable.
- Updated all sub-composables (`MapOverlayContent`, `MapControls`, etc.) to use `uiState.northUp` instead of a passed-in parameter.
- Cleaned up unused imports and variables.

## Benefits
- **Performance**: Recompositions no longer trigger redundant observer registrations.
- **Architecture**: Preference state is now properly managed in the ViewModel, following the Single Source of Truth principle.
- **Robustness**: Using a standard preference listener in the ViewModel avoids potential memory leaks associated with direct LiveData observation in Composables.

## Verification Results
- The code was checked for compilation errors (none found).
- Unused variables and imports were removed.
