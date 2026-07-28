package com.almica.ramani_lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.CircleOptions

@Composable
@MapLibreComposable
fun Circle(
    center: LatLng,
    radius: Float,
    isDraggable: Boolean = false,
    color: String = "Yellow",
    opacity: Float = 1.0f,
    borderColor: String = "Black",
    borderWidth: Float = 0.0F,
    zIndex: Int = 0,
    data: JsonElement = JsonNull.INSTANCE,
    onCenterDragged: (LatLng) -> Unit = {},
    onDragFinished: (LatLng) -> Unit = {},
    onClick: (JsonElement?) -> Unit = {},
    onLongClick: (JsonElement?) -> Unit = {}
) {
    val mapApplier = currentComposer.applier as MapApplier

    ComposeNode<CircleNode, MapApplier>(factory = {
        val circleManager = mapApplier.getOrCreateCircleManagerForZIndex(zIndex)

        val circleOptions = CircleOptions()
            .withCircleRadius(radius)
            .withLatLng(center)
            .withDraggable(isDraggable)
            .withCircleStrokeColor(borderColor)
            .withCircleStrokeWidth(borderWidth)
            .withCircleOpacity(opacity)
            .withData(data)

        val circle = circleManager.create(circleOptions)

        CircleNode(
            circleManager,
            circle,
            onCircleDragged = { onCenterDragged(it.latLng) },
            onCircleDragStopped = { onDragFinished(it.latLng) },
            onCircleClicked = { onClick(it.data) },
            onCircleLongClicked = { onLongClick(it.data) }
        )
    }, update = {
        update(onCenterDragged) {
            this.onCircleDragged = { onCenterDragged(it.latLng) }
        }

        update(onDragFinished) {
            this.onCircleDragStopped = { onDragFinished(it.latLng) }
        }

        set(center) {
            circle.latLng = center
            circleManager.update(circle)
        }

        set(color) {
            circle.circleColor = color
            circleManager.update(circle)
        }

        set(radius) {
            circle.circleRadius = radius
            circleManager.update(circle)
        }
    })
}
