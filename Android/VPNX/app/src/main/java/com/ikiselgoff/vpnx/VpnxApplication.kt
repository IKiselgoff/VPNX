package com.ikiselgoff.vpnx

import android.app.Application

class VpnxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RuntimeAssets.ensure(this)
        BirdRepository.seedFromAssets(this)
        SyncScheduler.schedule(this)
    }
}
