package com.almica.ramani.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.pdf.PdfDocument
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.almica.ramani.Const
import com.almica.ramani.R
import com.almica.ramani.RouteInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

// Source - https://stackoverflow.com/a/79247047
// Posted by BenjyTec, modified by community. See post 'Timeline' for change history
// Retrieved 2026-06-12, License - CC BY-SA 4.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewer(
    finish: () -> Unit,
    routeDataSelection: (RouteInfo) -> Unit,
    viewModel: DocumentViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = getDefaultSharedPreferences(context)
    val prefRouteFolder = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, null)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initialize(context, prefRouteFolder)
    }

    BackPressHandler {
        Timber.i("Back Press intercepted")
        finish()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        it?.let { uri -> viewModel.handleSelectedTree(context, uri, prefRouteFolder) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val rootUri = viewModel.documentUri ?: return@rememberLauncherForActivityResult
        viewModel.importFiles(context, uris, rootUri) {
            finish()
        }
    }

    val pdfViewerState = remember { PdfViewerState() }

    LaunchedEffect(viewModel.popupSnackMsg) {
        viewModel.popupSnackMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.popupSnackMsg = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = viewModel.displayName ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.pdfDocument?.let { doc ->
                            coroutineScope.launch {
                                pdfViewerState.scrollToPage(doc.pageCount - 1)
                            }
                        }
                    }) {
                        Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Scroll to bottom")
                    }
                    IconButton(onClick = {
                        viewModel.pdfDocument?.let { _ ->
                            coroutineScope.launch {
                                pdfViewerState.scrollToPage(0)
                            }
                        }
                    }) {
                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "Scroll to top")
                    }
                    IconButton(onClick = { viewModel.menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = viewModel.menuExpanded,
                        onDismissRequest = { viewModel.menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_pdf_geojson)) },
                            onClick = {
                                viewModel.menuExpanded = false
                                importLauncher.launch("*/*")
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                viewModel.menuExpanded = false
                                viewModel.shareFiles(context)
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Routes") },
                            onClick = {
                                viewModel.menuExpanded = false
                                viewModel.exportRoutes(context, prefRouteFolder)
                            },
                            leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Snapshots") },
                            onClick = {
                                viewModel.menuExpanded = false
                                viewModel.exportSnapshots(context, prefRouteFolder)
                            },
                            leadingIcon = { Icon(Icons.Default.Preview, contentDescription = null) })
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LaunchedEffect(Unit) {
                delay(100.milliseconds)
                if (viewModel.documentUri == null) {
                    launcher.launch(null)
                }
            }

            LaunchedEffect(viewModel.geojsonText) {
                if (viewModel.geojsonText.isNotEmpty()) {
                    viewModel.parseGeoJsonAndBuildRouteMap()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                viewModel.routeInfo?.let { info ->
                    TextButton(modifier = Modifier.weight(1f), onClick = {
                        routeDataSelection(info)
                        Timber.i(info.name)
                    }) {
                        Text(text = info.name ?: "", textAlign = TextAlign.Center)
                    }
                }
            }

            viewModel.pdfDocument?.let { document ->
                PdfViewerContainer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    pdfViewerState = pdfViewerState,
                    pdfDocument = document,
                    onRouteInfoSelected = { info ->
                        viewModel.routeInfo = info
                    }
                )
            }

            viewModel.progressMsg?.let {
                ProgressPopup(it)
            }
        }
    }
}

@Composable
fun PdfViewerContainer(
    modifier: Modifier = Modifier,
    pdfViewerState: PdfViewerState,
    pdfDocument: PdfDocument,
    onRouteInfoSelected: (RouteInfo) -> Unit
) {
    val viewModel: DocumentViewerViewModel = viewModel()

    LaunchedEffect(viewModel.routeMapIsReady) {
        if (viewModel.routeMapIsReady != 0L) {
            delay(100.milliseconds)
            pdfViewerState.scrollToPage(2)
        }
    }

    LaunchedEffect(pdfViewerState, viewModel.routeMap) {
        snapshotFlow { pdfViewerState.firstVisiblePage }
            .collectLatest { pageIndex ->
                viewModel.routeMap[pageIndex]?.let { namedRoute ->
                    onRouteInfoSelected(
                        RouteInfo(
                            name = namedRoute.name,
                            formattedDistance = namedRoute.points.getDistanceFromLllh().formatDistM(true),
                            points = namedRoute.points
                        )
                    )
                }
            }
    }

    PdfViewer(
        pdfDocument = pdfDocument,
        state = pdfViewerState,
        modifier = modifier
    )
}

@Composable
fun ProgressPopup(progressMsg: String?) {
    Popup(properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        alignment = Alignment.Center,
        onDismissRequest = {
        }) {
        Surface(
            color = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(0.5f),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(text = progressMsg?: stringResource(R.string.loading), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.inverseSurface)
                    CircularProgressIndicator()
                }
            }
        }
    }
}
@Composable
fun ProgressDialog() {
    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        confirmButton = {},
        title = { Text(stringResource(R.string.loading)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    )
}

@ComposePreview(showBackground = true)
@Composable
fun DocumentViewerPreview() {
    DocumentViewer(
        finish = {},
        routeDataSelection = { }
    )
}

