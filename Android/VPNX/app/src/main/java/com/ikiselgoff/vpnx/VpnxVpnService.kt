package com.ikiselgoff.vpnx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import libXray.DialerController
import libXray.LibXray
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class VpnxVpnService : VpnService() {
    companion object {
        const val ACTION_START = "com.ikiselgoff.vpnx.START"
        const val ACTION_STOP = "com.ikiselgoff.vpnx.STOP"
        const val ACTION_SWITCH = "com.ikiselgoff.vpnx.SWITCH"
        const val ACTION_STATE = "com.ikiselgoff.vpnx.STATE"
        const val EXTRA_RUNNING = "running"
        private const val CHANNEL_ID = "vpnx_connection"
        private const val NOTIFICATION_ID = 71
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val transitioning = AtomicBoolean(false)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var lastNetworkSync = 0L
    private lateinit var connectivity: ConnectivityManager

    private val controller = object : DialerController {
        override fun protectFd(fd: Long): Boolean = protect(fd.toInt())
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val now = System.currentTimeMillis()
            if (now - lastNetworkSync < 120_000L) return
            lastNetworkSync = now
            executor.execute {
                val changed = runCatching { BirdRepository.sync(this@VpnxVpnService).changed }.getOrDefault(false)
                if (changed && isDesiredRunning()) restartEngine()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        connectivity = getSystemService(ConnectivityManager::class.java)
        connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> executor.execute { stopEngine(clearDesired = true) }
            ACTION_SWITCH -> executor.execute { restartEngine() }
            else -> {
                setDesiredRunning(true)
                startForeground(NOTIFICATION_ID, notification("Подключение…"))
                executor.execute { startEngine() }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        executor.execute { stopEngine(clearDesired = true) }
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { connectivity.unregisterNetworkCallback(networkCallback) }
        runCatching { stopEngine(clearDesired = false) }
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun startEngine() {
        if (!transitioning.compareAndSet(false, true)) return
        try {
            RuntimeAssets.ensure(this)
            if (BirdRepository.profiles(this).isEmpty()) BirdRepository.sync(this)
            val profile = BirdRepository.selected(this) ?: error("Нет профилей BIRD")

            stopNative()
            val descriptor = Builder()
                .setSession("VPNX — ${profile.title}")
                .setMtu(1500)
                .addAddress("172.19.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("94.140.14.14")
                .setMetered(false)
                .establish() ?: error("Android не создал VPN-интерфейс")
            vpnInterface = descriptor

            LibXray.registerDialerController(controller)
            LibXray.registerListenerController(controller)
            LibXray.setDNS(controller, "94.140.14.14:53")

            val config = androidConfig(profile.config, descriptor.fd)
            val request = JSONObject()
                .put("apiVersion", 1)
                .put("method", "runXray")
                .put("payload", JSONObject().put("xrayJson", config.toString()))
            val response = JSONObject(LibXray.invoke(request.toString()))
            check(response.optBoolean("success")) { response.optString("error", "Xray start failed") }

            setRunning(true)
            startForeground(NOTIFICATION_ID, notification(profile.title))
        } catch (error: Throwable) {
            Log.e("VPNX", "VPN engine failed", error)
            setRunning(false)
            setDesiredRunning(false)
            stopNative()
            startForeground(NOTIFICATION_ID, notification("Ошибка: ${error.message ?: "неизвестно"}"))
            stopSelf()
        } finally {
            transitioning.set(false)
        }
    }

    private fun restartEngine() {
        stopNative()
        setRunning(false)
        startEngine()
    }

    private fun stopEngine(clearDesired: Boolean) {
        if (clearDesired) setDesiredRunning(false)
        stopNative()
        setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopNative() {
        runCatching {
            LibXray.invoke(JSONObject().put("apiVersion", 1).put("method", "stopXray").put("payload", JSONObject()).toString())
        }
        runCatching { LibXray.resetDNS() }
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun androidConfig(source: JSONObject, tunFd: Int): JSONObject {
        val config = JSONObject(source.toString())
        config.remove("remarks")
        config.put("env", JSONObject()
            .put("xray.tun.fd", tunFd.toString())
            .put("xray.location.asset", filesDir.absolutePath))
        config.put("inbounds", JSONArray().put(JSONObject()
            .put("tag", "vpnx-tun")
            .put("protocol", "tun")
            .put("port", 0)
            .put("settings", JSONObject().put("name", "vpnx0").put("mtu", 1500))))
        return config
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "VPNX", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, VpnxVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("VPNX")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, "Отключить", stopIntent)
            .build()
    }

    private fun setRunning(value: Boolean) {
        getSharedPreferences("vpnx", Context.MODE_PRIVATE).edit().putBoolean("running", value).apply()
        sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra(EXTRA_RUNNING, value))
    }

    private fun setDesiredRunning(value: Boolean) {
        getSharedPreferences("vpnx", Context.MODE_PRIVATE).edit().putBoolean("auto_start", value).apply()
    }

    private fun isDesiredRunning(): Boolean =
        getSharedPreferences("vpnx", Context.MODE_PRIVATE).getBoolean("auto_start", false)
}
