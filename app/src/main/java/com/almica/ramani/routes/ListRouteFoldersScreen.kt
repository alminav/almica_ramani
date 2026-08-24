package com.almica.ramani.routes

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.RemoveFromQueue
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.almica.ramani.BuildConfig
import com.almica.ramani.Const
import com.almica.ramani.FolderReference
import com.almica.ramani.R
import com.almica.ramani.RouteFolder
import com.almica.ramani.filepicker.FileImportActivity
import com.almica.ramani.filepicker.FileType
import com.almica.ramani.pdfcreator.MainActivity
import com.almica.ramani.ui.theme.Margin
import com.almica.ramani.ui.theme.RamaniTheme
import com.almica.ramani.utils.BackPressHandler
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.outlined.FilterAltOff

private const val logtag: String = "ListRouteFolderScreen"

private fun createNewFolder(context: Context, folderName: String): Pair<String, String> {
    Timber.i("createNewFolder: $folderName")
    val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), folderName).apply { mkdirs() }
    return Pair(routeFolder.name, routeFolder.path)
}

/**
 * Composable screen that displays and manages route folders.
 * Leaner than RoutesManager but faster 06jun2026
 *
 * @param marginTopDp The top padding to apply to the Scaffold.
 * @param selectRouteFolder Callback invoked when a folder is selected, providing its name and path.
 * @param routeFoldersIn Optional initial list of route folder pairs (name, path).
 * @param routeFolderNameIn Optional initial name of the currently selected route folder.
 * @param finished Callback invoked when exiting the screen, returning the name of the active folder.
 * @param route Callback invoked when a specific route file within a folder is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRouteFoldersScreen(
    marginTopDp: Float,
    selectRouteFolder: (routeFolder: RouteFolder) -> Unit,
    routeFoldersIn: List<FolderReference>? = null,
    routeFolderNameIn: String? = null,
    withSearch: Boolean = true,
    finished: (String?) -> Unit,
    route: (File) -> Unit,
    routeSnapshot: (RouteFileBundle) -> Unit,
    routeInfo: (File) -> Unit,
    createSnapshots: (String?) -> Unit,
    dialogMode: Int
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    //var routeFolders by remember { mutableStateOf(routeFoldersIn ?: createRouteFolderList(context)) }
    var routeFolderListRefresh by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val routeFolders by produceState(initialValue = emptyList(), key1 = routeFolderListRefresh) {
        value = createRouteFolderList(context)
        Timber.i("routeFolders.size: ${value.size}")
    }

    var activityResultTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                activityResultTime = System.currentTimeMillis()
                Timber.i("activityResultTime: $activityResultTime")
            }
        }

    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val prefRouteFolderPath = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, "")
    val prefRouteFolderName = prefRouteFolderPath?.let { File(it).name }
    var routeFolderName by remember { mutableStateOf(routeFolderNameIn ?: prefRouteFolderName) }
    var routeFolderFileCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(routeFolderName, key2 = activityResultTime) {
        routeFolderFileCount = routeFolderName?.let {
            File(File(context.filesDir, Const.ROUTEFOLDER), it).listFiles()?.size
        } ?: 0
    }
    var newRouteFolderMode by remember { mutableStateOf(false) }
    var newRouteFolder by remember { mutableStateOf("") }
    //var removeFolderRequest by remember { mutableStateOf(false) }
    //var importRequest by remember { mutableStateOf(false) }
    var routeFilesState by remember { mutableStateOf(false) }
    var menuRouteFolder by remember { mutableStateOf<String?>(null) }
    var menuRouteFile by remember { mutableStateOf<File?>(null) }
    var confirmFilter by remember { mutableStateOf(false) }
    //val marginTopDp = TopAppBarDefaults.TopAppBarExpandedHeight.value
    //routeFolders = createRouteFolderList(context).toMutableList()
    Timber.i("routeFolders.hashCode:${routeFolders.hashCode()}")

    BackPressHandler {
        Timber.i("Back Press intercepted")
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val prefRouteFolderPath = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, "")
        val prefRouteFolderName = prefRouteFolderPath?.let { File(it).name }
        finished(prefRouteFolderName)
    }
    Scaffold(modifier = Modifier.padding(top = marginTopDp.dp),
        //Scaffold(
        floatingActionButton = {
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { selectRouteFolder(RouteFolder("", "", 0))
                            //ScreenRouter.navigateHome()
                            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                            val prefRouteFolderPath = prefs.getString(Const.PREF_ROUTEFOLDER_FILEPATH, "")
                            val prefRouteFolderName = prefRouteFolderPath?.let { File(it).name }
                            Timber.i("finished: $prefRouteFolderName")
                            finished(prefRouteFolderName)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back home"
                        )
                    }
                }, title = {
                    Text(text = stringResource(R.string.route_folders), fontSize = 14.sp)
                }, actions = {
                    if (withSearch)
                        IconButton(onClick = {
                            routeFolderName = null
                            routeFilesState = true
                            //confirmFilter = true
                        })
                        {
                            Icon(
                                Icons.Outlined.Search,
                                "search",
                                modifier = Modifier
                                    .padding(end = 10.dp, start = 10.dp)
                                    .width(60.dp)
                                    .height(60.dp)
                            )
                        }
                    if (routeFolderName != null) {
                        BadgedBox(badge = { Badge { Text("$routeFolderFileCount") } }) {
                            TextButton(
                                colors = ButtonDefaults.buttonColors(Color.Transparent),
                                border = BorderStroke(1.dp, Color.LightGray),
                                onClick = {
                                    newRouteFolderMode = false
                                    routeFilesState = true
                                }
                            ) {
                                Text(
                                    routeFolderName!!,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    IconButton(onClick = { newRouteFolderMode = !newRouteFolderMode })
                    {
                        Icon(
                            Icons.Outlined.Add,
                            "newFolder",
                            modifier = Modifier
                                .padding(end = 10.dp, start = 10.dp)
                                .width(60.dp)
                                .height(60.dp)
                        )
                    }
                })

            AnimatedVisibility(visible = newRouteFolderMode) {
                Row(modifier = Modifier.padding(top = 60.dp)) {
                    OutlinedTextField(
                        value = newRouteFolder,
                        onValueChange = { newRouteFolder = it },
                        label = { Text(stringResource(R.string.routefolder_name)) },
                        modifier = Modifier
                            .padding(start = 6.dp, end = 6.dp)
                            .fillMaxWidth(0.8f)
                    )
                    IconButton(
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        //.border(border = BorderStroke(2.dp, Color.LightGray)),
                        onClick = {
                            if (newRouteFolder.isNotEmpty()) {
                                val folder = createNewFolder(context, newRouteFolder)
                                newRouteFolder = folder.first
                                newRouteFolderMode = false
                                routeFolderListRefresh = System.currentTimeMillis()
                            }
                        }
                    ) {
                        Icon(//modifier = Modifier.align(alignment = Alignment.CenterVertically),
                            imageVector = Icons.Outlined.Done,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        var alertMsg: String? by remember { mutableStateOf(null) }
        alertMsg?.let { msg ->
            AlertDialog(
                onDismissRequest = { alertMsg = null },
                confirmButton = {
                    TextButton(onClick = { alertMsg = null }) {
                        Text("OK")
                    }
                },
                title = { Text("Alert") },
                text = { Text(msg) }
            )
        }
        var popupSnackMsg: String? by remember { mutableStateOf(null) }
        LaunchedEffect(key1 = popupSnackMsg) {
            Timber.i( "LaunchedEffect $popupSnackMsg")
            delay(5000.milliseconds)
            popupSnackMsg = null
        }
        if (routeFilesState && routeFolderName == null) {
            RouteFilesList(
                context, null,
                finish = {
                    routeFilesState = false
                },
                route = {routeFile ->
                    route(routeFile)
                    //routeFilesState = false
                },
                routeSnapshot = {
                    routeSnapshot(it)
                },
                filterChanged = {
                    //popupSnackMsg = resources.getString(R.string.filter_changed_, it)
                }, menu = { file ->
                    menuRouteFile = file
                }
            )
//            ConfirmFilterDlg("") { filter ->
//                confirmFilter = false
//                if (filter != null) {
//                    Timber.i("filter: $filter")
//                    search(filter)
//                }
//            }
        }
        popupSnackMsg?.let { msg ->
            Popup(properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                alignment = Alignment.TopCenter,
                onDismissRequest = {
                    popupSnackMsg = null
                    routeFilesState = true
                }) {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        menuRouteFile?.let { routeFile ->
            Timber.i("menuRouteFile: $routeFile")
            routeFilesState = false
            DropdownRouteFileMenu(menuRouteFile) { action ->
                when (action) {
                    RouteFileAction.Nothing -> {
                        menuRouteFile = null
                        routeFilesState = true
                    }

                    RouteFileAction.Delete -> {
                        menuRouteFile?.let { file ->
                            Timber.i("RouteFileAction.Delete: ${file.path}")
                            var deleteCount = 0
                            val name = file.nameWithoutExtension
                            val parent = file.parentFile
                            deleteCount = if (file.delete()) 1 else 0
                            parent?.listFiles { file, fileName ->
                                (fileName.contains(name)
                                        && !fileName.endsWith(Const.GEOJSON_EXT)) }?.forEach {
                                if (it.delete())
                                    deleteCount++
                            }
                            val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
                            if (File(thumbnailsFolder, file.name.plus(Const.JPG_EXT)).delete())
                                deleteCount++
                            popupSnackMsg = resources.getString(R.string.deleted_files_, deleteCount)
                            menuRouteFile = null
                        }
                    }

                    RouteFileAction.Info -> {
                        Timber.i("menuRouteFile: $menuRouteFile")
                        menuRouteFile?.let {
                            //route(it)
                            routeInfo(it)
                        }
                        menuRouteFile = null
                    }

                    RouteFileAction.Share -> {
                        menuRouteFile?.let {
                            shareRouteFile(context, it)
                        }
                        menuRouteFile = null
                        routeFilesState = true
                    }
                }
            }
        }

        menuRouteFolder?.let {
            DropdownRouteFolderMenu(dialogModeOrdinal = dialogMode, action = { action ->
                when(action) {
                    RouteFolderAction.Nothing -> { menuRouteFolder = null }
                    RouteFolderAction.TextImport -> {
                        launcher.launch(FileImportActivity.getIntent(context, FileType.Route, routeFolder = menuRouteFolder))
                        menuRouteFolder = null
                    }
                    RouteFolderAction.ImageImport -> {
                        launcher.launch(FileImportActivity.getIntent(context, FileType.RouteThumbnail, routeFolder = menuRouteFolder))
                        menuRouteFolder = null
                    }

                    RouteFolderAction.Cleanup -> {
                        menuRouteFolder?.let { region ->
                            val routeFolder = File(File(context.filesDir, Const.ROUTEFOLDER), region)
                            cleanUpRouteFolder(context, File(context.filesDir, Const.THUMBNAILS)) {deleteCount, renameCount ->
                                Timber.i("cleanUp ${Const.THUMBNAILS} deleteCount $deleteCount renameCount $renameCount")
                            }
                            cleanUpRouteFolder(context, routeFolder) {deleteCount, renameCount ->
                                alertMsg = resources.getString(R.string.changed_files_, deleteCount, renameCount)
                            }
                            menuRouteFolder = null
                        }
                    }

                    RouteFolderAction.RemoveContent -> {
                        removeRouteFolderContent(context, menuRouteFolder, onFinished = { result ->
                            alertMsg = "$menuRouteFolder deleteRecursively: $result"
                            menuRouteFolder = null
                            routeFolderListRefresh = System.currentTimeMillis()
                        })
                    }
                    RouteFolderAction.CreateSnapshots -> {
                        createSnapshots(menuRouteFolder)
                        menuRouteFolder = null
                    }

                    RouteFolderAction.RemoveSnapshots -> {
                        removeRouteFolderSnapshots(context, menuRouteFolder, onFinished = { result ->
                            alertMsg = resources.getString(R.string.deleted_files_, result)
                            Timber.i("alertMsg: $alertMsg")
                            menuRouteFolder = null
                        })
                    }

                    RouteFolderAction.CreatePdf -> {
                        launcher.launch(
                            Intent(context, MainActivity::class.java)
                                .setAction(resources.getString(R.string.pdf_creator))
                                .putExtra(Const.EXTRA_ROUTEFOLDER, menuRouteFolder)
                        )
                        menuRouteFolder = null
                    }
                }
            })
        }
        if (routeFilesState.and(routeFolderName != null)) {
            Timber.i("launch RouteFilesList routeFolderName: $routeFolderName")
            RouteFilesList(
                context, routeFolderName!!,
                finish = {
                    routeFilesState = false
                },
                route = { routeFile ->
                    Timber.i("route: $routeFile")
                    route(routeFile)
                    //routeFilesState = false
                },
                routeSnapshot = {
                    Timber.i("routeSnapshot: $it")
                    routeSnapshot(it)
                },
                filterChanged = {
                    //popupSnackMsg = resources.getString(R.string.filter_changed_, it)
                }, menu = { file ->
                    menuRouteFile = file
                }
            )
        }
        ListRouteFoldersContent(Modifier.padding(paddingValues), routeFolderName, routeFolders,
            selectRouteFolder = { routeFolderFeedback ->
                selectRouteFolder(routeFolderFeedback)
                routeFolderName = routeFolderFeedback.name.also { prefs.edit { putString(Const.PREF_ROUTEFOLDER_FILEPATH, it) } }
                newRouteFolderMode = false
                Timber.i("routeFolderName: $routeFolderName")
                routeFilesState = true
            }, menuRouteFolder = { folderName ->
                Timber.i("menuRouteFolder: $folderName")
                menuRouteFolder = folderName
            }, prefRouteFolderChanged = {
                routeFolderName = it.also { prefs.edit { putString(Const.PREF_ROUTEFOLDER_FILEPATH, it) } }
            }
        )
        //RouteFilesList(context, PaddingValues(), routeFolderName!!)
    }
}

fun createRouteFolderList(context: Context): List<FolderReference> {
    val routesRootFolder = File(context.filesDir, Const.ROUTEFOLDER).apply { mkdirs() }
    val fileFilter = FileFilter { file: File? -> file?.isDirectory == true }
    val files = (routesRootFolder.listFiles(fileFilter) ?: emptyArray<File>()).sortedBy { it.name }
    Timber.i("route folders: ${files.size}")
    val routeFolderList = mutableListOf<FolderReference>()
    for (file in files) {
        routeFolderList.add(FolderReference(file.name, file.path))
    }
    return routeFolderList
}

fun removeRouteFolderSnapshots(context: Context, routeFolderName: String?, onFinished: (resultDelete: Int) -> Unit) {
    var deleteCount = 0
    val routeFolderRoot = File(context.filesDir, Const.ROUTEFOLDER)
    val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
    if (routeFolderName != null) {
        val routeFolder = File(routeFolderRoot, routeFolderName)
        val routeFiles = routeFolder.listFiles() ?: return
        routeFiles.forEach { routeFile ->
            // Delete thumbnail in central thumbnails folder
            if (File(thumbnailsFolder, routeFile.nameWithoutExtension.plus(Const.JPG_EXT)).delete()) {
                deleteCount++
            }
            // Delete JPG files inside the route folder itself
            if (routeFile.extension.equals(Const.JPG_EXT.removePrefix("."), ignoreCase = true)) {
                if (routeFile.delete())
                    deleteCount++
            }
        }
        Timber.i("snapshots deleted: $deleteCount")
        onFinished(deleteCount)
    }
}

fun removeRouteFolderContent(context: Context, routeFolderName: String?, onFinished: (resultDelete: Boolean) -> Unit) {
    val routeFolderRoot = File(context.filesDir, Const.ROUTEFOLDER)
    val thumbnailsFolder = File(context.filesDir, Const.THUMBNAILS)
    if (routeFolderName != null) {
        val routeFolder = File(routeFolderRoot, routeFolderName)
        val routeFiles = routeFolder.listFiles()
        var thumbCount = 0
        routeFiles?.forEach { routeFile ->
            val b = File(thumbnailsFolder, routeFile.nameWithoutExtension.plus(Const.JPG_EXT)).delete()
            if (b)
                thumbCount++
        }
        Timber.i("thumbCount deleted: $thumbCount")
        if (routeFolderName != Const.HOME) {
            val result = routeFolder.deleteRecursively()
            Timber.i("$routeFolderName deleteRecursively: $result")
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val homeRouteFolder = File(routeFolderRoot, Const.HOME)
            prefs.edit { putString(Const.PREF_ROUTEFOLDER_FILEPATH, Const.HOME) }
            onFinished(result)
        } else {
            val result = routeFolder.deleteRecursively()
            Timber.i("$routeFolderName deleteRecursively: $result")
            routeFolder.mkdir()
            onFinished(result)
        }
    } else
        onFinished(false)
}

@Composable
fun ListRouteFoldersContent(
    modifier: Modifier,
    prefRouteFolderName: String?,
    routeFolders: List<FolderReference>,
    selectRouteFolder: (routeFolder: RouteFolder) -> Unit,
    menuRouteFolder: (folderName: String) -> Unit,
    prefRouteFolderChanged: (String?) -> Unit
) {
    Timber.i("routeFolders.size: ${routeFolders.size}")
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier
            .padding(
                horizontal = Margin.horizontal,
                vertical = Margin.vertical
            ),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(routeFolders) { routeFolder ->
            RouteFolderItem(prefRouteFolderName, routeFolderName = routeFolder.name, onItemClick = {
                val folderFile = File(File(context.filesDir, Const.ROUTEFOLDER), routeFolder.name)
                val routeFiles = folderFile.listFiles()
                selectRouteFolder(
                    RouteFolder(
                        name = folderFile.name,
                        path = folderFile.path,
                        fileCount = routeFiles?.size ?: 0
                    )
                )
            }, menu = { folderName ->
                Timber.i("menu: $folderName")
                menuRouteFolder(folderName)
            }, prefRouteFolderChanged = {
                prefRouteFolderChanged(it)
            })
        }
    }
}

@Composable
private fun RouteFolderItem(
    prefRouteFolderName: String?,
    routeFolderName: String,
    onItemClick: (String) -> Unit,
    menu: (String) -> Unit,
    prefRouteFolderChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onItemClick(routeFolderName) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(
                text = routeFolderName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        IconButton(onClick = { menu(routeFolderName) }) {
            Icon(Icons.Outlined.Menu, contentDescription = "Folder Menu")
        }
        Switch(
            checked = prefRouteFolderName == routeFolderName,
            onCheckedChange = {
                prefRouteFolderChanged(routeFolderName)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RouteFilesListPreview() {
    RamaniTheme {
        RouteFilesList(
            context = LocalContext.current,
            routeFolderName = "Sample Folder",
            finish = {},
            route = {},
            routeSnapshot = {},
            menu = {},
            filterChanged = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFilesList(context: Context,
                   routeFolderName: String?,
                   finish:() -> Unit,
                   route:(File) -> Unit,
                   routeSnapshot:(RouteFileBundle) -> Unit,
                   menu:(File) -> Unit,
                   filterChanged:(String?) -> Unit
) {

    var routeNameFilter: String? by remember { mutableStateOf(null) }
    var refreshTrigger by remember { mutableLongStateOf(0L) }
    val rootRouteFolder = File(context.filesDir, Const.ROUTEFOLDER)
    val routeFolder = if (routeFolderName != null)
        File(rootRouteFolder, routeFolderName) else null
    val allRouteFiles by produceState(initialValue = arrayListOf(), key1 = routeNameFilter, key2 = refreshTrigger) {
        filterChanged(routeNameFilter)
        Timber.i("routeNameFilter: $routeNameFilter")
        withContext(Dispatchers.IO) {
            val files = arrayListOf<File>()
            rootRouteFolder.walkTopDown().forEach { routeFile ->
                Timber.i("${routeFile.parentFile?.name} - ${routeFile.nameWithoutExtension}")
                if (routeNameFilter.isNullOrEmpty() && routeFile.isFile)
                    files.add(routeFile)
                else {
                    if (routeFile.isFile && routeFile.nameWithoutExtension.contains(
                            routeNameFilter ?: "",
                            true
                        )
                    ) {
                        files.add(routeFile)
                    }
                }
            }
            value = files
        }
    }

    val singleFolderRouteFiles by produceState(initialValue = emptyArray<File>(), key1 = routeNameFilter, key2 = refreshTrigger) {
        Timber.i("routeNameFilter: $routeNameFilter")
        withContext(Dispatchers.IO) {
            routeFolder?.let { folder ->
                Timber.i("routeNameFilter: $routeNameFilter")
                val files = if (routeNameFilter.isNullOrEmpty())
                    folder.listFiles()
                else
                    folder.listFiles { f ->
                        routeNameFilter?.let { filter ->
                            f.nameWithoutExtension.contains(
                                filter,
                                true
                            )
                        } ?: true
                    }
                value = files ?: emptyArray()
            }
        }
    }
    Timber.i("singleFolderRouteFiles: ${singleFolderRouteFiles.size}")
    var displayFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var readyTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(singleFolderRouteFiles, allRouteFiles) {
        val singleFiles = singleFolderRouteFiles
        displayFiles = if (routeFolderName == null) {
            allRouteFiles.sortedWith(
                compareByDescending<File> { it.name.endsWith(".geojson", ignoreCase = true) }
                    .thenBy { it.name })
        }
        else
            singleFiles.sortedWith(
                compareByDescending<File> { it.name.startsWith("${routeFolderName}_", ignoreCase = true) }
                    .thenBy { it.name })
        Timber.i("displayFiles: ${displayFiles.size}")
        Timber.i("allRouteFiles: ${allRouteFiles.size}")
        readyTime = System.currentTimeMillis()
    }

    RoutesDialog(
        onDismiss = { finish() },
        routeFolderName = routeFolderName,
        routeFiles = displayFiles,
        filter = routeNameFilter ?: "",
        onRouteClick = { routeFile ->
            Timber.i("onRouteClick route: $routeFile")
            route(routeFile)
        }, onSnapshotClick = { routeFileBundle ->
            Timber.i("onSnapshotClick routeFileBundle: $routeFileBundle")
            routeSnapshot(routeFileBundle)
        }, onMenuClick = { file ->
            Timber.i("menu: $file")
            menu(file)
        }, onFilterChange = {routeNameFilter = it}
        , onRefresh = {
            refreshTrigger = System.currentTimeMillis()
        },
        refreshTrigger = refreshTrigger
    )
//    LaunchedEffect(key1 = displayFiles) {
//        if (displayFiles.isEmpty()) {
//            filterChanged()
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutesDialog(
    onDismiss: () -> Unit,
    routeFolderName: String?,
    routeFiles: List<File>,
    filter: String, // Pass the filter value directly
    onRouteClick: (File) -> Unit,
    onSnapshotClick: (RouteFileBundle) -> Unit,
    onMenuClick: (File) -> Unit,
    onFilterChange: (String) -> Unit,
    onRefresh: () -> Unit,
    refreshTrigger: Long
) {
    var isSearchVisible by remember { mutableStateOf(filter.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close dialog")
                    }

                    Text(
                        text = routeFolderName ?: stringResource(R.string.all_routes),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = { onRefresh() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh list"
                        )
                    }
                    IconButton(onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) onFilterChange("")
                    }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Outlined.FilterAltOff else Icons.Outlined.FilterAlt,
                            contentDescription = "Toggle filter",
                            tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                }

                HorizontalDivider()

                // Animated Search Bar
                AnimatedVisibility(visible = isSearchVisible) {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = onFilterChange,
                        placeholder = { Text("Search routes...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                        trailingIcon = if (filter.isNotEmpty()) {
                            {
                                IconButton(onClick = { onFilterChange("") }) {
                                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                                }
                            }
                        } else null,
                        singleLine = true
                    )

                    LaunchedEffect(isSearchVisible) {
                        if (isSearchVisible) focusRequester.requestFocus()
                    }
                }

                // List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = routeFiles,
                        key = { it.absolutePath }
                    ) { routeFile ->
                        RouteFilesItem(
                            routeFile = routeFile,
                            refreshTrigger = refreshTrigger,
                            onItemClick = onRouteClick,
                            onSnapshotClick = onSnapshotClick,
                            fileMenu = onMenuClick
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskForNameFilter(onFilter: (String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = { onFilter(null) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Filter by name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onFilter(text) }) {
                Text("Apply")
            }
        }
    }
}

data class RouteFileBundle (
    val routeFile: File,
    val thumbnailFile: File
)

@Composable
fun RouteFilesItem(
    routeFile: File,
    refreshTrigger: Long,
    onItemClick: (File) -> Unit,
    onSnapshotClick: (RouteFileBundle) -> Unit,
    fileMenu: (File) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    val thumbnailFile = remember(routeFile, refreshTrigger) {
        val centralThumb = File(File(context.filesDir, Const.THUMBNAILS), routeFile.nameWithoutExtension.plus(Const.JPG_EXT))
        if (centralThumb.exists()) {
            centralThumb
        } else {
            // Fallback: look for thumbnail in the same folder as the route file
            val localThumb = File(routeFile.parentFile, routeFile.nameWithoutExtension.plus(Const.JPG_EXT))
            if (localThumb.exists()) localThumb else centralThumb
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onItemClick(routeFile) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Section
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        //if (thumbnailFile.exists())
                        onSnapshotClick(RouteFileBundle(routeFile, thumbnailFile))
                        //else onItemClick(routeFile)
                    },
            ) {
                if (thumbnailFile.exists()) {
                    AsyncImage(
                        model = thumbnailFile,
                        contentDescription = "Route thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Text Information Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = routeFile.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dateFormatter.format(Date(routeFile.lastModified()))} • ${String.format("%.1f KB", routeFile.length() / 1024f)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Action Menu
            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = { fileMenu(routeFile) }
            ) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = "File Menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

enum class RouteFolderAction{
    Nothing,
    TextImport,
    ImageImport,
    Cleanup,
    RemoveContent,
    CreatePdf,
    CreateSnapshots,
    RemoveSnapshots
}

@Composable
private fun DropdownRouteFolderMenu(dialogModeOrdinal: Int, action: (RouteFolderAction) -> Unit) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(RouteFolderAction.Nothing) }
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.FileDownload,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_file)
                )
            },
            onClick = {
                action(RouteFolderAction.TextImport)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Image,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.import_photo)
                )
            },
            onClick = {
                action(RouteFolderAction.ImageImport)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.ClearAll,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.cleanup)
                )
            },
            onClick = {
                action(RouteFolderAction.Cleanup)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.PlaylistRemove,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.remove_all_routes)
                )
            },
            onClick = {
                action(RouteFolderAction.RemoveContent)
            }
        )
        if (dialogModeOrdinal == RouteDialogMode.Admin.ordinal) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Outlined.PictureInPicture,
                        null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.create_snapshots)
                    )
                },
                onClick = {
                    action(RouteFolderAction.CreateSnapshots)
                }
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Outlined.RemoveFromQueue,
                        null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.remove_snapshots)
                    )
                },
                onClick = {
                    action(RouteFolderAction.RemoveSnapshots)
                }
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Outlined.PictureAsPdf,
                        null
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.pdf_creator)
                    )
                },
                onClick = {
                    action(RouteFolderAction.CreatePdf)
                }
            )
        }
    }
}

enum class RouteFileAction{
    Nothing,
    Delete,
    Info,
    Share
}
@Composable
private fun DropdownRouteFileMenu(menuRouteFile: File?, action: (RouteFileAction) -> Unit) {
    Timber.i("DropdownRouteFileMenu")
    DropdownMenu(
        expanded = true,
        onDismissRequest = { action(RouteFileAction.Nothing) },
        properties = PopupProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        DropdownMenuItem(leadingIcon = {}, enabled = false,
            text = { menuRouteFile?.let { Text(it.nameWithoutExtension) } }, onClick = {})
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete)
                )
            },
            onClick = {
                action(RouteFileAction.Delete)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Info,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.route_preview)
                )
            },
            onClick = {
                action(RouteFileAction.Info)
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    Icons.Outlined.Share,
                    null
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.share_file)
                )
            },
            onClick = {
                action(RouteFileAction.Share)
            }
        )
    }
}

fun shareRouteFile(context: Context, routeFile: File) {
    try {
        if(routeFile.exists()) {
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", routeFile)
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else
            Timber.i(context.getString(R.string.file_not_found, routeFile.path))
    } catch (e: Exception) {
        val msg = e.message
        if (msg != null) {
            Timber.e(msg)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListRouteFoldersScreenPreview() {
    val sampleFolders = listOf(
        FolderReference("Sample Folder 1", "/sample/1"),
        FolderReference("Sample Folder 2", "/sample/2"),
        FolderReference("Sample Folder 3", "/sample/3")
    )
    RamaniTheme {
        ListRouteFoldersScreen(
            marginTopDp = 0f,
            selectRouteFolder = {},
            routeFoldersIn = sampleFolders,
            routeFolderNameIn = "Sample Folder 1",
            finished = {},
            route = {},
            routeSnapshot = {},
            routeInfo = {},
            createSnapshots = {},
            dialogMode = RouteDialogMode.Admin.ordinal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListRouteFoldersContentPreview() {
    val sampleFolders = listOf(
        FolderReference("Folder A", "/path/A"),
        FolderReference("Folder B", "/path/B")
    )
    RamaniTheme {
        ListRouteFoldersContent(
            modifier = Modifier,
            prefRouteFolderName = "Folder A",
            routeFolders = sampleFolders,
            selectRouteFolder = {},
            menuRouteFolder = {},
            prefRouteFolderChanged = {}
        )
    }
}

