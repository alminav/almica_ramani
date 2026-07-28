package com.almica.ramani.compass

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.R
import com.almica.ramani.compass.theme.ThemedPreview
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun CompassDirectionPointer(
    painter: Painter,
    @StringRes contentDsc: Int,
    modifier: Modifier = Modifier,
    angle: Float = 0f,
)
{
    Image(
        modifier = modifier
            .padding(COMPASS_PADDING)
            .rotate(angle)
            .fillMaxSize(),
        painter = painter,
        contentDescription = stringResource(id = contentDsc),
        contentScale = ContentScale.Fit,
    )
}

@Preview(showBackground = true)
@Composable
fun CompassDirectionPointerPreview() {
    ThemedPreview {
        CompassDirectionPointer(
            painter = painterResource(id = R.drawable.ic_line),
            contentDsc = R.string.phone_direction,
            angle = 45f
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompassDirectionPointerDarkPreview() {
    ThemedPreview(darkTheme = true) {
        CompassDirectionPointer(
            painter = painterResource(id = R.drawable.ic_line),
            contentDsc = R.string.phone_direction,
            angle = 45f
        )
    }
}
