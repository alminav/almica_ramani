package com.almica.ramani

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.ui.theme.RamaniTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.utils.BackPressHandler
import com.almica.ramani.utils.GhHelper
import com.almica.ramani.utils.MagentaCloud
import com.almica.ramani.utils.MagentaCloudDownloader
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import androidx.compose.ui.platform.LocalResources


/**
 * replaced by ListGhFolderWithSwipe
 */
@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGhScreen(
    viewModel: ListGhViewModel = viewModel(),
    latlng: org.maplibre.android.geometry.LatLng?,
    selectGhFolder: (Pair<String, String>) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val downloader: MagentaCloudDownloader = remember { MagentaCloudDownloader(context) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    fun startDownloadArchive(
        fileName: String,
        link: String,
        onProcess: suspend (File) -> Unit
    ) {
        Timber.i("Start download of $fileName")
        scope.launch {
            try {
                isDownloading = true
                downloadMessage = resources.getString(R.string.download_running_, fileName)
                val cacheFile = File(context.cacheDir, fileName)
                val downloadedFile = downloader.downloadFile(link, cacheFile)

                if (downloadedFile != null) {
                    Timber.i("Download successful: ${downloadedFile.absolutePath}")
                    downloadMessage = resources.getString(R.string.download_success, downloadedFile.name)

                    try {
                        onProcess(downloadedFile)
                        viewModel.refreshFolders()
                        //onGhFoldersRefresh()
                    } catch (e: Exception) {
                        Timber.e(e, "Processing failed for $fileName")
                        downloadMessage = resources.getString(R.string.processing_failed)
                    } finally {
                        val bCleanup = downloadedFile.delete()
                        Timber.i("Cleanup: $bCleanup ${downloadedFile.path}")
                    }
                } else {
                    Timber.e("Download failed.")
                    downloadMessage = resources.getString(R.string.download_failed)
                }
            } finally {
                isDownloading = false
            }
        }
    }

    fun startGhzDownload(fileName: String, link: String) = startDownloadArchive(fileName, link) { file ->
        // mapsforge_compose uses externalFilesDir instead of filesDir for gh and it works
        val ghRootDir = context.filesDir?.resolve(Const.GH_ROOT_FOLDER)
        if (ghRootDir != null) {
            GhHelper.unzipGhFile(context, Uri.fromFile(file), ghRootDir)
        }
    }
    if (isDownloading || downloadMessage != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloading) downloadMessage = null },
            title = { Text(if (isDownloading) "Download läuft" else "Download Status") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                    downloadMessage?.let { Text(it) }?: Text("Bitte warten ...")
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    TextButton(onClick = { downloadMessage = null }) {
                        Text("OK")
                    }
                }
            }
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val tileName =
        latlng?.let { Helpers.getTileName(org.maplibre.android.geometry.LatLng(it.latitude, it.longitude)) }
    BackPressHandler {
        Timber.i("Back Press intercepted")
        selectGhFolder(Pair("", ""))
    }

    ListGhScreenContent(
        ghFolders = uiState.ghFolders,
        prefGhFolderName = uiState.prefGhFolderName,
        tileName,
        onBack = { selectGhFolder(Pair("", "")) },
        onRefresh = { viewModel.refreshFolders() },
        onImport = {
            FileImportActivity.launch(context, FileType.GhFolderZip)
        },
        onSelectGhFolder = { path, name ->
            viewModel.selectFolder(path, name)
            selectGhFolder(Pair(path, name))
        }, onDownload = {filename, link ->
            startGhzDownload(filename, link)
        }, onDeleteFolder = { path ->
            viewModel.deleteFolder(path)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGhScreenContent(
    ghFolders: List<File>,
    prefGhFolderName: String?,
    tileName: String? = null,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onImport: () -> Unit,
    onDownload: (String, String) -> Unit,
    onSelectGhFolder: (path: String, name: String) -> Unit,
    onDeleteFolder: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val magentaCloudUrl = MagentaCloud.gh[tileName + "3d.ghz"]
    // masforge_compose use externalFilesDir instead of filesDir for gh and it works
    val ghRootFolder = LocalContext.current.filesDir?.resolve(Const.GH_ROOT_FOLDER)
    val ghFolder = tileName?.let { ghRootFolder?.resolve(it+"3d") }
    Timber.i("ghFolder: $ghFolder")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.gh_folders))
                }, actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh list")
                    }
                    if (magentaCloudUrl != null && ghFolder?.exists() != true) {
                        IconButton(onClick = { onDownload(tileName + "3d.ghz", magentaCloudUrl) }) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = "Download folder archive"
                            )
                        }
                    }
                    IconButton(onClick = onImport) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = "Import folder archive")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        ListGhFolder(
            modifier = Modifier.padding(paddingValues),
            ghFolders = ghFolders,
            prefGhFolderName = prefGhFolderName,
            onSelectGhFolder = onSelectGhFolder,
            onDeleteFolder = { path ->
                onDeleteFolder(path)
            }
        )
    }
}

@Composable
fun ListGhFolder(
    modifier: Modifier = Modifier,
    ghFolders: List<File>,
    prefGhFolderName: String?,
    onSelectGhFolder: (path: String, name: String) -> Unit,
    onDeleteFolder: (path: String) -> Unit
) {
    Timber.i("ghFolders: ${ghFolders.size}")
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = Margin.horizontal,
                vertical = Margin.vertical
            )
    ) {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ghFolders) { folder ->
                GhFolderItem(
                    ghName = folder.name,
                    ghPath = folder.path,
                    isSelected = folder.name == prefGhFolderName,
                    onItemClick = onSelectGhFolder,
                    onDeleteClick = { onDeleteFolder(folder.path) }
                )
            }
        }
    }
}

@Composable
fun GhFolderItem(
    ghName: String,
    ghPath: String,
    isSelected: Boolean,
    onItemClick: (path: String, name: String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { onItemClick(ghPath, ghName) }
            ) {
                Text(
                    text = ghName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(onClick = onDeleteClick, modifier = Modifier.weight(0.2f)) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.padding(horizontal = 8.dp).weight(0.2f)
                )
            } else {
                Spacer(modifier = Modifier.weight(0.2f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListGhScreenPreview() {
    val sampleFolders = listOf(File("Europe"), File("Asia"), File("North-America"))

    RamaniTheme {
        ListGhScreenContent(
            ghFolders = sampleFolders,
            prefGhFolderName = "Europe",
            onBack = {},
            onRefresh = {},
            onImport = {},
            onDownload = {_, _ -> },
            onSelectGhFolder = { _, _ -> },
            onDeleteFolder = {_ ->}
        )
    }
}
