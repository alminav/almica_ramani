package com.almica.ramani_lib

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode

@MapLibreComposable
@Composable
fun MapObserver(
    onMapMoved: () -> Unit = {},
    onMapScaled: () -> Unit = {},
    onMapRotated: (Double) -> Unit = {},
) {
    ComposeNode<MapObserverNode, MapApplier>(
        factory = {
            MapObserverNode(onMapMoved, onMapScaled, onMapRotated)
        },
        update = {
            update(onMapMoved) {
                this.onMapMoved = onMapMoved
            }

            update(onMapScaled) {
                this.onMapScaled = onMapScaled
            }

            update(onMapRotated) {
                this.onMapRotated = onMapRotated
            }
        }
    )
}
