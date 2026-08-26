package com.ikiselgoff.vpnx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SyncScheduler.schedule(context)
        val prefs = context.getSharedPreferences("vpnx", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_start", false)) {
            ContextCompat.startForegroundService(context, Intent(context, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_START))
        }
    }
}
