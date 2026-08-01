package com.almica.ramani

import android.app.Application
import com.almica.ramani.utils.MyDebugTree
import com.almica.ramani.utils.ManifestUtils
import com.google.android.libraries.places.api.Places
import com.google.android.gms.maps.MapsInitializer
import org.maplibre.android.MapLibre
import org.maplibre.android.utils.ThreadUtils
import timber.log.Timber

/**
 * Project linked to GitHub: https://github.com/almica/ramani 28jul2026
 * New github repo: https://github.com/alminav/almica_ramani 01aug2026, reoson google api key may not be public
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (android.os.Process.isIsolated()) {
            return
        }
        Timber.plant(MyDebugTree())
        Timber.i("App onCreate")
        val useOpenGL = BuildConfig.USE_OPEN_GL // true: not Vulkan (Nokia 1), false: vulkan
        Timber.i("useOpenGL: $useOpenGL")
        ThreadUtils.init(this)  // moved here from LauncherActivity
        try {
            MapLibre.getInstance(
                this,
                Const.MAPBOX_ACCESS_TOKEN,
                Const.wellKnownTileServer
            )
        } catch (e: Exception) {
            Timber.e(e, "MapLibre initialization failed")
        }

        try {
            MapsInitializer.initialize(
                applicationContext,
                MapsInitializer.Renderer.LATEST
            ) { renderer ->
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> Timber.i("The latest version of the renderer is used.")
                    MapsInitializer.Renderer.LEGACY -> Timber.i("The legacy version of the renderer is used.")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MapsInitializer failed")
        }

        val apiKey = ManifestUtils.getApiKeyFromManifest(this)
        if (!Places.isInitialized() && apiKey != null) {
            try {
                Places.initializeWithNewPlacesApiEnabled(this, apiKey)
            } catch (e: Exception) {
                Timber.e(e, "Places initialization failed")
            }
        }
    }
}
