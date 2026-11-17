package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.TravaNetRepository
import com.example.domain.EffectiveLockState
import com.example.domain.LockReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer

class ParentalVpnService : VpnService() {

    companion object {
        const val ACTION_SYNC_STATE = "com.example.action.SYNC_VPN_STATE"
        const val ACTION_STOP_VPN = "com.example.action.STOP_VPN"
        const val EXTRA_INTERNAL_START = "extra_internal_start"

        private const val CHANNEL_ID = "travanet_shield"
        private const val NOTIFICATION_ID = 7001

        fun startOrSync(context: Context) {
            val intent = Intent(context, ParentalVpnService::class.java).apply {
                action = ACTION_SYNC_STATE
                putExtra(EXTRA_INTERNAL_START, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ParentalVpnService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: TravaNetRepository
    private lateinit var connectivityManager: ConnectivityManager

    private var vpnInterface: ParcelFileDescriptor? = null
    private var drainJob: Job? = null
    private var tickerJob: Job? = null
    private var callbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            serviceScope.launch { evaluateAndApply() }
        }

        override fun onLost(network: Network) {
            serviceScope.launch { evaluateAndApply() }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            serviceScope.launch { evaluateAndApply() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = TravaNetRepository(applicationContext)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        ensureNotificationChannel()
        registerNetworkWatcher()
        startSafetyTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            tearDownTunnel()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundNotification(
            title = getString(R.string.app_name),
            body = "Verificando regras de bloqueio parental...",
        )

        serviceScope.launch {
            evaluateAndApply()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        tearDownTunnel()
        stopForegroundCompat()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        unregisterNetworkWatcher()
        tearDownTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerNetworkWatcher() {
        if (callbackRegistered) return
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            callbackRegistered = true
        } catch (_: Exception) {
            callbackRegistered = false
        }
    }

    private fun unregisterNetworkWatcher() {
        if (!callbackRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        } finally {
            callbackRegistered = false
        }
    }

    private fun startSafetyTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(15_000L)
                evaluateAndApply()
            }
        }
    }

    @Synchronized
    private fun applyTunnelState(shouldSinkhole: Boolean) {
        if (shouldSinkhole) {
            if (vpnInterface != null) return

            val established = try {
                Builder()
                    .setSession("TravaNet Bloqueio Parental")
                    .addAddress("10.120.0.1", 32)
                    .addAddress("fd00:120::1", 128)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDnsServer("10.120.0.2")
                    .setBlocking(true)
                    .establish()
            } catch (_: Exception) {
                try {
                    Builder()
                        .setSession("TravaNet Bloqueio Parental")
                        .addAddress("10.120.0.1", 32)
                        .addRoute("0.0.0.0", 0)
                        .addDnsServer("10.120.0.2")
                        .setBlocking(true)
                        .establish()
                } catch (_: Exception) {
                    null
                }
            }

            if (established != null) {
                vpnInterface = established
                startPacketDrain(established)
            }
        } else {
            tearDownTunnel()
        }
    }

    private fun tearDownTunnel() {
        drainJob?.cancel()
        drainJob = null
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        } finally {
            vpnInterface = null
        }
    }

    private fun startPacketDrain(pfd: ParcelFileDescriptor) {
        drainJob?.cancel()
        drainJob = serviceScope.launch {
            val input = FileInputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)
            try {
                while (isActive) {
                    val length = input.channel.read(buffer)
                    if (length > 0) {
                        buffer.clear()
                    } else if (length < 0) {
                        break
                    }
                }
            } catch (_: Exception) {
                // Closed when tunnel tears down
            }
        }
    }

    private suspend fun evaluateAndApply() {
        val state = repository.evaluateCurrentLockState()
        ScheduleAlarmManager.scheduleNextTransition(applicationContext, state.nextTransitionEpochMs)

        if (!state.isBlocked) {
            tearDownTunnel()
            stopForegroundCompat()
            stopSelf()
            return
        }

        val (hasWifi, hasCellular) = detectPhysicalTransports()
        val shouldSinkhole = when {
            state.blockWifi && state.blockMobileData -> true
            state.blockWifi -> hasWifi || !hasCellular
            state.blockMobileData -> hasCellular || !hasWifi
            else -> false
        }

        applyTunnelState(shouldSinkhole)
        updateNotification(state)
    }

    @Suppress("DEPRECATION")
    private fun detectPhysicalTransports(): Pair<Boolean, Boolean> {
        var wifi = false
        var cellular = false
        try {
            for (network in connectivityManager.allNetworks) {
                val caps = connectivityManager.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) cellular = true
            }
        } catch (_: Exception) {
        }
        return wifi to cellular
    }

    private fun updateNotification(state: EffectiveLockState) {
        val targetText = when {
            state.blockWifi && state.blockMobileData -> "Wi-Fi e Internet Móvel bloqueados"
            state.blockWifi -> "Wi-Fi bloqueado"
            state.blockMobileData -> "Internet Móvel bloqueada"
            else -> "Monitoramento ativo"
        }

        val title = when (val reason = state.reason) {
            is LockReason.TotalLock -> "Bloqueio Total Ativo"
            is LockReason.QuickBlock -> {
                val mins = ((reason.remainingMs + 59_999L) / 60_000L).coerceAtLeast(1L)
                "Bloqueio Rápido • ${mins} min restantes"
            }
            is LockReason.ScheduledBlock -> {
                val profileName = reason.activeProfileNames.firstOrNull() ?: "Agendamento"
                "Perfil Ativo: $profileName"
            }
            LockReason.Unlocked -> "TravaNet"
        }

        startForegroundNotification(title = title, body = "$targetText • Toque para desbloquear com senha")
    }

    private fun startForegroundNotification(title: String, body: String) {
        val notification = buildNotification(title, body)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(title: String, body: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
