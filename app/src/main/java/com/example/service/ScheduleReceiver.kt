package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_EVALUATE_LOCK = "com.example.action.EVALUATE_LOCK"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LockCoordinator.syncProtection(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
