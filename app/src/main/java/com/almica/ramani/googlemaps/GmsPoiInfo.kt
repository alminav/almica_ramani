package com.almica.ramani.googlemaps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber

@Composable
fun GmsPoiInfo(place: MapUtils.PoiInfo) {
    val resources = LocalResources.current
    Timber.i("place.name: ${place.name}")
    val name = place.name
    val address = place.formattedAddress
    val gmsUri = place.googleMapsUri
    Box {
        Box(
            Modifier
                .align(alignment = Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(10))
        ) {
            Column(
                modifier = Modifier
                    .background(colorResource(R.color.white))
                    .padding(6.dp)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    text = name,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    text = address,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                val annotatedLinkString: AnnotatedString =
                    buildAnnotatedString {
                        val styleCenter = SpanStyle(
                            color = Color(0xff64B5F6),
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.Underline
                        )

                        withLink(LinkAnnotation.Url(url = gmsUri)) {
                            Timber.i("gmsUri: $gmsUri")
                            withStyle(
                                style = styleCenter
                            ) {
                                append(resources.getString(R.string.show_on_gms))
                            }
                        }
                    }
                Column {
                    Text(
                        annotatedLinkString,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GmsPoiInfoPreview() {
    RamaniTheme {
        val samplePoi = MapUtils.PoiInfo(
            name = "Eiffel Tower",
            latLng = LatLng(48.8584, 2.2945),
            formattedAddress = "Champ de Mars, 5 Av. Anatole France, 75007 Paris, France",
            googleMapsUri = "https://maps.google.com/?cid=123456789"
        )
        GmsPoiInfo(place = samplePoi)
    }
}
