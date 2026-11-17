package com.example.service

import android.content.Context
import android.net.VpnService
import com.example.data.TravaNetRepository

object LockCoordinator {
    suspend fun syncProtection(context: Context) {
        val appContext = context.applicationContext
        val repository = TravaNetRepository(appContext)
        val state = repository.evaluateCurrentLockState()

        ScheduleAlarmManager.scheduleNextTransition(appContext, state.nextTransitionEpochMs)

        val vpnReady = try {
            VpnService.prepare(appContext) == null
        } catch (_: Exception) {
            false
        }

        if (state.isBlocked && vpnReady) {
            try {
                ParentalVpnService.startOrSync(appContext)
            } catch (_: Exception) {
            }
        } else if (!state.isBlocked) {
            try {
                ParentalVpnService.stop(appContext)
            } catch (_: Exception) {
            }
        }
    }
}
