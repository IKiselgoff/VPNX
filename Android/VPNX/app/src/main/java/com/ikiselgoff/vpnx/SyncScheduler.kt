package com.ikiselgoff.vpnx

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object SyncScheduler {
    private const val JOB_ID = 8621

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, BirdSyncJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(15 * 60 * 1000L)
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }
}
