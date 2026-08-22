package com.almica.ramani.filepicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.almica.ramani.R
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File

class FileImportActivity : ComponentActivity() {

    private val viewModel: FileImportViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initializeFromIntent(intent)

        setContent {
            RamaniTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                
                // Launcher for general documents
                val docLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let { viewModel.onFileSelected(it) }
                }

                // Launcher for media (photo picker)
                val mediaLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    uri?.let { viewModel.onFileSelected(it) }
                }

                // Initial launcher trigger
                var hasLaunchedInitial by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!hasLaunchedInitial) {
                        if (viewModel.fileDirectDownloadUrl != null) {
                            viewModel.onDirectDownload()
                        } else {
                            if (viewModel.selectedFileType == FileType.RouteThumbnail) {
                                mediaLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } else {
                                docLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                            }
                        }
                        hasLaunchedInitial = true
                    }
                }

                // Observe snackbar messages
                val popupMsg = viewModel.popupSnackMsg
                LaunchedEffect(popupMsg) {
                    popupMsg?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.dismissPopup()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddings ->
                    Column(
                        Modifier
                            .padding(paddings)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val importTitle = getImportTitle(viewModel.selectedFileType)
                        TextInsideBoxScreen(importTitle)

                        AnimatedVisibility(
                            visible = viewModel.processState,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                                Text(
                                    text = "${viewModel.filename} …",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.width(30.dp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }

                    viewModel.showImportedFile?.let { result ->
                        ModalBottomSheet(onDismissRequest = {
                            viewModel.showImportedFile = null
                            setResult(RESULT_OK)
                            finish()
                        }) {
                            ImportResultContent(result)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun getImportTitle(selectedFileType: FileType): String {
        val baseTitle = stringResource(R.string.import_title)
        return when (selectedFileType) {
            FileType.GeoJson -> "$baseTitle ${selectedFileType.name}"
            FileType.Route -> {
                val rootRouteFolder = File(LocalContext.current.filesDir, com.almica.ramani.Const.ROUTEFOLDER)
                val routeFolderExtraName = intent.getStringExtra(com.almica.ramani.Const.EXTRA_ROUTEFOLDER)
                
                routeFolderExtraName?.let {
                    val routeFolder = File(rootRouteFolder, it)
                    "$baseTitle ${selectedFileType.name} [${routeFolder.name}]"
                } ?: "$baseTitle ${selectedFileType.name}"
            }
            else -> "$baseTitle {${selectedFileType.name}}"
        }
    }

    @Composable
    private fun ImportResultContent(result: SaveFileResult) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (result.success) stringResource(R.string.file_imported_successfully)
                else stringResource(R.string.file_imported_failed)
            )
            Text(text = result.filename)
            Text(text = "${result.bytesCount} bytes")
            if (result.filesCount > 0) {
                Text(text = "${result.filesCount} files")
            }
        }
    }

    data class SaveFileResult(val filename: String, val bytesCount: Long, val success: Boolean, val filesCount: Int)

    companion object {
        fun getIntent(
            context: Context,
            fileType: FileType,
            routeFolder: String? = null,
            directDownloadUrl: String? = null
        ): Intent {
            return Intent(context, FileImportActivity::class.java).apply {
                putExtra(Const.EXTRA_FILETYPE, fileType.name)
                routeFolder?.let { putExtra(com.almica.ramani.Const.EXTRA_ROUTEFOLDER, it) }
                directDownloadUrl?.let { putExtra(Const.EXTRA_DIRECT_DOWNLOAD_URL, it) }
            }
        }

        fun launch(
            context: Context,
            fileType: FileType,
            routeFolder: String? = null,
            directDownloadUrl: String? = null
        ) {
            context.startActivity(getIntent(context, fileType, routeFolder, directDownloadUrl))
        }
    }
}

@Composable
fun TextInsideBoxScreen(title: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.border(width = 2.dp, color = Color.Blue, shape = RectangleShape)
    ) {
        Text(
            text = title,
            color = Color.Blue,
            fontWeight = FontWeight.W800,
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 10.dp)
        )
    }
}
