package com.almica.ramani

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.almica.ramani.utils.DistComparatorCenter
import com.almica.ramani.utils.FeatureItem
import com.almica.ramani.utils.NameComparator
import com.almica.ramani.utils.formatDistM
import com.almica.ramani.utils.isNotNull
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import java.util.Collections

private const val logtag = "ListFeaturesScreen"
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListFeaturesScreen(
    latLng: LatLng?,
    features: List<FeatureItem>?,
    onDismissRequest: () -> Unit,
    onItemClick: (FeatureItem) -> Unit,
    featuresReady: (Boolean) -> Unit
) {
    Timber.i("features: ${features?.size}")
    val geoJsonFeatures = ArrayList<FeatureItem>()
    if (features != null) {
        geoJsonFeatures.addAll(features)
    }
    var sortOrder by remember { mutableIntStateOf(0) }
    var onlyRoutes by remember { mutableStateOf(false) }
    //sortOrder = featuresSortOrder
    var featuresGrouped = geoJsonFeatures.groupBy { it.poicatText }
    featuresReady(true)
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.92f),
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        ),
        shape = RoundedCornerShape(20.dp),
        onDismissRequest = {
            onDismissRequest()
        }, confirmButton = {
            TextButton(onClick = {
                onlyRoutes = onlyRoutes.not()
                Timber.i("onlyRoutes: $onlyRoutes")
            }) {
                Text(text = if (onlyRoutes) "${stringResource(R.string.routes_only)} ${Const.UC_CHECKMARK}"
                    else stringResource(R.string.routes_only))
            }
        }, dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(text = stringResource(R.string.uc_close))
            }
        }, title = {
            Column {
                Text(
                    text = stringResource(R.string.features_sorted_by),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .fillMaxWidth(),
                    //.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (type in 0 until 3) {
                        OutlinedButton(
                            onClick = {
                                Timber.i("sortOrder: $type")
                                sortOrder = type
                            }, border = ButtonDefaults.outlinedButtonBorder(true)
                                .takeIf { sortOrder == type }
                        ) {
                            Text(
                                text = when (type) {
                                    1 -> stringResource(R.string.name)
                                    2 -> stringResource(R.string.category)
                                    0 -> stringResource(R.string.distance)
                                    else -> {
                                        "???"
                                    }
                                }.uppercase(), fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }, text = {
            //Timber.i("sortOrder $sortOrder")
            when (sortOrder) {
                0 -> Collections.sort(
                    geoJsonFeatures,
                    DistComparatorCenter(latLng)
                )

                1 -> Collections.sort(geoJsonFeatures, NameComparator())
                2 -> {
                    Collections.sort(geoJsonFeatures, NameComparator())
                    featuresGrouped = geoJsonFeatures.groupBy { it.poicatText }
                }
            }
            if (sortOrder == 2) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    featuresGrouped.forEach { (initial, geoJsonFeatures) ->
                        stickyHeader {
                            Column(
                                modifier = Modifier
                                    .height(24.dp)
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                            ) {
                                Text(
                                    text = initial.toString(),
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        items(count = geoJsonFeatures.size) { index ->
                            Timber.i("onlyRoutes: $onlyRoutes")
                            val isRoute = geoJsonFeatures[index].region.isNotNull()
                            val includeItem = (isRoute && onlyRoutes) || onlyRoutes.not()
                            if (includeItem)
                                FeatureGeojsonItem(geoJsonFeatures[index], latLng) { featureItem ->
                                    featureItem.let {
                                        Timber.i("${it.name.toString()} " +
                                                "cat: ${it.poicatText} region: ${it.region}")
                                        onItemClick(featureItem)
                                    }
                                }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(count = geoJsonFeatures.size) { index ->
                        val isRoute = geoJsonFeatures[index].region.isNotNull()
                        val includeItem = (isRoute && onlyRoutes) || onlyRoutes.not()
                        if (includeItem)
                            FeatureGeojsonItem(geoJsonFeatures[index], latLng) { featureItem ->
                                Timber.i("${featureItem.name}")
                                featureItem.let {
                                    Timber.i("${it.name.toString()} cat: ${it.poicatText} region: ${it.region}")
                                    onItemClick(featureItem)
                                }
                            }
                    }
                }
            }
        })
}

@Composable
fun FeatureGeojsonItem(
    featureItem: FeatureItem,
    latLng: LatLng?,
    onItemClick: (FeatureItem) -> Unit
) {
    Box(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxSize()
            .clickable { onItemClick(featureItem) }
    ) {
        Column {
            val dist = latLng?.let {
                SphericalUtil.computeDistanceBetween(it, LatLng(
                    featureItem.lat,
                    featureItem.lon
                ))
            }
            val textDist = dist?.formatDistM(true)
            val heading = latLng?.let {
                SphericalUtil.computeHeading(
                    it, LatLng(
                        featureItem.lat,
                        featureItem.lon
                    )
                )
            }
            var textHeading = heading?.let { Helpers.getArrowDirection(it, LocalContext.current) }
            if (dist != null) {
                if (dist < 50)
                    textHeading = Const.UC_DISTANCE_ARROW
            }
            //com.almica.common.Helpers.formatDistM(cityEntities[position].distance, true)
            val drawableId = featureItem.drawableId
            Row(
                verticalAlignment = Alignment.CenterVertically) {
                Timber.i("${featureItem.name} " +
                        "region: ${featureItem.region} cat: ${featureItem.poicatText} color: ${featureItem.color}")
                if (drawableId != null && drawableId >= 0)
                    Icon(painterResource(drawableId), contentDescription = featureItem.poicatText,
                        modifier = Modifier.size(24.dp, 24.dp))
                else
                    Icon(painterResource(R.drawable.ic_empty_24), null,
                        modifier = Modifier.size(24.dp, 24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (featureItem.region.isNotNull()) "${featureItem.name} ${featureItem.routeDistance}"
                                else {if (featureItem.enabled) "${featureItem.name.toString()} ${Const.UC_CHECKMARK}"
                                        else featureItem.name.toString()},
                    color = if (featureItem.color != null) Color(featureItem.color!!) else Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$textHeading $textDist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

