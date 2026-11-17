package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.TravaNetApplication
import kotlinx.coroutines.launch

class ParentalDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "Proteção contra desinstalação ativada.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val app = context.applicationContext as? TravaNetApplication
        app?.applicationScope?.launch {
            app.repository.setTotalLock(active = true, blockWifi = true, blockMobile = true)
            LockCoordinator.syncProtection(app)
        }
        return "Ação protegida pelos pais: para desativar ou desinstalar o TravaNet, use a senha dos pais dentro do aplicativo. O bloqueio total foi acionado por segurança."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "Proteção contra desinstalação desativada.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}
