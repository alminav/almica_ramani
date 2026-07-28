package com.almica.ramani.pdfcreator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.icu.text.SimpleDateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.almica.ramani.Const
import com.almica.ramani.Helpers
import com.almica.ramani.routes.drawRouteName
import androidx.compose.ui.tooling.preview.Preview
import com.almica.ramani.routes.drawLastPageIndicator
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel(), routeFolderExtraName: String?) {
    Timber.i( "routeFolderExtraName: $routeFolderExtraName")
    val context = LocalContext.current
    var pendingFileName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = GetMultipleContents(),
        onResult = {
            viewModel.onImagesSelected(it, context)
        }
    )

    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = OpenDocumentTree(),
        onResult = { uri ->
            uri?.let { viewModel.writeToSelectedPath(it, context, pendingFileName) }
        }
    )

    val state by viewModel.state.collectAsState()
    val imageBitmaps = state.imageBitmaps
    val isLoading = state.isLoading
    val success = state.success
    Timber.i("isLoading: $isLoading")


    LaunchedEffect(key1 = routeFolderExtraName) {
        if (routeFolderExtraName != null) {
            val routeImages = getAllRouteImagesWithSeparateName(context, routeFolderExtraName)
            Timber.i("routeImages: ${routeImages.size}")
            viewModel.onRouteFolderSelected(routeImages, routeFolderExtraName, context)
        }
    }

    LaunchedEffect(key1 = success ) {
        if (success != null){
            if (success) {
                Toast.makeText(
                    context,
                    "Successfully converted images to pdf",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Something went wrong",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    MainScreenContent(
        imageBitmaps = imageBitmaps,
        isLoading = isLoading,
        onRemoveImage = { viewModel.removeImage(it) },
        onSelectImagesClick = { galleryLauncher.launch("image/*") },
        onSaveToFolderClick = {
            val timeFormat = SimpleDateFormat(Const.TIME_PATTERN_LONG, Locale.getDefault())
            val textTime: String? = timeFormat.format(System.currentTimeMillis())
            pendingFileName = "${routeFolderExtraName}_${textTime}"
            openDirectoryLauncher.launch(null)
        }
    )
}

@Composable
fun MainScreenContent(
    imageBitmaps: List<Bitmap>,
    isLoading: Boolean,
    onRemoveImage: (Int) -> Unit,
    onSelectImagesClick: () -> Unit,
    onSaveToFolderClick: () -> Unit,
) {
    Timber.i("isLoading: $isLoading imageBitmaps: ${imageBitmaps.size}")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        if (imageBitmaps.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight(0.7f)
            ) {
                itemsIndexed(imageBitmaps) { index: Int, bitmap: Bitmap ->
                    ImagePreviewItem(
                        bitmap = bitmap,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 15.dp),
                        onRemoveClick = {
                            onRemoveImage(index)
                        }
                    )
                }
            }
        } else Box(
            modifier = Modifier
                .fillMaxWidth().fillMaxHeight(0.7f),
            contentAlignment = Center
        ){
            Text(text = "Select some images")
        }

        Box(contentAlignment = BottomCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
        ) {
            Column(horizontalAlignment = CenterHorizontally) {

                Button(onClick = onSelectImagesClick) {
                    Text("Select images")
                }

                Button(
                    enabled = imageBitmaps.isNotEmpty(),
                    onClick = onSaveToFolderClick
                ) {
                    Text("Save to Folder")
                }
            }
        }
    }

    if (isLoading){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Center){
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                buttons = {}
            )
            CircularProgressIndicator()
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

    regionRouteFolder.let { regionFolder ->
        regionFolder.listFiles()?.sortedBy { it.name }?.forEach { routeFile ->
            if (routeFile.exists() && routeFile.isFile && routeFile.name.endsWith(Const.JPG_EXT)) {
                routePaths.add(routeFile.path)
            } else {
                val thumbnailFile =
                    File(thumbnailFolder, routeFile.nameWithoutExtension + Const.JPG_EXT)
                if (thumbnailFile.exists() && thumbnailFile.isFile && thumbnailFile.name.endsWith(
                        Const.JPG_EXT)) {
                    routePaths.add(thumbnailFile.path)
                }
            }
        }
    }
    Timber.i("routePaths: ${routePaths.size}")

    routePaths.forEach { routePath ->
        val routeFile = File(routePath)
        val description = Helpers.getImageDescriptionFromExif(routeFile)?.substringBefore(" ")

        val thumbnail = BitmapFactory.decodeFile(routeFile.path)
        // Task failed with an exception com.google.mlkit.common.MlKitException: InputImage width and height should be at least 32!
        val bmpName: Bitmap = createBitmap(thumbnail.width, 32)
        bmpName.let {
            val thumbCanvas = Canvas(it)
            thumbCanvas.drawColor(android.graphics.Color.WHITE)
            drawRouteName(context, thumbCanvas, routeFile.nameWithoutExtension, textSize = 20f)
            routeImages.add(bmpName)
        }
        val bmp: Bitmap = createBitmap(thumbnail.width, thumbnail.height + 30)
        bmp.let {
            val thumbCanvas = Canvas(it)
            thumbCanvas.drawColor(android.graphics.Color.WHITE)
            thumbCanvas.drawBitmap(thumbnail, 0f, 0f, null)
            description?.let { name -> drawRouteName(context, thumbCanvas, name, textSize = 20f) }
            routeImages.add(bmp)
        }
    }
    Timber.i("routeImages: ${routeImages.size}")
    return routeImages
}

