package com.almica.ramani.googlemaps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 * This composable animates a vehicle (e.g., car or bike) marker along a polyline path—
 * similar to how ride-hailing or delivery apps show the driver's live location.
 *
 * It interpolates between points on the polyline using easing curves,
 * updates the marker’s position and rotation,
 * and computes frame-by-frame delays based on the actual distance between points.
 *
 * This creates a smooth, real-world movement simulation of a vehicle along a route.
 */
@Composable
fun SimulateCarMovement(
    // List of coordinates representing the route
    cameraPositionState: CameraPositionState,
    // Custom car marker icon
    routePolyline: List<LatLng>,
    // Simulated speed in kilometers per hour
    carIcon: BitmapDescriptor,
    speedKmph: Double = 10.0,
    moveMap: (LatLng?) -> Unit,
    finished: () -> Unit
) {
    //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: speedKmph: $speedKmph")
    // Car's current LatLng position (drives the Marker UI)
    val carPosition = remember { mutableStateOf(routePolyline.first()) }

    // Car's rotation in degrees (used to face the direction it's moving)
    val carRotation = remember { mutableFloatStateOf(0f) }

    // Marker state that updates as carPosition changes
    val markerState = rememberUpdatedMarkerState(position = carPosition.value)


    Marker(
        state = markerState,
        icon = carIcon,     // Vehicle icon
        flat = true,        // Ensures the marker rotates on a 2D plane
        anchor = Offset(0.5f, 0.5f),    // Center the icon's reference point
        rotation = carRotation.floatValue,    // // Rotate to match movement direction
        onClick = { marker ->
            Timber.i( "${marker.title} click")
            finished()
            false // Rückgabewert steuert, ob die Standard-Info-Ansicht angezeigt wird
        }
    )

    // Launch the animation once when the polyline is available
    LaunchedEffect(routePolyline) {
        for (i in 0 until routePolyline.lastIndex) {
            val start = routePolyline[i]
            val end = routePolyline[i + 1]

            // Wrap each LatLng in a LatLngPoint to use with utility functions
            val startPoint = LatLng(start.latitude, start.longitude)
            val endPoint = LatLng(end.latitude, end.longitude)

            // Use current rotation as starting angle; fallback to 0f
            val startRotation = carRotation.floatValue

            // Calculate the target bearing angle between start and end
            val targetRotation = MapUtils.getBearing(startPoint, endPoint)

            // Compute the physical distance of this segment (in meters)
            val distanceMeters = MapUtils.calculateHaversineDistance(startPoint, endPoint)

            // Simulate time to traverse the segment based on speed
            val durationMs = ((distanceMeters / 1000.0) / speedKmph) * 3600_000
            //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: durationMs: $durationMs")
            // Break the segment into smaller steps for smoother animation
            val steps = 30
            val stepDuration = (durationMs / steps).coerceIn(10.0, 100.0).toLong()
            val visibleRegion = cameraPositionState.projection?.visibleRegion
            val latLngBounds = visibleRegion?.latLngBounds
            latLngBounds?.contains(carPosition.value)?.let {
                if (!it) {
                    moveMap(carPosition.value)
                    //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: carPosition outside")
                } //else Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: carPosition inside")
            }
            //Timber.i( "${Thread.currentThread().stackTrace[2].lineNumber}: stepDuration: $stepDuration")
            for (step in 1..steps) {
                val t = step.toFloat() / steps // Step progress (0.0 to 1.0)

                // Apply easing function to smooth movement (e.g., accelerate then decelerate)
                val easedT = MapUtils.easeInOutCubic(t)

                // Interpolate LatLng between start and end using eased progress
                val lat = start.latitude + (end.latitude - start.latitude) * easedT
                val lng = start.longitude + (end.longitude - start.longitude) * easedT

                // Interpolate rotation to smoothly turn the vehicle
                val interpolatedRotation = MapUtils.lerpAngle(startRotation, targetRotation, easedT)

                // Update marker position and rotation
                carPosition.value = LatLng(lat, lng)
                carRotation.floatValue = interpolatedRotation

                // Small delay between steps to create animation
                delay(stepDuration.milliseconds)
            }
        }
        finished()
    }
}

@Preview(showBackground = true)
@Composable
fun SimulateCarMovementPreview() {
    val cameraPositionState = rememberCameraPositionState()
    val routePolyline = listOf(
        LatLng(-1.286389, 36.817223),
        LatLng(-1.287000, 36.818000),
        LatLng(-1.288000, 36.819000),
        LatLng(-1.289000, 36.820000)
    )

    RamaniTheme {
        GoogleMap(
            cameraPositionState = cameraPositionState
        ) {
            SimulateCarMovement(
                cameraPositionState = cameraPositionState,
                routePolyline = routePolyline,
                carIcon = BitmapDescriptorFactory.defaultMarker(),
                speedKmph = 20.0,
                moveMap = {},
                finished = {}
            )
        }
    }
}
