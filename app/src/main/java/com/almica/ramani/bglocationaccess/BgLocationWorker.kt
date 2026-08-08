/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almica.ramani.bglocationaccess

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.almica.ramani.Const
import com.almica.ramani.GpsRepository
import com.almica.ramani.R
import com.almica.ramani.locations.LocationRepository
import com.almica.room.data.location.LocationEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import timber.log.Timber
import java.util.Date
import java.util.concurrent.Executors

class BgLocationWorker(context: Context, param: WorkerParameters) :
    CoroutineWorker(context, param) {
    companion object {
        // unique name for the work
        const val workName = "BgLocationWorker"
        private const val TAG = "BackgroundLocationWork"
        private const val PhoneNumber = "015735727627"
    }
    private val locationRepository = LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val altitudeCorrection = preferences.getInt(context.getString(R.string.pref_gps_altitude_correction_key),
            Const.ALTITUDE_CORRECTION)
    val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(SmsManager::class.java)
    } else {
        SmsManager.getDefault()
    }
    val ifilter: IntentFilter  = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

    override suspend fun doWork(): Result {
        Log.i(workName, "${Thread.currentThread().stackTrace[2].lineNumber}: altitudeCorrection: $altitudeCorrection")
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }
        locationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token,
        ).addOnSuccessListener { location ->
            location?.let {
                val locationEntity = LocationEntity(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitude = if (it.altitude > 0) it.altitude + altitudeCorrection
                    else it.altitude,
                    speed = it.speed * 3.6f,
                    bearing = it.bearing,
                    hasBearing = it.hasBearing(),
                    time = it.time,
                    recordedAt = Date(it.time)
                )
                if (GpsRepository.getInstance().isTrackingEnabled.value) {
                    locationRepository.addLocation(locationEntity)
                }
                val locationMsg = "Location = [lat : ${location.latitude}, lng : ${location.longitude}]"
                Timber.i("$workName $locationMsg")
                var temperature = 0.0
                if (batteryStatus != null) {
                    temperature =
                        0.1 * batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                }
                // on below line sending sms
/*
                smsManager.sendTextMessage(PhoneNumber,
                    null, "$locationMsg ${temperature.format(1)}°C", null, null)
 */
            }
        }
        return Result.success()
    }
}
