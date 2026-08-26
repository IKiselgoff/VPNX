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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MaintenanceTunnelService : Service() {
    companion object {
        private const val CHANNEL_ID = "vpnx_maintenance"
        private const val NOTIFICATION_ID = 72
        private const val PRIVATE_KEY = "maintenance_id_rsa"
        private const val KNOWN_HOSTS = "maintenance_known_hosts"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MaintenanceTunnelService::class.java))
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    @Volatile private var session: Session? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "VPNX maintenance", NotificationManager.IMPORTANCE_MIN))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification("Запуск защищённого канала…"))
        if (started.compareAndSet(false, true)) executor.execute(::connectionLoop)
        return START_STICKY
    }

    override fun onDestroy() {
        started.set(false)
        session?.disconnect()
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun connectionLoop() {
        while (started.get()) {
            try {
                val key = File(filesDir, PRIVATE_KEY)
                val knownHosts = File(filesDir, KNOWN_HOSTS)
                if (!key.isFile || !knownHosts.isFile) {
                    updateNotification("Ожидается ключ обслуживания")
                    Thread.sleep(30_000)
                    continue
                }

                val jsch = JSch().apply {
                    setKnownHosts(knownHosts.absolutePath)
                    addIdentity(key.absolutePath)
                }
                val connected = jsch.getSession("root", "45.146.165.85", 22).apply {
                    setConfig("StrictHostKeyChecking", "yes")
                    setConfig("PreferredAuthentications", "publickey")
                    serverAliveInterval = 30_000
                    serverAliveCountMax = 3
                    connect(20_000)
                    setPortForwardingR("127.0.0.1", 25556, "127.0.0.1", 5555)
                }
                session = connected
                updateNotification("Удалённая диагностика защищена")
                while (started.get() && connected.isConnected) Thread.sleep(5_000)
            } catch (error: Throwable) {
                Log.e("VPNX", "Maintenance tunnel failed", error)
                updateNotification("Канал восстанавливается…")
            } finally {
                session?.disconnect()
                session = null
            }
            if (started.get()) Thread.sleep(5_000)
        }
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
