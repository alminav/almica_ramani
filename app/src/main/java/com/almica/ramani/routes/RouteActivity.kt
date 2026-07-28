package com.almica.ramani.routes

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.almica.ramani.Const
import com.almica.ramani.RouteDialog
import com.almica.ramani.ui.theme.RamaniTheme
import timber.log.Timber
import java.io.File

class RouteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val latitude = intent.getDoubleExtra(Const.EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(Const.EXTRA_LONGITUDE, 0.0)
        val dialogModeOrdinal = intent.getIntExtra(Const.EXTRA_ROUTE_DIALOG_MODE, RouteDialogMode.Admin.ordinal)
        val dialogMode = RouteDialogMode.entries.getOrElse(dialogModeOrdinal) { RouteDialogMode.Admin }

        val userLocation = Location(null).apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        setContent {
            RamaniTheme {
                var showRouteDialog by remember { mutableStateOf<RouteEntity?>(null) }

                showRouteDialog?.let { routeEntity ->
                    Timber.i("showRouteDialog: ${routeEntity.name}")
                    val rootFolder = File(filesDir, Const.ROUTEFOLDER)
                    val routeFolder = File(rootFolder, routeEntity.region)
                    val routeFile = File(routeFolder, routeEntity.name)
                    RouteDialog(filesDir, routeFile, finish = {
                        Timber.i("finish routeFile = null")
                        showRouteDialog = null
                    }, alert = { _ -> showRouteDialog = null
                    }, share = { showRouteDialog = null
                    }, refresh = { showRouteDialog = null
                    }, select = { showRouteDialog = null
                    }, dialogModeOrdinal = RouteDialogMode.MapProvider.ordinal)
                }

                RoutesManager(
                    location = userLocation,
                    selectRoute = { routeEntity, routeAction ->
                        when (routeAction) {
                            RouteMenu.Map -> showRouteDialog = routeEntity
                            else -> {
                                setResult(RESULT_OK)
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

enum class RouteDialogMode {
    MapProvider,
    Admin
}


