package com.almica.ramani.compass

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.almica.ramani.R
import com.almica.ramani.compass.theme.ThemedPreview
import com.google.accompanist.drawablepainter.rememberDrawablePainter

val COMPASS_PADDING = 16.dp
val COMPASS_ROSE_PADDING = 24.dp
const val UPDATE_FREQUENCY = 250
@Composable
fun Compass(
    direction: Int?,
    rotation: Int
)
{
    val (lastRotation, setLastRotation) = remember { mutableIntStateOf(0) }
    var newRotation = lastRotation
    val modLast = if (lastRotation > 0) lastRotation % 360 else 360 - (-lastRotation % 360)
    val animatedAzimuth by animateFloatAsState(
        targetValue = rotation.toFloat(),
        animationSpec = spring(), // Provides natural spring behavior
        label = "CompassRotation"
    )
    if (modLast != rotation)
    {
        // new rotation comes in
        val backward = if (rotation > modLast) modLast + 360 - rotation else modLast - rotation
        val forward = if (rotation > modLast) rotation - modLast else 360 - modLast + rotation
        
        newRotation = if (backward < forward)
        {
            // backward rotation is shorter
            lastRotation - backward
        }
        else
        {
            // forward rotation is shorter (or they are equals)
            lastRotation + forward
        }
        
        setLastRotation(newRotation)
    }
    
    val angle: Float by animateFloatAsState(
        targetValue = -newRotation.toFloat(),
        animationSpec = tween(
            durationMillis = UPDATE_FREQUENCY,
            easing = LinearEasing
        )
    )
    
    if (direction != null)
    {
        val drawable = AppCompatResources.getDrawable(LocalContext.current,
            R.drawable.shape_pointerdot) //R.drawable.ic_pointerdot
        CompassDirectionPointer(
            angle = angle + direction.toFloat(),
            //pointerIcon = R.drawable.ic_pointerdot,
            painter = rememberDrawablePainter(drawable = drawable),
            contentDsc = R.string.destination_direction
        )
    }
    
    CompassDirectionPointer(
        painter = painterResource(id = R.drawable.ic_line),
        contentDsc = R.string.phone_direction
    )
    //Rose(angle = angle, rotation = rotation)
    CompassRose(animatedAzimuth, modifier = Modifier.padding(COMPASS_ROSE_PADDING).fillMaxSize())
}

// region previews

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithDirection()
{
    ThemedPreview {
        Compass(direction = 45, rotation = -85)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithDirectionDark()
{
    ThemedPreview(
        darkTheme = true
    ) {
        Compass(direction = 45, rotation = -85)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCompassWithoutDirection()
{
    ThemedPreview {
        Compass(direction = null, rotation = -85)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewCompassWithoutDirectionDark()
{
    ThemedPreview(
        darkTheme = true
    ) {
        Compass(direction = null, rotation = -85)
    }
}

// endregion