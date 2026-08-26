package com.ikiselgoff.vpnx

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

object ShizukuShell {
    private const val PERMISSION_REQUEST = 71
    private var service: IShellService? = null
    private var callback: (() -> Unit)? = null
    private var listenersInstalled = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IShellService.Stub.asInterface(binder)
            callback?.invoke()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            callback?.invoke()
        }
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
        if (requestCode == PERMISSION_REQUEST && result == PackageManager.PERMISSION_GRANTED) bind()
        callback?.invoke()
    }

    fun connect(context: Context, onChanged: () -> Unit) {
        callback = onChanged
        if (!listenersInstalled) {
            listenersInstalled = true
            Shizuku.addBinderReceivedListenerSticky { bind() }
            Shizuku.addBinderDeadListener {
                service = null
                callback?.invoke()
            }
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }
        if (isRunning()) bind()
    }

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isReady(): Boolean = runCatching { service?.uid() == 2000 || service?.uid() == 0 }.getOrDefault(false)

    fun requestPermission(context: Context) {
        if (!isRunning()) {
            context.startActivity(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api"))
            return
        }
        when (Shizuku.checkSelfPermission()) {
            PackageManager.PERMISSION_GRANTED -> bind()
            else -> Shizuku.requestPermission(PERMISSION_REQUEST)
        }
    }

    fun execute(command: String): String? = runCatching { service?.execute(command) }.getOrNull()

    private fun bind() {
        if (!isRunning() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return
        val args = Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, VpnxShellUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
        Shizuku.bindUserService(args, connection)
    }
}
