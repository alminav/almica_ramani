package com.almica.ramani_lib

import android.content.Context
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolOptions
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_CENTER
import org.maplibre.android.style.layers.Property.TEXT_ANCHOR_CENTER
import org.maplibre.android.style.layers.Property.TEXT_JUSTIFY_CENTER
import timber.log.Timber

/**
 * almica 23aug2025
 * clone of Symbol.kt with changes for vector drawables
 */
@Composable
@MapLibreComposable
fun SymbolVector(context: Context,
           center: LatLng,
           size: Float = 1F,
           color: String = "",
           isDraggable: Boolean = false,
           zIndex: Int = 0,
           imageId: Int? = null,
           imageAnchor: String = ICON_ANCHOR_CENTER,
           imageOffset: Array<Float> = arrayOf(0f, 0f),
           imageRotation: Float? = null,
           text: String? = null,
           textAnchor: String = TEXT_ANCHOR_CENTER,
           textJustify: String = TEXT_JUSTIFY_CENTER,
           textOffset: Array<Float> = arrayOf(0f, 3f),
           textColor: String = "#000000",
           textHaloColor: String = "#000000",
           textHaloWidth: Float = 0f,
           data: JsonElement = JsonNull.INSTANCE,
           onSymbolDragged: (LatLng) -> Unit = {},
           onDragFinished: (LatLng) -> Unit = {},
           onClick: (JsonElement?) -> Unit = {},
           onLongClick: (JsonElement?) -> Unit = {}
) {
    val mapApplier = currentComposer.applier as MapApplier

    val style = mapApplier.style.value
    LaunchedEffect(imageId, style) {
        if (imageId != null && style != null) {
            mapApplier.map.getStyle { loadedStyle ->
                try {
                    val d = ContextCompat.getDrawable(context, imageId)
                    if (d != null) {
                        val bitmap = createBitmap(d.intrinsicWidth, d.intrinsicHeight)
                        val canvas = Canvas(bitmap)
                        d.setBounds(0, 0, bitmap.width, bitmap.height)
                        d.draw(canvas)
                        if (loadedStyle.getImage("$imageId") == null) {
                            loadedStyle.addImage("$imageId", bitmap)
                        }
                    }
                } catch (e: Exception) {
                    // This can happen if a newer style is loading/has loaded
                    Timber.w("SymbolVector: Style is no longer valid: ${e.message}")
                }
            }
        }
    }

    ComposeNode<SymbolNode, MapApplier>(factory = {
        val symbolManager = mapApplier.getOrCreateSymbolManagerForZIndex(zIndex)

        var symbolOptions = SymbolOptions()
            .withDraggable(isDraggable)
            .withLatLng(center)
            .withData(data)

        imageId?.let {
            symbolOptions = symbolOptions
                .withIconImage(imageId.toString())
                .withIconColor(color)
                .withIconSize(size)
                .withIconAnchor(imageAnchor)
                .withIconRotate(imageRotation)
                .withIconOffset(imageOffset)
        }

        text?.let {
            symbolOptions = symbolOptions
                .withTextField(text)
                .withTextColor(textColor)
                .withTextHaloColor(textHaloColor)
                .withTextHaloWidth(textHaloWidth)
                .withTextSize(size * 10) // almica 26aug2025
                .withTextJustify(textJustify)
                .withTextAnchor(textAnchor)
                .withTextOffset(textOffset)
        }

        val symbol = symbolManager.create(symbolOptions)

        SymbolNode(
            symbolManager,
            symbol,
            onSymbolDragged = { onSymbolDragged(it.latLng) },
            onSymbolDragStopped = { onDragFinished(it.latLng) },
            onSymbolClicked = { onClick(it.data) },
            onSymbolLongClicked = { onLongClick(it.data) }
        )
    }, update = {
/*
        set(isDraggable) {
            Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: zoom:${mapApplier.map.zoom}")
            symbol.isDraggable = mapApplier.map.zoom > 13.0
        }
 */
        set(center) {
            symbol.latLng = center
            symbolManager.update(symbol)
        }

        set(text) {
            symbol.textField = text
            symbolManager.update(symbol)
        }

        set(color) {
            symbol.iconColor = color
        }

        set(imageRotation) {
            symbol.iconRotate = imageRotation
        }
    })
}
