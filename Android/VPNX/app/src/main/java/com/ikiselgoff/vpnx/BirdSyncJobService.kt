package com.ikiselgoff.vpnx

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import java.util.concurrent.Executors

class BirdSyncJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartJob(params: JobParameters): Boolean {
        RecoveryScheduler.schedule(this)
        runCatching { MaintenanceTunnelService.start(this) }
        executor.execute {
            runCatching { BirdRepository.sync(this) }.onFailure { Log.e("VPNX", "Background BIRD sync failed", it) }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
