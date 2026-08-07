package com.almica.ramani.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path

/**
 * A custom [ImageVector] representing a square with marked corners.
 * Based on Icons.Outlined.Square but with 4x4 filled squares at each corner.
 */
public val Icons.Outlined.SquareMarkedCorners: ImageVector
    get() {
        if (_squareMarkedCorners != null) {
            return _squareMarkedCorners!!
        }
        _squareMarkedCorners = materialIcon(name = "Outlined.SquareMarkedCorners") {
            // Main hollow square (path copied from Icons.Outlined.Square)
            path(stroke = SolidColor(Color.DarkGray), strokeLineWidth = 0.5f) {
                moveTo(3.0f, 3.0f)
                verticalLineToRelative(18.0f)
                horizontalLineToRelative(18.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(3.0f)
                close()
                moveTo(19.0f, 19.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(5.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(19.0f)
                close()
            }
            // Four 4x4 filled markers at the corners.
            // Adjusted to fit logically within the 24x24 viewport:
            // Top-left: (0,0) to (4,4), Top-right: (20,0) to (24,4),
            // Bottom-left: (0,20) to (4,24), Bottom-right: (20,20) to (24,24).
/*
            path(stroke = SolidColor(Color.Transparent), strokeLineWidth = 0.6f,
                fill = SolidColor(Color.Red)) {
                // Top-left
                // Drawing a 4x4 circle centered at (2.5, 2.5) with radius 2.0
                moveTo(2.5f, 0.5f)
                curveTo(1.4f, 0.5f, 0.5f, 1.4f, 0.5f, 2.5f)
                reflectiveCurveTo(1.4f, 4.5f, 2.5f, 4.5f)
                reflectiveCurveTo(4.5f, 3.6f, 4.5f, 2.5f)
                reflectiveCurveTo(3.6f, 0.5f, 2.5f, 0.5f)
                close()
            }
 */
/*
            path(stroke = SolidColor(Color.Transparent), strokeLineWidth = 0.6f,
                fill = SolidColor(Color.Blue)) {
                // Top-right
                // Drawing a 4x4 circle centered at (21.5, 2.5) with radius 2.0
                moveTo(21.5f, 0.5f)
                curveTo(20.4f, 0.5f, 19.5f, 1.4f, 19.5f, 2.5f)
                reflectiveCurveTo(20.4f, 4.5f, 21.5f, 4.5f)
                reflectiveCurveTo(23.5f, 3.6f, 23.5f, 2.5f)
                reflectiveCurveTo(22.6f, 0.5f, 21.5f, 0.5f)
                close()
            }

            path(stroke = SolidColor(Color.Transparent), strokeLineWidth = 0.6f,
                fill = SolidColor(Color.Yellow)) {
                // Bottom-left
                // Drawing a 4x4 circle centered at (2.5, 21.5) with radius 2.0
                moveTo(2.5f, 19.5f)
                curveTo(1.4f, 19.5f, 0.5f, 20.4f, 0.5f, 21.5f)
                reflectiveCurveTo(1.4f, 23.5f, 2.5f, 23.5f)
                reflectiveCurveTo(4.5f, 22.6f, 4.5f, 21.5f)
                reflectiveCurveTo(3.6f, 19.5f, 2.5f, 19.5f)
                close()
            }
*/
/*
            path(stroke = SolidColor(Color.Transparent), strokeLineWidth = 0.6f,
                fill = SolidColor(Color.Green)) {
                // Bottom-right
                // Drawing a 4x4 circle centered at (21.5, 21.5) with radius 2.0
                moveTo(21.5f, 19.5f)
                curveTo(20.4f, 19.5f, 19.5f, 20.4f, 19.5f, 21.5f)
                reflectiveCurveTo(20.4f, 23.5f, 21.5f, 23.5f)
                reflectiveCurveTo(23.5f, 22.6f, 23.5f, 21.5f)
                reflectiveCurveTo(22.6f, 19.5f, 21.5f, 19.5f)
                close()
            }
 */
        }
        return _squareMarkedCorners!!
    }

private var _squareMarkedCorners: ImageVector? = null
