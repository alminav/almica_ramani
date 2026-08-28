package com.almica.ramani.pdfcreator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.icu.text.SimpleDateFormat
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.R
import com.almica.ramani.routes.drawRouteName
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.routes.drawLastPageIndicator
import com.almica.ramani.ui.theme.RamaniTheme
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), routeFolderExtraName: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingFileName by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = GetMultipleContents(),
        onResult = { uris ->
            viewModel.onImagesSelected(uris)
        }
    )

    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { viewModel.writeToSelectedPath(it, context, pendingFileName) }
        }
    )

    val state by viewModel.state.collectAsState()
    val imageUris = state.imageUris
    val isLoading = state.isLoading
    val success = state.success

    LaunchedEffect(key1 = routeFolderExtraName) {
        if (routeFolderExtraName != null) {
            val routeImages = getAllRouteImagesWithSeparateName(context, routeFolderExtraName)
            viewModel.onRouteFolderSelected(routeImages, routeFolderExtraName, context)
        }
    }

    val successMsg = stringResource(R.string.pdf_success)
    val errorMsg = stringResource(R.string.pdf_creator_error)

    LaunchedEffect(key1 = success) {
        success?.let {
            val message = if (it) successMsg else errorMsg
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        MainScreenContent(
            imageUris = imageUris,
            isLoading = isLoading,
            modifier = Modifier.padding(paddingValues),
            onRemoveImage = { viewModel.removeImage(it) },
            onSelectImagesClick = { galleryLauncher.launch("image/*") },
            onSaveToFolderClick = {
                val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG, Locale.getDefault())
                val textTime = timeFormat.format(System.currentTimeMillis())
                pendingFileName = "${routeFolderExtraName ?: "export"}_${textTime}"
                openDirectoryLauncher.launch(null)
            }
        )
    }
}

@Composable
fun MainScreenContent(
    imageUris: List<Uri>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onRemoveImage: (Int) -> Unit,
    onSelectImagesClick: () -> Unit,
    onSaveToFolderClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (imageUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(imageUris) { index, uri ->
                    ImagePreviewItem(
                        uri = uri,
                        modifier = Modifier.fillMaxHeight(),
                        onRemoveClick = { onRemoveImage(index) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.pdf_select_some_images),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSelectImagesClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.pdf_select_images))
            }

            Button(
                enabled = imageUris.isNotEmpty(),
                onClick = onSaveToFolderClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(stringResource(R.string.pdf_save_to_folder))
            }
        }
    }

    if (isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ImagePreviewItem(uri: Uri, onRemoveClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRemoveClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(text = stringResource(R.string.pdf_remove))
        }
    }
}

fun getAllRouteImagesWithSeparateName(context: Context, region: String): List<Bitmap> {
    val routeImages = ArrayList<Bitmap>()
    val routePaths = ArrayList<String>()
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val regionRouteFolder = File(rootRouteFolder, region)
    val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
    if (!regionRouteFolder.exists()) return emptyList()

    regionRouteFolder.listFiles()?.sortedBy { it.name }?.forEach { routeFile ->
        if (routeFile.exists() && routeFile.isFile && routeFile.name.endsWith(Const.JPG_EXT)) {
            routePaths.add(routeFile.path)
        } else {
            val thumbnailFile = File(thumbnailFolder, routeFile.nameWithoutExtension + Const.JPG_EXT)
            if (thumbnailFile.exists() && thumbnailFile.isFile) {
                routePaths.add(thumbnailFile.path)
            }
        }
    }

    routePaths.forEach { routePath ->
        val routeFile = File(routePath)
        val description = Helpers.getImageDescriptionFromExif(routeFile)?.substringBefore(" ")

        val thumbnail = BitmapFactory.decodeFile(routeFile.path) ?: return@forEach
        
        // Header Bitmap
        val bmpName = createBitmap(thumbnail.width, 32)
        val nameCanvas = Canvas(bmpName)
        nameCanvas.drawColor(android.graphics.Color.WHITE)
        drawRouteName(context, nameCanvas, routeFile.nameWithoutExtension, textSize = 20f)
        routeImages.add(bmpName)
        
        // Image Bitmap
        val bmp = createBitmap(thumbnail.width, thumbnail.height + 30)
        val imgCanvas = Canvas(bmp)
        imgCanvas.drawColor(android.graphics.Color.WHITE)
        imgCanvas.drawBitmap(thumbnail, 0f, 0f, null)
        description?.let { name -> drawRouteName(context, imgCanvas, name, textSize = 20f) }
        routeImages.add(bmp)
        
        thumbnail.recycle()
    }
    return routeImages
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RamaniTheme {
        MainScreenContent(
            imageUris = emptyList(),
            isLoading = false,
            onRemoveImage = {},
            onSelectImagesClick = {},
            onSaveToFolderClick = {}
        )
    }
}
