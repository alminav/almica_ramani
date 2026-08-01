package com.almica.ramani.googlemaps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.almica.ramani.R
import com.almica.ramani.charts.theme.Teal200

/**
 * A reusable control button for the map UI with consistent styling.
 * Complies with accessibility guidelines by using a 48dp touch target.
 */
@Composable
fun MapControlButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // Accessibility: Standard touch target size
            .border(1.dp, Teal200, RectangleShape)
            .background(colorResource(R.color.teal_200_trans))
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}
