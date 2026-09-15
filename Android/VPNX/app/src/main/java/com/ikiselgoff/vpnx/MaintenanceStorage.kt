package com.ikiselgoff.vpnx

import android.content.Context
import java.io.File

object MaintenanceStorage {
    const val PRIVATE_KEY = "maintenance_id_rsa"
    const val KNOWN_HOSTS = "maintenance_known_hosts"
    const val CONTROL_TOKEN = "maintenance_control_token"
    const val ADB_REMOTE_PORT = "maintenance_adb_port"
    const val CONTROL_REMOTE_PORT = "maintenance_control_port"

    private val files = listOf(PRIVATE_KEY, KNOWN_HOSTS, CONTROL_TOKEN, ADB_REMOTE_PORT, CONTROL_REMOTE_PORT)

    fun filesDir(context: Context): File = context.createDeviceProtectedStorageContext().filesDir

    fun migrateFromCredentialStorage(context: Context) {
        val targetDir = filesDir(context).apply { mkdirs() }
        files.forEach { name ->
            val source = File(context.filesDir, name)
            if (source.isFile) source.copyTo(File(targetDir, name), overwrite = true)
        }
    }
}
