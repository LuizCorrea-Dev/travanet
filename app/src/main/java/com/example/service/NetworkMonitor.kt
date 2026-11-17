package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceNetworkSnapshot(
    val isWifiConnected: Boolean = false,
    val isMobileConnected: Boolean = false,
    val isVpnTunnelActive: Boolean = false,
    val isWifiRadioEnabled: Boolean = false,
)

class NetworkMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _snapshot = MutableStateFlow(readCurrentSnapshot())
    val snapshot: StateFlow<DeviceNetworkSnapshot> = _snapshot.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onLost(network: Network) = refresh()
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) = refresh()
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
            // Ignore in test environments where network callbacks may be restricted
        }
    }

    fun refresh() {
        _snapshot.value = readCurrentSnapshot()
    }

    @Suppress("DEPRECATION")
    private fun readCurrentSnapshot(): DeviceNetworkSnapshot {
        var wifi = false
        var mobile = false
        var vpn = false

        try {
            val networks = connectivityManager.allNetworks
            for (net in networks) {
                val caps = connectivityManager.getNetworkCapabilities(net) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) mobile = true
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
            }
        } catch (_: Exception) {
            // Fallback safe defaults
        }

        val wifiEnabled = try {
            wifiManager.isWifiEnabled
        } catch (_: Exception) {
            wifi
        }

        return DeviceNetworkSnapshot(
            isWifiConnected = wifi,
            isMobileConnected = mobile,
            isVpnTunnelActive = vpn,
            isWifiRadioEnabled = wifiEnabled,
        )
    }
}
