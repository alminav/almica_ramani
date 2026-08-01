# Implementation Plan - Refactor OfflineMapCreatorScreen

Refactor `OfflineMapCreatorScreen` to follow modern Android development practices by introducing a `ViewModel` for state management and logic, and improving the Compose UI structure.

## Proposed Changes

### [Component] app/src/main/java/com/almica/ramani/

#### [NEW] [OfflineMapCreatorViewModel.kt](file:///C:/Users/altmi/AndroidStudioProjects/ramani_12apr2026_1620/app/src/main/java/com/almica/ramani/OfflineMapCreatorViewModel.kt)
- Create a new `OfflineMapCreatorViewModel` class.
- Define `OfflineMapCreatorUiState` to hold all screen states:
    - `regionName`
    - `tilemakerUrl`
    - `mapType`
    - `tilemakerMaxZoom` / `tilemakerMinZoom`
    - `tileLimit`
    - `tileLimitExceeded` (Long)
    - `progressAnimation` (Float)
    - `statusText` (String)
    - `showDropDownRasterMaptype` (Boolean)
- Move `SharedPreferences` observation and reading logic into the ViewModel.
- Handle `mapType` change logic and corresponding `tilemakerUrl` updates.
- Move deletion logic and other IO-bound tasks to `viewModelScope`.

#### [MODIFY] [OfflineMapCreatorScreen.kt](file:///C:/Users/altmi/AndroidStudioProjects/ramani_12apr2026_1620/app/src/main/java/com/almica/ramani/OfflineMapCreatorScreen.kt)
- Integrate `OfflineMapCreatorViewModel`.
- **UI Refinements:**
    - Use `PaddingValues` from `Scaffold` to ensure content doesn't overlap with system bars.
    - Replace local `remember { mutableStateOf(...) }` with `uiState` from the ViewModel.
    - Simplify the `OfflineMapCreatorContent` parameters by passing the state or specific callbacks.
    - Fix the `regionName` initialization issue by using `LaunchedEffect` or handling it in the ViewModel's `init`.
    - Extract `AlertDialog` into a smaller, reusable Composable if appropriate.
    - Clean up redundant code and logs.
    - Remove `@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")` by actually using the padding.

## User Review Required

> [!IMPORTANT]
> The refactoring moves all preference-related logic to the ViewModel. This is a breaking change for the internal structure of the screen but ensures better testability and configuration change handling.

> [!NOTE]
> I will assume `regionNameParm` is the initial name and should be editable. The ViewModel will initialize its state with this value.

## Verification Plan

### Automated Tests
- Since I'm refactoring UI and State, manual verification is primary, but I will ensure the code compiles.
- If unit tests exist for ViewModels, I will add one for `OfflineMapCreatorViewModel` to verify preference reading logic.

### Manual Verification
- Deploy the app and navigate to the Offline Map Creator.
- Verify that changing the map type updates the URL correctly.
- Verify that progress bars and status text update during "download".
- Verify that the "Tile Limit Exceeded" dialog appears when appropriate.
- Verify that the back press handler works as expected.
