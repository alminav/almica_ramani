package com.almica.ramani.bglocationaccess

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.almica.ramani.Const
import com.almica.ramani.GpsRepository
import com.almica.ramani.MainActivity
import com.almica.ramani.R
import com.almica.ramani.utils.simpleStringWithTime
import com.almica.ramani.locations.LocationRepository
import com.almica.ramani.utils.format
import com.almica.ramani.utils.formatDistM
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService


/**
 * @Author: Abdul Rehman
 * @Date: 06/05/2024.
 */
private const val logtag = "LocationService"
private const val CHANNEL_ID = "ramani"
private const val NOTIFICATION_ID = 999
private const val DISTANCE_THRESHOLD = 20.0

class LocationService : Service(), LocationUpdatesCallBack, SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var gpsLocationClient: GPSLocationClient
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var startTime = System.currentTimeMillis()
    private var lastRoomLocation: Location? = null
    private var appState = State.FOREGROUND
    private var altitudeCorrection = 0
    private var goPendingIntent: PendingIntent? = null
    private var goIntent: Intent? = null
    private var distanceM = 0.0
    private var timeLong = 0L
    private var useStepCounter = false
    private var stepCounter = 0
    private var stepGoal = 500
    private val sensorManager: SensorManager by lazy {
        getSystemService(SENSOR_SERVICE) as SensorManager
    }
    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    private val locationRepository: LocationRepository by lazy {
        LocationRepository.getInstance(applicationContext, executor)
    }

    private var sensor: Sensor? = null
    override fun onCreate() {
        super.onCreate()
        gpsLocationClient = GPSLocationClient()
        gpsLocationClient.setLocationUpdatesCallBack(this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        altitudeCorrection = preferences.getInt(getString(R.string.pref_gps_altitude_correction_key),0)
        //val activityIntent = Intent(this, MainActivity::class.java)
        goIntent = Intent(this, MainActivity::class.java)
        goIntent?.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP // 09Apr2026
        goIntent?.action = Intent.ACTION_MAIN
        goIntent?.addCategory(Intent.CATEGORY_LAUNCHER) // launcher intent does the trick
        goPendingIntent = PendingIntent.getActivity(this, 1, goIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        useStepCounter = if (sensor == null)
            false
        else
            prefs.getBoolean(Const.PREF_USE_STEPCOUNTER, false)
        //Timber.i("useStepCounter: $useStepCounter")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Timber.i( "action: ${intent?.action} altitudeCorrection: $altitudeCorrection")
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
                startTime = intent.getLongExtra(Const.TAG_START_TIME, System.currentTimeMillis())
                //val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                //val textTime: String? = sdf.format(startTime)
                //Timber.i("startTime: $textTime")
                val result =
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
                if (!result) useStepCounter = false
                //Timber.i( "sensorManager.registerListener: $result")
                startService()
            }
            ACTION_SERVICE_STOP -> {
                Timber.i("ACTION_SERVICE_STOP")
                sensorManager.unregisterListener(this)
                ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleEventObserver)
                stopService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_SERVICE_START = "ACTION_START"
        const val ACTION_SERVICE_STOP = "ACTION_STOP"
    }

    var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Timber.i("$event")
                appState = State.BACKGROUND
            }
            Lifecycle.Event.ON_START -> {
                Timber.i( "$event")
                appState = State.FOREGROUND
                //notificationManager?.cancelAll()
            }
            else -> {}//Timber.i( "$event")
        }
    }

    private fun startService() {
        prefs.registerOnSharedPreferenceChangeListener(this)
        gpsLocationClient.getLocationUpdates(applicationContext)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Searching...")
            //.setContentIntent(goPendingIntent)
            .setContentIntent(goPendingIntent)
            .setSmallIcon(R.drawable.baseline_route_24)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setUsesChronometer(true)
            .setOngoing(true)
        notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.createNotificationChannel(channel)
        Timber.i( "startForeground: $NOTIFICATION_ID")
        startForeground(NOTIFICATION_ID, notificationBuilder?.build())
    }

    private fun stopService() {
        Timber.i("stopService")
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        if (useStepCounter)
            sensorManager.unregisterListener(this)
        gpsLocationClient.stopLocations() // 09apr2026 GPS status icon
        gpsLocationClient.setLocationUpdatesCallBack(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager?.cancelAll()
        executor.shutdown()
        stopSelf()
    }

    override fun locationException(message: String) {
        Timber.e(message)
    }

    override fun onLocationUpdate(location: Location) {
        //Timber.i( "$appState Steps: $stepCounter")
        val textStepCounter = "\nSteps: $stepCounter"

        var deltaDistRoom = Double.MIN_VALUE
        if (lastRoomLocation == null) {
            //startTime = location.time
            lastRoomLocation = Location(location)
        } else {
            deltaDistRoom = SphericalUtil.computeDistanceBetween(
                LatLng(location.latitude, location.longitude),
                LatLng(lastRoomLocation!!.latitude, lastRoomLocation!!.longitude)
            )
//            Timber.i( "${Date(location.time).simpleStringWithTime()} " +
//                    "deltaDistRoom: ${deltaDistRoom.format(1)}")

            //distanceM += deltaDistRoom
            timeLong = location.time - startTime
        }

        if (deltaDistRoom > DISTANCE_THRESHOLD) {
            distanceM += deltaDistRoom
//            Timber.i( "${Date(location.time).simpleStringWithTime()} deltaDistRoom: ${deltaDistRoom.format(1)}" +
//                    " distanceM: ${distanceM.format(1)}")

            lastRoomLocation = Location(location)
        }

        val timeOffset = ZonedDateTime.now().offset.totalSeconds
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        if (appState == State.BACKGROUND) {
            val locationEntity = LocationEntity(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.altitude > 0) location.altitude + altitudeCorrection
                else location.altitude,
                speed = location.speed * 3.6f,
                bearing = location.bearing,
                hasBearing = location.hasBearing(),
                time = location.time,
                recordedAt = Date(location.time)
            )
            
            if (deltaDistRoom > DISTANCE_THRESHOLD) {
                locationRepository.addLocation(locationEntity)
                Timber.i("addLocation: ${Date(location.time).simpleStringWithTime()}")
            }

            // format: hour:minute:second
            //Timber.i( "distanceM: ${distanceM.format(1)}m")
            val textTime: String? = sdf.format(timeLong - timeOffset * 1000) // 01:30:00
            val textDist = "distance: ${distanceM.formatDistM(true)}"
            val notificationText = "$textTime ${textDist}\n" +
                    "lat:${location.latitude.format(4)}° lon:${location.longitude.format(4)}° " +
                    "h:${(location.altitude + altitudeCorrection).format(1)}m"

            val textTitle = if (useStepCounter)
                textStepCounter
            else
                "Background Tracking"
            val updatedNotification = notificationBuilder?.setContentTitle(textTitle)
                ?.setContentText(notificationText)?.setSilent(stepCounter < stepGoal)
            if (stepCounter > stepGoal) {
                stepGoal += 500
            }
            //Timber.i(notificationText)
            notificationManager?.notify(NOTIFICATION_ID, updatedNotification?.build())
        } else {
            //Timber.i( "Foreground Tracking")
            var notificationTitle = "Foreground Tracking"
            notificationTitle = if (useStepCounter)
                notificationTitle.plus(textStepCounter)
            else
                notificationTitle.plus("\nStepcounter is inactive")
            val textTime: String? = sdf.format(timeLong - timeOffset * 1000) // 01:30:00
            val textDist = "distance: ${distanceM.formatDistM(true)}"
            val notificationText = "$textTime $textDist"
            val updatedNotification = notificationBuilder?.setContentText(notificationText)
                ?.setContentTitle(notificationTitle)?.setSilent(stepCounter < stepGoal)
            if (stepCounter > stepGoal) {
                stepGoal += 500
            }
            notificationManager?.notify(NOTIFICATION_ID, updatedNotification?.build())
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Timber.i( "onAccuracyChanged $p1")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { event ->
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                Timber.i( "stepCounter: $stepCounter")
                stepCounter += 1
                GpsRepository.getInstance().updateStepCounter(stepCounter)
            }
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        if (key == Const.PREF_USE_STEPCOUNTER) {
            useStepCounter = sharedPreferences?.getBoolean(key, false) ?: false
            Timber.i( "useStepCounter: $useStepCounter")
            if (useStepCounter)
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            else {
                sensorManager.unregisterListener(this)
                stepCounter = 0
            }
        }
    }

    fun getLastRoomLocation(): Location? {
        Timber.i("getLastRoomLocation")
        val locations = locationRepository.getLastLocation()
        if (locations.isEmpty()) return null
        
        val lastLocationEntity = locations[0]
        val lastLocation = Location("service")
        lastLocation.let {
            it.time = lastLocationEntity.time
            it.latitude = lastLocationEntity.latitude
            it.longitude = lastLocationEntity.longitude
            it.speed = lastLocationEntity.speed
            it.altitude = lastLocationEntity.altitude
            it.bearing = lastLocationEntity.bearing
        }
        return lastLocation
    }
}

enum class State {
    FOREGROUND, BACKGROUND
}