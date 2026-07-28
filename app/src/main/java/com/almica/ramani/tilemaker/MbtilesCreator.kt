package com.almica.ramani.tilemaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.almica.ramani.Const
import com.almica.ramani.R
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Locale

private const val logtag = "MbtilesCreator"
open class MbtilesCreator(val context: Context) {
    lateinit var notificationView : RemoteViews
    lateinit var notificationBuilder : NotificationCompat.Builder
    lateinit var job : Job

    fun createMbtiles(
        name: String,
        mapType: String,
        baseUrl: String,
        area: Array<LatLng>,
        zooms: IntArray,
        progress: (Job, Int) -> Unit,
        ready: (String) -> Unit,
        cancel: (String) -> Unit
    ) {
        val northWest = doubleArrayOf(area[0].latitude, area[0].longitude)
        val southEast = doubleArrayOf(area[2].latitude, area[2].longitude)
        //val dbName = name + "_" + mapType + borderLen + Const.MBTILES_EXT
        val splitsCount = name.split(Const.UNDERLINE).size
        val dbName = if (splitsCount == 5)
            "${name}${Const.MBTILES_EXT}" else "${name}_${mapType}${Const.MBTILES_EXT}"
        Timber.i("$dbName $northWest $southEast")

        job = CoroutineScope(Dispatchers.Main).launch {
            try {
                //shows something in the UI - progressBar
                withContext(Dispatchers.IO) {
                    val dbHelper = getDbHelper(dbName)
                    val db = dbHelper.writableDatabase
                    val dbFile = File(db.path)
                    Timber.i("${db.path}")

                    val tilesSet: Set<Tile.TileCoordinate> =
                        Tile.getTiles(northWest, southEast, zooms)
                    var tiles: List<Tile.TileCoordinate> = ArrayList(tilesSet)
                    tiles = tiles.sortedBy { it.z }
                    var downloadedTiles = 0
                    tileLoop@ for (tileCoordinate in tiles) {
                        if (job.isCancelled) {
                            Timber.i("job.isCancelled")
                            break@tileLoop
                        }
                        var url: String
                        if (baseUrl.contains(Const.URL_OPENSTREETMAP_CYCLOSM)) {
                            val aNumber = (3 * Math.random()).toInt()
                            val tempUrl = Const.URLS_OPENSTREETMAP_CYCLOSM[aNumber]
                            url = tempUrl
                                .replace("{z}", java.lang.String.valueOf(tileCoordinate.z))
                                .replace("{x}", java.lang.String.valueOf(tileCoordinate.x))
                                .replace("{y}", java.lang.String.valueOf(tileCoordinate.y))
                        } else if (baseUrl.contains(Const.OPENTOPO_PART)) {
                            val aNumber = (3 * Math.random()).toInt()
                            val tempUrl = Const.URLS_OPENTOPO[aNumber]
                            url = tempUrl
                                .replace("{z}", java.lang.String.valueOf(tileCoordinate.z))
                                .replace("{x}", java.lang.String.valueOf(tileCoordinate.x))
                                .replace("{y}", java.lang.String.valueOf(tileCoordinate.y))
                        } else {
                            url = baseUrl
                                .replace("{z}", java.lang.String.valueOf(tileCoordinate.z))
                                .replace("{x}", java.lang.String.valueOf(tileCoordinate.x))
                                .replace("{y}", java.lang.String.valueOf(tileCoordinate.y))
                        }
                        try {
                            val outputStream: ByteArrayOutputStream = downloadFile(url)
                            val byteArray = outputStream.toByteArray()
                            val valid = checkValidByteArray(byteArray)
                            if (valid) {
                                MbtilesDatabase.insertTiles(
                                    db,
                                    tileCoordinate.z,
                                    tileCoordinate.x,
                                    tileCoordinate.y,
                                    byteArray
                                )
                            } else
                                Timber.i("downloaded data is invalid: $url")
                            downloadedTiles++
                            val progress: Int = 100 * downloadedTiles / tiles.size
                            progress(job, progress)
                            //updateNotification(name, progress, customNotification)
                            Timber.i("$progress %$url bytes: ${byteArray.size}")
                        } catch (e: IOException) {
                            e.message?.let {
                                Timber.e("${Thread.currentThread().stackTrace[2].lineNumber} $it")
                            }
                        }
                    }
                    MbtilesDatabase.insertMetadata(
                        db, name, northWest, southEast, zooms[0],
                        zooms[zooms.size - 1]
                    )
                    //db.close() // Don't close db directly, close helper
                    dbHelper.close()

//                    if (isStopped) {
//                        val b = dbFile.delete()
//                        Timber.i("${Thread.currentThread().stackTrace[2].lineNumber}:${dbFile.path} deleted $b")
//                    }
                    val manager =
                        context.applicationContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(Const.FM_NOTIFICATION_ID)
                    if (job.isCancelled) cancel(dbName) else ready(dbName)
                }
            } catch (e: IOException) {
                e.message?.let { Timber.i("$it") }
                val manager =
                    context.applicationContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(Const.FM_NOTIFICATION_ID)
                ready(dbName)
            }
        }
    }

    fun checkValidByteArray(byteArray: ByteArray): Boolean {
        try {
            val bitmap =
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            return bitmap != null
        } catch (e: Exception) {
            e.message?.let { Timber.i("$it") }
        }
        return false
    }

    fun getDbHelper(dbName: String): MbtilesDatabase.MbtilesHelper {
        return MbtilesDatabase.MbtilesHelper(context.applicationContext, dbName)
    }

    fun getNotification(name: String, mapType: String, progress: Int): Notification {
        val notiMsg = String.format(Locale.getDefault(), " %s %s", name, mapType)
        notificationView.setTextViewText(R.id.notification_title, notiMsg)
        notificationView.setProgressBar(R.id.notification_progress, 100, progress, false)
        val manager =
            context.applicationContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        val description = "mbtiles"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            Const.CHANNEL_ID,
            name,
            importance
        )
        channel.description = description
        manager.createNotificationChannel(channel)
        // Add as notification
        return notificationBuilder
            .setSmallIcon(R.drawable.baseline_cloud_download_24)
            .setColor(ContextCompat.getColor(context.applicationContext, R.color.purple_500))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationView)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSilent(true)
            .setChannelId(Const.CHANNEL_ID)
//            .addAction(R.drawable.ic_baseline_language_24, context.getString(R.string.stop_), pintStopSelf)
            .build()
    }

    fun updateNotification(name: String, progress: Int, customNotification: Notification) {
        notificationView.setTextViewText(R.id.notification_title, name)
        notificationView.setProgressBar(R.id.notification_progress, 100, progress, false)
//        val customNotification = notificationBuilder.setCustomContentView(notificationView).build()
        // Add as notification
        val manager =
            context.applicationContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            Const.FM_NOTIFICATION_ID,
            customNotification)
    }

    private fun downloadFile(urlString: String): ByteArrayOutputStream {
        val url = URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connect()

        try {
            connection.inputStream.use { input ->
                val bufferedInput = BufferedInputStream(input, 8192)
                val output = ByteArrayOutputStream()
                val data = ByteArray(1024)
                var count: Int
                while (bufferedInput.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }
                output.flush()
                return output
            }
        } finally {
            connection.disconnect()
        }
    }

}