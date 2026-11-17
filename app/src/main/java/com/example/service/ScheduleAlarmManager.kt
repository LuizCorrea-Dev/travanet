package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ScheduleAlarmManager {
    private const val REQUEST_CODE_TRANSITION = 4102

    fun scheduleNextTransition(context: Context, triggerEpochMs: Long?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_EVALUATE_LOCK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TRANSITION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.cancel(pendingIntent)
        if (triggerEpochMs == null || triggerEpochMs <= System.currentTimeMillis()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent,
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpochMs,
                pendingIntent,
            )
        }
    }
}
