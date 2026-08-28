package com.ikiselgoff.vpnx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

class MaintenanceTunnelService : Service() {
    companion object {
        private const val CHANNEL_ID = "vpnx_maintenance"
        private const val NOTIFICATION_ID = 72
        private const val PRIVATE_KEY = "maintenance_id_rsa"
        private const val KNOWN_HOSTS = "maintenance_known_hosts"
        private const val CONTROL_TOKEN = "maintenance_control_token"
        private const val CONTROL_PORT = 8765

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MaintenanceTunnelService::class.java))
        }
    }

    private val tunnelExecutor = Executors.newFixedThreadPool(2)
    private val controlExecutor = Executors.newSingleThreadExecutor()
    private val controlClients = Executors.newCachedThreadPool()
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    private val started = AtomicBoolean(false)
    @Volatile private var adbSession: Session? = null
    @Volatile private var controlSession: Session? = null
    @Volatile private var controlServer: ServerSocket? = null
    @Volatile private var lastSyncAttempt = 0L

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "VPNX maintenance", NotificationManager.IMPORTANCE_MIN))
        ShizukuShell.connect(this) {}
        controlExecutor.execute(::controlLoop)
        watchdog.scheduleWithFixedDelay(::watchdogTick, 15, 60, TimeUnit.SECONDS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification("Запуск защищённого канала…"))
        if (started.compareAndSet(false, true)) {
            tunnelExecutor.execute { connectionLoop(25556, 5555) { adbSession = it } }
            tunnelExecutor.execute { connectionLoop(25557, CONTROL_PORT) { controlSession = it } }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        started.set(false)
        adbSession?.disconnect()
        controlSession?.disconnect()
        runCatching { controlServer?.close() }
        tunnelExecutor.shutdownNow()
        controlExecutor.shutdownNow()
        controlClients.shutdownNow()
        watchdog.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectionLoop(remotePort: Int, localPort: Int, store: (Session?) -> Unit) {
        while (started.get()) {
            var connected: Session? = null
            try {
                val key = File(filesDir, PRIVATE_KEY)
                val knownHosts = File(filesDir, KNOWN_HOSTS)
                val token = File(filesDir, CONTROL_TOKEN)
                if (!key.isFile || !knownHosts.isFile || !token.isFile) {
                    updateNotification("Ожидается ключ обслуживания")
                    Thread.sleep(30_000)
                    continue
                }

                val jsch = JSch().apply {
                    setKnownHosts(knownHosts.absolutePath)
                    addIdentity(key.absolutePath)
                }
                connected = jsch.getSession("root", "45.146.165.85", 22).apply {
                    setConfig("StrictHostKeyChecking", "yes")
                    setConfig("PreferredAuthentications", "publickey")
                    serverAliveInterval = 30_000
                    serverAliveCountMax = 3
                    timeout = 45_000
                    connect(20_000)
                    setPortForwardingR("127.0.0.1", remotePort, "127.0.0.1", localPort)
                }
                store(connected)
                updateNotification("Удалённая диагностика защищена")
                while (started.get() && connected?.isConnected == true) {
                    Thread.sleep(15_000)
                    connected?.sendKeepAliveMsg()
                }
            } catch (error: Throwable) {
                Log.e("VPNX", "Maintenance tunnel $remotePort failed", error)
                updateNotification("Канал восстанавливается…")
            } finally {
                connected?.disconnect()
                store(null)
            }
            if (started.get()) Thread.sleep(5_000)
        }
    }

    private fun controlLoop() {
        while (!controlExecutor.isShutdown) {
            try {
                ServerSocket(CONTROL_PORT, 4, InetAddress.getByName("127.0.0.1")).use { server ->
                    controlServer = server
                    while (!server.isClosed) {
                        val socket = server.accept()
                        controlClients.execute { socket.use(::handleControl) }
                    }
                }
            } catch (error: Throwable) {
                if (!controlExecutor.isShutdown) Log.e("VPNX", "Maintenance control server failed", error)
            } finally {
                controlServer = null
            }
            if (!controlExecutor.isShutdown) Thread.sleep(2_000)
        }
    }

    private fun handleControl(socket: Socket) {
        socket.soTimeout = 15_000
        val reader = socket.getInputStream().bufferedReader()
        val writer = socket.getOutputStream().bufferedWriter()
        val expected = File(filesDir, CONTROL_TOKEN).takeIf(File::isFile)?.readText()?.trim()
        val supplied = reader.readLine()?.trim()
        val command = reader.readLine()?.trim()?.uppercase()
        val response = when {
            expected.isNullOrEmpty() || supplied != expected -> JSONObject().put("ok", false).put("error", "unauthorized")
            command == "STATUS" -> status()
            command == "SYNC" -> runCatching { BirdRepository.sync(this) }
                .fold({ JSONObject().put("ok", true).put("profiles", it.count).put("changed", it.changed) }, ::error)
            command == "RESTART_VPN" -> {
                ContextCompat.startForegroundService(this, Intent(this, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_SWITCH))
                JSONObject().put("ok", true)
            }
            command == "RESTORE_ADB_TCP" && ShizukuShell.isReady() -> {
                val result = ShizukuShell.execute("(setprop service.adb.tcp.port 5555; stop adbd; start adbd) >/dev/null 2>&1 &")
                JSONObject().put("ok", result != null).put("result", result ?: "shizuku unavailable")
            }
            command == "RESTORE_ADB_TCP" -> JSONObject().put("ok", false).put("error", "shizuku unavailable")
            else -> JSONObject().put("ok", false).put("error", "unsupported command")
        }
        writer.write(response.toString())
        writer.newLine()
        writer.flush()
    }

    private fun status(): JSONObject {
        val prefs = getSharedPreferences("vpnx", Context.MODE_PRIVATE)
        return JSONObject()
            .put("ok", true)
            .put("vpnRunning", prefs.getBoolean("running", false))
            .put("vpnDesired", prefs.getBoolean("auto_start", false))
            .put("profile", BirdRepository.selected(this)?.title ?: JSONObject.NULL)
            .put("profiles", BirdRepository.profiles(this).size)
            .put("syncedAt", BirdRepository.syncedAt(this))
            .put("shizukuRunning", ShizukuShell.isRunning())
            .put("shizukuReady", ShizukuShell.isReady())
    }

    private fun error(error: Throwable) = JSONObject().put("ok", false).put("error", error.message ?: error.javaClass.simpleName)

    private fun watchdogTick() {
        runCatching {
            ShizukuShell.connect(this) {}
            val prefs = getSharedPreferences("vpnx", Context.MODE_PRIVATE)
            if (prefs.getBoolean("auto_start", false) && !prefs.getBoolean("running", false)) {
                ContextCompat.startForegroundService(this, Intent(this, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_START))
            }
            val now = System.currentTimeMillis()
            if (now - BirdRepository.syncedAt(this) > 60 * 60 * 1000L && now - lastSyncAttempt > 15 * 60 * 1000L) {
                lastSyncAttempt = now
                runCatching { BirdRepository.sync(this) }.onFailure { Log.e("VPNX", "Watchdog BIRD sync failed", it) }
            }
        }.onFailure { Log.e("VPNX", "Maintenance watchdog failed", it) }
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setContentTitle("VPNX")
        .setContentText(text)
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }
}
