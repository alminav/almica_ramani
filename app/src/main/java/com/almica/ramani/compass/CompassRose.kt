package com.almica.ramani.compass

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.ui.theme.RamaniTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassRose(
    azimuth: Float, // Rotation in degrees (0 = North, 90 = East, etc.)
    modifier: Modifier = Modifier
) {
    // Define colors
    val darkColor = Color(0xFF2C3E50)
    val lightColor = Color(0xFFBDC3C7)
    val textColor = android.graphics.Color.BLACK
/*    Image(
        modifier = modifier
            .fillMaxSize()
            .rotate(-azimuth),
        painter = painterResource(id = com.almica.ramani.R.drawable.ic_rose_very_simple),
        contentDescription = stringResource(id = com.almica.ramani.R.string.compass),
        colorFilter = ColorFilter.tint(
            color = MaterialTheme.colors.onBackground
        )
    )*/

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = min(centerX, centerY) * 0.90f
        val widthFactor = radius * 0.15f // Width of the point base

        // Helper paths for the points (reusable within the DrawScope)
        val rightPointPath = Path()
        val leftPointPath = Path()

        // Total canvas rotation based on the azimuth
        rotate(degrees = -azimuth, pivot = Offset(centerX, centerY)) {

            // 1. The 4 main cardinal directions (N, E, S, W)
            for (i in 0 until 4) {
                rotate(degrees = i * 90f, pivot = Offset(centerX, centerY)) {
                    // Right (dark) side of the point
                    rightPointPath.apply {
                        reset()
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY - radius)
                        lineTo(centerX + widthFactor, centerY)
                        close()
                    }
                    drawPath(path = rightPointPath, color = darkColor)

                    // Left (light) side of the point
                    leftPointPath.apply {
                        reset()
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY - radius)
                        lineTo(centerX - widthFactor, centerY)
                        close()
                    }
                    drawPath(path = leftPointPath, color = lightColor)
                }
            }

            // 2. The 4 intermediate directions (NE, SE, SW, NW) - slightly shorter
            /*
            val subRadius = radius * 0.7f
            val subWidthFactor = subRadius * 0.15f

            for (i in 0 until 4) {
                // Offset by 45 degrees for the intermediate directions
                rotate(degrees = (i * 90f) + 45f, pivot = Offset(centerX, centerY)) {
                    rightPointPath.apply {
                        reset()
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY - subRadius)
                        lineTo(centerX + subWidthFactor, centerY)
                        close()
                    }
                    drawPath(path = rightPointPath, color = darkColor)

                    leftPointPath.apply {
                        reset()
                        moveTo(centerX, centerY)
                        lineTo(centerX, centerY - subRadius)
                        lineTo(centerX - subWidthFactor, centerY)
                        close()
                    }
                    drawPath(path = leftPointPath, color = lightColor)
                }
            }
             */
        }
        drawCircle(
            Color.LightGray,
            28F,
            Offset(centerX, centerY),
            1f,
            Fill,
            null,
            DrawScope.DefaultBlendMode
        )
        drawCircle(
            Color.DarkGray,
            radius +52f, //size.height,
            Offset(centerX, centerY),
            1f,
            style = Stroke(3f),
            null,
            DrawScope.DefaultBlendMode
        )
        // 3. Draw static text labels (N, E, S, W) outside the rotation,
        // so that the letters always remain upright.
        val labelOffset = radius + 36f
        val labels = arrayOf("N", "E", "S", "W")

        // Access the native Android canvas for text rendering
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint().apply {
                color = textColor
                textSize = 20.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (i in labels.indices) {
                // Mathematical positioning of the letters based on rotation
                val angleRad = Math.toRadians((i * 90 - azimuth - 90).toDouble())
                val x = (centerX + labelOffset * cos(angleRad)).toFloat()
                val y = (centerY + labelOffset * sin(angleRad)).toFloat() -
                        ((paint.descent() + paint.ascent()) / 2f) // Vertical centering

                drawText(labels[i], x, y, paint)
            }
        }
    }
    Text(
        modifier = modifier.padding(top = 0.dp)
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
        text = stringResource(id = com.almica.ramani.R.string.degree_format, azimuth.toInt()),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body1
    )
}

@Preview(showBackground = true)
@Composable
fun CompassRosePreview() {
    RamaniTheme {
        Surface {
            CompassRose(
                azimuth = 45f,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}
