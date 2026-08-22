package com.almica.ramani

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.utils.BackPressHandler
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListCyclewayTilesScreen(
    innerPadding: PaddingValues,
    viewModel: CyclewayTilesViewModel = viewModel(),
    finish: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val snackbarData by viewModel.snackbarData.collectAsState()
    val useCyclewayOverlay by viewModel.useCyclewayOverlay.collectAsState()

    LaunchedEffect(key1 = snackbarData) {
        if (snackbarData != null) {
            delay(5000.milliseconds)
            viewModel.clearSnackbar()
        }
    }

    BackPressHandler {
        finish(false)
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { finish(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                },
                title = {
                    Text(text = stringResource(R.string.cycleway_overlays), fontSize = 14.sp)
                },
                actions = {
                    IconButton(onClick = {
                        FileImportActivity.launch(context, FileType.CycleWay)
                    }) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .border(
                                1.dp, SolidColor(Color.LightGray),
                                shape = RoundedCornerShape(15.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.use_cycleway_overlays),
                            fontSize = 14.sp
                        )
                        Checkbox(
                            checked = useCyclewayOverlay,
                            onCheckedChange = { viewModel.toggleUseCyclewayOverlay(it) },
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        if (snackbarData != null) {
            MoboSnack(snackbarData!!) { viewModel.clearSnackbar() }
        }

        CyclewayListContent(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel,
            onConfirm = { finish(true) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CyclewayListContent(
    modifier: Modifier,
    viewModel: CyclewayTilesViewModel,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.overlayItemModels.collectAsState()
    val checkCount by viewModel.checkCount.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()

    Column(
        modifier = modifier.padding(
            horizontal = Margin.horizontal,
            vertical = Margin.vertical
        )
    ) {
        Row(
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.loadOverlayFiles() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(10.dp))
            AnimatedVisibility(visible = checkCount > 0) {
                BadgedBox(badge = { Badge { Text("$checkCount") } }) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = checkCount > 0) {
                IconButton(onClick = { viewModel.shareSelected(context) }) {
                    Icon(
                        Icons.Outlined.Share,
                        modifier = Modifier.padding(10.dp),
                        contentDescription = stringResource(R.string.export_title)
                    )
                }
            }
            AnimatedVisibility(visible = checkCount > 0) {
                IconButton(onClick = { viewModel.deleteSelected() }) {
                    Icon(
                        Icons.Outlined.Delete,
                        modifier = Modifier.padding(10.dp),
                        contentDescription = stringResource(R.string.remove)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            AnimatedVisibility(visible = hasChanges) {
                TextButton(onClick = {
                    viewModel.confirmChanges()
                    onConfirm()
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items, key = { _, item -> item.path }) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleOverlaySelection(index) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.name)
                        if (item.selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "selected",
                                tint = Color.Magenta,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoboSnack(
    cycleWaySnackbarData: CycleWaySnackbarData,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            Row(
                modifier = Modifier.border(
                    width = 2.dp,
                    color = Color.LightGray,
                    shape = RectangleShape
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cycleWaySnackbarData.title?.let {
                    Text(
                        text = it,
                        Modifier
                            .weight(0.8f)
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Blue
                    )
                }
                cycleWaySnackbarData.actionText?.let { text ->
                    TextButton(
                        onClick = {
                            Timber.i("Snackbar action clicked: $text")
                            onDismiss()
                        },
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Text(
                            text = text,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Blue
                        )
                    }
                }
            }
        }
    }
}

