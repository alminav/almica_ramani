package com.almica.ramani.bglocationaccess

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.almica.ramani.bglocationaccess.Constants.ALARM_ID
import com.almica.ramani.bglocationaccess.Constants.MESSAGE
import com.almica.ramani.bglocationaccess.Constants.TITLE

private const val logtag = "AlarmSchedulerImpl"
class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {

    val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: alarm: ${alarm.title}")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.hashCode(),
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra(TITLE, alarm.title)
                putExtra(MESSAGE, alarm.message)
                putExtra(ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val timeToTrigger = System.currentTimeMillis() + 10 * 1000
        val intervalo: Long = 1 * 60 * 1000
        Log.i(logtag, "${Thread.currentThread().stackTrace[2].lineNumber}: ")
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,   //AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + intervalo,
            intervalo,
            pendingIntent)
        /*
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.scheduleAt,
                    pendingIntent
                )
     */
    }

    override fun cancel(alarm: Alarm) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                alarm.hashCode(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }

}