fun getAllRouteImages(context: Context, region: String): List<Bitmap> {
    val routeImages = ArrayList<Bitmap>()
    val routePaths = ArrayList<String>()
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val regionRouteFolder = File(rootRouteFolder, region)
    val thumbnailFolder = File(context.filesDir, Const.THUMBNAILS)
    if (!regionRouteFolder.exists()) return emptyList()

    regionRouteFolder.let { regionFolder ->
        regionFolder.listFiles()?.sortedBy { it.name }?.forEach { routeFile ->
            if (routeFile.exists() && routeFile.isFile && routeFile.name.endsWith(Const.JPG_EXT)) {
                routePaths.add(routeFile.path)
            } else {
                val thumbnailFile =
                    File(thumbnailFolder, routeFile.nameWithoutExtension + Const.JPG_EXT)
                if (thumbnailFile.exists() && thumbnailFile.isFile && thumbnailFile.name.endsWith(
                        Const.JPG_EXT)) {
                    routePaths.add(thumbnailFile.path)
                }
            }
        }
    }
    Timber.i("routePaths: ${routePaths.size}")

    routePaths.forEach { routePath ->
        val routeFile = File(routePath)
        val thumbnail = BitmapFactory.decodeFile(routeFile.path)
        val bmp: Bitmap = createBitmap(thumbnail.width, thumbnail.height + 30)
        bmp.let {
            val thumbCanvas = Canvas(it)
            thumbCanvas.drawColor(android.graphics.Color.WHITE)
            thumbCanvas.drawBitmap(thumbnail, 0f, 0f, null)
            drawRouteName(context, thumbCanvas, "#${routeFile.nameWithoutExtension}#", textSize = 20f)
            routeImages.add(bmp)
        }
    }
    val bmp: Bitmap = createBitmap(512, 512)
    val dummyCanvas = Canvas(bmp)
    Timber.i("routeImages: ${routeImages.size}")
    drawLastPageIndicator(context, dummyCanvas, "LAST PAGE")
    routeImages.add(bmp)
    return routeImages
}

@Composable
fun ImagePreviewItem(bitmap: Bitmap, onRemoveClick: () -> Unit,modifier: Modifier) {

    Column(modifier = modifier){
        AsyncImage(
            model = bitmap,
            contentDescription = null,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onRemoveClick,
            colors = buttonColors(backgroundColor = Color.Red, contentColor = Color.White),
            modifier = Modifier.align(CenterHorizontally)
        ) {
            Text(text = "Remove")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val dummyBitmap = createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    RamaniTheme {
        MainScreenContent(
            imageBitmaps = listOf(dummyBitmap, dummyBitmap),
            isLoading = false,
            onRemoveImage = {},
            onSelectImagesClick = {},
            onSaveToFolderClick = {}
        )
    }
}
