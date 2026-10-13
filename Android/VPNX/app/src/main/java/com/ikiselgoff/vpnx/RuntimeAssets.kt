package com.ikiselgoff.vpnx

import android.content.Context
import java.io.File

object RuntimeAssets {
    fun ensure(context: Context) {
        listOf("geoip.dat", "geosite.dat").forEach { name ->
            val target = File(context.filesDir, name)
            if (!target.exists() || target.length() == 0L) {
                context.assets.open(name).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
