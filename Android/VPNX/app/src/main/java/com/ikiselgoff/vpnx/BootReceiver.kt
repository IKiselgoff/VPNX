package com.ikiselgoff.vpnx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_RECOVER = "com.ikiselgoff.vpnx.action.RECOVER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        SyncScheduler.schedule(context)
        RecoveryScheduler.schedule(context)
        runCatching { MaintenanceTunnelService.start(context) }
        val prefs = context.getSharedPreferences("vpnx", Context.MODE_PRIVATE)
        if (intent.action != ACTION_RECOVER && prefs.getBoolean("auto_start", false)) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_START),
                )
            }
        }
    }
}
