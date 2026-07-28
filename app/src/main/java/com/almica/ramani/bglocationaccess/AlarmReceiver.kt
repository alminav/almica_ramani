package com.almica.ramani.bglocationaccess

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.almica.ramani.MainActivity
import com.almica.ramani.R
import com.almica.ramani.bglocationaccess.Constants.ALARM_CHANNEL_NAME
import com.almica.ramani.bglocationaccess.Constants.ALARM_ID
import com.almica.ramani.bglocationaccess.Constants.MESSAGE
import com.almica.ramani.bglocationaccess.Constants.STOP_ALARM
import com.almica.ramani.bglocationaccess.Constants.TITLE
import com.almica.ramani.utils.format
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val logtag = "AlarmReceiver"
private const val PhoneNumber = "015735727627"

class AlarmReceiver : BroadcastReceiver() {
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "RECOMPOSITION") { // not working, not used
            Timber.i("RECOMPOSITION")
            val i = Intent(context,MainActivity::class.java)
            i.flags = FLAG_ACTIVITY_NEW_TASK
            context?.startActivity(i)
            return
        }

        val locationClient = context?.let { LocationServices.getFusedLocationProviderClient(it) }
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
//        val mediaPlayer: MediaPlayer =
//            MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
//        mediaPlayer.isLooping = true


        if (intent?.action == STOP_ALARM) {
            val alarmId = intent.getIntExtra(ALARM_ID, 2)
            NotificationManagerCompat.from(context).cancel(alarmId)

//            mediaPlayer.release()
//            mediaPlayer.stop()

            val pIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pIntent)

            return
        }
        val title = intent?.getStringExtra(TITLE) ?: return
        val message = intent.getStringExtra(MESSAGE)
        val alarmId = intent.getIntExtra(ALARM_ID, 1)
        val goIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 1, goIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 1, Intent(context, AlarmReceiver::class.java).apply {
                action = STOP_ALARM
                putExtra(ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val currentTime = LocalDateTime.now().format(formatter)
        var temperature = 0.0
        if (batteryStatus != null) {
            temperature =
                0.1 * batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        }
        val builder = NotificationCompat.Builder(context, ALARM_CHANNEL_NAME)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setContentTitle("$currentTime")
            .setSilent(true)
            .setContentText("${temperature.format(1)}°C")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.outline_alarm_off_24, "STOP",
                stopPendingIntent
            )

        if (locationClient != null) {
//            val lastLocation = locationClient.lastLocation.result
//            Log.i(logtag,
//                "${Thread.currentThread().stackTrace[2].lineNumber}: lastLocation: $lastLocation")

            locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    location.let {
                        val locationMsg =
                            "Location = [lat : ${location.latitude.format(4)}, lng : ${
                                location.longitude.format(
                                    4
                                )
                            }]"
                        Timber.i(locationMsg)
                        // on below line sending sms
                        smsManager.sendTextMessage(
                            PhoneNumber,
                            null, "$currentTime $locationMsg ${temperature.format(1)}°C", null, null
                        )

                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            NotificationManagerCompat.from(context).notify(1, builder.build())
                        }
                    }
                } else {
                    Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: location = null")
                }
            }
        } else {
            Timber.i("locationClient is null")
            smsManager.sendTextMessage(
                PhoneNumber,
                null, "No Location $currentTime ${temperature.format(1)}°C", null, null
            )

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(1, builder.build())
            }
        }
        Timber.i("$currentTime ${temperature.format(1)}°C")
        //mediaPlayer.start()
    }
}