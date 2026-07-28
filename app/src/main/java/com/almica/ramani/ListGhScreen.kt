package com.almica.ramani

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import timber.log.Timber
import java.io.File


/**
 * replaced by ListGhFolderWithSwipe
 */
@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGhScreen(
    viewModel: ListGhViewModel = viewModel(),
    selectGhFolder: (name: Pair<String, String>) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    BackPressHandler {
        Timber.i("Back Press intercepted")
        selectGhFolder(Pair("", ""))
    }

    ListGhScreenContent(
        ghFolders = uiState.ghFolders,
        prefGhFolderName = uiState.prefGhFolderName,
        onBack = { selectGhFolder(Pair("", "")) },
        onRefresh = { viewModel.refreshFolders() },
        onDelete = { viewModel.deleteSelectedFolder() },
        onImport = {
            context.startActivity(
                Intent(context, FileImportActivity::class.java)
                    .setAction(context.getString(R.string.import_title))
                    .putExtra(Const.EXTRA_FILETYPE, FileType.GhFolderZip.name)
            )
        },
        onSelectGhFolder = { path, name ->
            viewModel.selectFolder(path, name)
            selectGhFolder(Pair(path, name))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListGhScreenContent(
    ghFolders: List<File>,
    prefGhFolderName: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onImport: () -> Unit,
    onSelectGhFolder: (path: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete selected folder")
                    }
                    IconButton(onClick = onImport) {
                        Icon(Icons.Outlined.ImportExport, contentDescription = "Import folder")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        ListGhFolder(
            modifier = Modifier.padding(paddingValues),
            ghFolders = ghFolders,
            prefGhFolderName = prefGhFolderName,
            onSelectGhFolder = onSelectGhFolder
        )
    }
}

@Composable
fun ListGhFolder(
    modifier: Modifier = Modifier,
    ghFolders: List<File>,
    prefGhFolderName: String?,
    onSelectGhFolder: (path: String, name: String) -> Unit
) {
    Timber.i("ghFolders: ${ghFolders.size}")
    
    Column(
        modifier = modifier.padding(
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
                    onItemClick = onSelectGhFolder
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
    onItemClick: (path: String, name: String) -> Unit
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

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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
            onDelete = {},
            onImport = {},
            onSelectGhFolder = { _, _ -> }
        )
    }
}
