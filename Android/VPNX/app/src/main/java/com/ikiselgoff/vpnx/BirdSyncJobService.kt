package com.ikiselgoff.vpnx

import android.app.job.JobParameters
import android.app.job.JobService
import java.util.concurrent.Executors

class BirdSyncJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onStartJob(params: JobParameters): Boolean {
        executor.execute {
            runCatching { BirdRepository.sync(this) }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}
