package com.example

import android.app.Application
import com.example.data.TravaNetRepository
import com.example.service.LockCoordinator
import com.example.service.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TravaNetApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: TravaNetRepository
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TravaNetRepository(this)
        networkMonitor = NetworkMonitor(this)

        applicationScope.launch {
            repository.ensureDefaultProfilesSeeded()
            LockCoordinator.syncProtection(this@TravaNetApplication)
        }
    }
}
