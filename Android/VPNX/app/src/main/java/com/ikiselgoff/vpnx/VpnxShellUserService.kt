package com.ikiselgoff.vpnx

import android.content.Context
import android.system.Os
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit

class VpnxShellUserService : IShellService.Stub {
    constructor()

    @Keep
    constructor(context: Context)

    override fun execute(command: String): String {
        require(command.length <= 4096) { "Command is too long" }
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return "timeout"
        }
        return process.inputStream.bufferedReader().use { it.readText() }.take(128 * 1024)
    }

    override fun uid(): Int = Os.getuid()

    override fun destroy() {
        System.exit(0)
    }
}
