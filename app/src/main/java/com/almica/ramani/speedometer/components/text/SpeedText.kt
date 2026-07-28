package com.almica.ramani.speedometer.components.text

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * @author Anas Altair
 */
@Composable
fun SpeedText(
    modifier: Modifier = Modifier.background(Color.White),
    speed: Float,
    style: TextStyle = TextStyle.Default.copy(fontSize = 20.sp).copy(fontWeight = FontWeight.Bold)
) {
    BasicText(
        modifier = modifier,
        text = speed.roundToInt().toString(),
        style = style,
    )
}
