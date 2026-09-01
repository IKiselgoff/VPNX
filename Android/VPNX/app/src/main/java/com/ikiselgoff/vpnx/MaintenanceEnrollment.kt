package com.ikiselgoff.vpnx

import android.content.Context
import android.provider.Settings
import android.util.Base64
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.File
import org.json.JSONObject

object MaintenanceEnrollment {
    private const val PRIVATE_KEY = "maintenance_id_rsa"
    private const val KNOWN_HOSTS = "maintenance_known_hosts"
    private const val CONTROL_TOKEN = "maintenance_control_token"
    private const val ADB_REMOTE_PORT = "maintenance_adb_port"
    private const val CONTROL_REMOTE_PORT = "maintenance_control_port"

    @Synchronized
    fun ensure(context: Context): Boolean {
        val required = listOf(PRIVATE_KEY, KNOWN_HOSTS, CONTROL_TOKEN, ADB_REMOTE_PORT, CONTROL_REMOTE_PORT)
        if (required.all { File(context.filesDir, it).isFile }) return true

        val jsch = JSch()
        val deviceKey = KeyPair.genKeyPair(jsch, KeyPair.RSA, 3072)
        val privateKey = File(context.filesDir, "$PRIVATE_KEY.pending")
        deviceKey.writePrivateKey(privateKey.absolutePath)
        val publicBlob = Base64.encodeToString(deviceKey.publicKeyBlob, Base64.NO_WRAP)
        deviceKey.dispose()

        val bootstrapKey = copyAsset(context, "maintenance-bootstrap.pem")
        val knownHosts = copyAsset(context, "maintenance-known-hosts")
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.replace(Regex("[^A-Za-z0-9_.-]"), "_") ?: "android"

        jsch.setKnownHosts(knownHosts.absolutePath)
        jsch.addIdentity(bootstrapKey.absolutePath)
        val session = jsch.getSession("root", "45.146.165.85", 22).apply {
            setConfig("StrictHostKeyChecking", "yes")
            setConfig("PreferredAuthentications", "publickey")
            timeout = 30_000
            connect(20_000)
        }
        try {
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand("enroll $deviceId $publicBlob")
            channel.inputStream = null
            val output = channel.inputStream
            channel.connect(15_000)
            val response = output.bufferedReader().use { it.readText() }
            while (!channel.isClosed) Thread.sleep(50)
            check(channel.exitStatus == 0) { "Enrollment rejected: $response" }
            val result = JSONObject(response)
            check(result.getBoolean("ok")) { result.optString("error", "Enrollment failed") }

            writePrivate(context, CONTROL_TOKEN, result.getString("token"))
            writePrivate(context, ADB_REMOTE_PORT, result.getInt("adbPort").toString())
            writePrivate(context, CONTROL_REMOTE_PORT, result.getInt("controlPort").toString())
            privateKey.renameTo(File(context.filesDir, PRIVATE_KEY)).also { check(it) }
            knownHosts.copyTo(File(context.filesDir, KNOWN_HOSTS), overwrite = true)
            return true
        } finally {
            session.disconnect()
            bootstrapKey.delete()
            knownHosts.delete()
        }
    }

    private fun copyAsset(context: Context, name: String): File {
        val target = File(context.cacheDir, name)
        context.assets.open(name).use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    private fun writePrivate(context: Context, name: String, value: String) {
        File(context.filesDir, name).apply {
            writeText(value.trim() + "\n")
            setReadable(false, false)
            setReadable(true, true)
            setWritable(false, false)
            setWritable(true, true)
        }
    }
}
