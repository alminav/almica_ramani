package com.almica.ramani.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap

/**
 * Extension function to convert an [ImageVector] to a [Bitmap].
 *
 * This function must be called from a Composable context as it relies on [LocalDensity]
 * and [rememberVectorPainter].
 *
 * @param width The desired width of the resulting bitmap in Dp. Defaults to 24.dp.
 * @param height The desired height of the resulting bitmap in Dp. Defaults to 24.dp.
 * @param layoutDirection The layout direction for rendering. Defaults to [LayoutDirection.Ltr].
 * @return A [Bitmap] representation of the [ImageVector].
 */
@Composable
fun ImageVector.toBitmap(
    width: Dp = 24.dp,
    height: Dp = 24.dp,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr
): Bitmap {
    val density = LocalDensity.current
    val painter = rememberVectorPainter(this)
    val widthPx = with(density) { width.toPx() }.toInt()
    val heightPx = with(density) { height.toPx() }.toInt()

    return remember(this, width, height, layoutDirection, density) {
        val imageBitmap = ImageBitmap(widthPx, heightPx)
        val canvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = Size(widthPx.toFloat(), heightPx.toFloat())
        ) {
            with(painter) {
                draw(size = size)
            }
        }
        imageBitmap.asAndroidBitmap()
    }
}

/**
 * Converts a drawable resource to a [Bitmap].
 * Useful for legacy Android drawables or vector drawables from resources.
 */
fun Context.drawableToBitmap(drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null
    return createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1)
    ).applyCanvas {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(this)
    }
}
