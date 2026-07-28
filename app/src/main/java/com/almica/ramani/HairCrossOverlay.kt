package com.almica.ramani

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.offsetYByPercent

@Composable
fun HairCrossOverlay(
    modifier: Modifier = Modifier,
    visible: Boolean,
    hairCrossOffsetFraction: Float,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 800)),
        exit = fadeOut(animationSpec = tween(durationMillis = 800))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                colors = ButtonDefaults.buttonColors(Color.Transparent),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = modifier.offsetYByPercent(hairCrossOffsetFraction),
                onClick = onClick
            ) {
                Text(
                    "+",
                    fontSize = 40.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HairCrossOverlayPreview() {
    RamaniTheme {
        HairCrossOverlay(
            visible = true,
            hairCrossOffsetFraction = 0.0f,
            onClick = {}
        )
    }
}
