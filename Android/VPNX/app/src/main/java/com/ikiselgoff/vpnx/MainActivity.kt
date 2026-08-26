package com.ikiselgoff.vpnx

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var syncStatus: TextView
    private lateinit var shellStatus: TextView
    private lateinit var profilesBox: LinearLayout
    private lateinit var progress: ProgressBar

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        SyncScheduler.schedule(this)
        MaintenanceTunnelService.start(this)
        ShizukuShell.connect(this) { runOnUiThread { renderShell() } }
        if (Build.VERSION.SDK_INT >= 33) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9)
        syncProfiles()
        handleAutomationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAutomationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, stateReceiver, IntentFilter(VpnxVpnService.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        render()
    }

    override fun onStop() {
        unregisterReceiver(stateReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(7, 21, 53)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(34), dp(28), dp(42))
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "VPNX"
            textSize = 38f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans", Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "BIRD NETWORK CONTROL"
            textSize = 12f
            letterSpacing = .18f
            setTextColor(Color.rgb(24, 199, 244))
        }, params(top = 0, bottom = 28))

        status = TextView(this).apply { textSize = 20f; setTextColor(Color.WHITE) }
        root.addView(status)
        toggle = Button(this).apply {
            isAllCaps = false
            textSize = 18f
            setOnClickListener { toggleVpn() }
        }
        root.addView(toggle, params(top = 14, bottom = 20, height = 58))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("Обновить") { syncProfiles() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(8) })
        actions.addView(actionButton("Диагностика") { diagnose() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(8) })
        root.addView(actions)

        progress = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(progress, params(top = 10))
        syncStatus = TextView(this).apply { textSize = 13f; setTextColor(Color.rgb(159, 180, 201)) }
        root.addView(syncStatus, params(top = 8, bottom = 24))

        shellStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(159, 180, 201))
            setOnClickListener { ShizukuShell.requestPermission(this@MainActivity) }
        }
        root.addView(shellStatus, params(bottom = 18))

        root.addView(TextView(this).apply {
            text = "ПРОФИЛИ BIRD"
            textSize = 13f
            letterSpacing = .12f
            setTextColor(Color.rgb(255, 148, 24))
            typeface = Typeface.DEFAULT_BOLD
        }, params(bottom = 10))
        profilesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(profilesBox)
        setContentView(scroll)
    }

    private fun render() {
        val prefs = getSharedPreferences("vpnx", Context.MODE_PRIVATE)
        val running = prefs.getBoolean("running", false)
        val selected = BirdRepository.selected(this)
        status.text = if (running) "Подключено · ${selected?.title ?: "BIRD"}" else "VPN отключён"
        toggle.text = if (running) "Отключить VPN" else "Подключить VPN"
        toggle.setBackgroundColor(if (running) Color.rgb(255, 148, 24) else Color.rgb(24, 199, 244))
        val syncedAt = BirdRepository.syncedAt(this)
        syncStatus.text = if (syncedAt > 0) "Последнее обновление: ${DateFormat.getDateFormat(this).format(Date(syncedAt))} ${DateFormat.getTimeFormat(this).format(Date(syncedAt))}" else "Подписка ещё не загружена"
        renderShell()

        profilesBox.removeAllViews()
        BirdRepository.profiles(this).forEach { profile ->
            val selectedNow = profile.id == selected?.id
            profilesBox.addView(TextView(this).apply {
                text = (if (selectedNow) "●  " else "○  ") + profile.title
                textSize = 17f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(15), dp(16), dp(15))
                setTextColor(if (selectedNow) Color.rgb(24, 199, 244) else Color.WHITE)
                setBackgroundColor(Color.rgb(16, 38, 76))
                setOnClickListener {
                    BirdRepository.select(this@MainActivity, profile.id)
                    if (running) ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_SWITCH))
                    render()
                }
            }, params(bottom = 7, height = 58))
        }
    }

    private fun toggleVpn() {
        val running = getSharedPreferences("vpnx", Context.MODE_PRIVATE).getBoolean("running", false)
        if (running) {
            startService(Intent(this, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_STOP))
            return
        }
        val permission = VpnService.prepare(this)
        if (permission != null) startActivityForResult(permission, 44) else startVpn()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 44 && resultCode == RESULT_OK) startVpn()
    }

    private fun startVpn() {
        ContextCompat.startForegroundService(this, Intent(this, VpnxVpnService::class.java).setAction(VpnxVpnService.ACTION_START))
    }

    private fun handleAutomationIntent(intent: Intent?) {
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("connect", false) == true) {
            window.decorView.post { startVpn() }
        }
    }

    private fun syncProfiles() {
        progress.visibility = View.VISIBLE
        syncStatus.text = "Загружаю BIRD…"
        executor.execute {
            val result = runCatching { BirdRepository.sync(this) }
            runOnUiThread {
                progress.visibility = View.GONE
                render()
                result.onFailure {
                    Log.e("VPNX", "BIRD sync failed", it)
                    syncStatus.text = "Ошибка обновления: ${it.message ?: it.javaClass.simpleName}"
                }
            }
        }
    }

    private fun diagnose() {
        progress.visibility = View.VISIBLE
        syncStatus.text = "Проверяю VPN и Telegram…"
        executor.execute {
            val ip = fetchText("https://api.ipify.org")
            val telegram = fetchCode("https://api.telegram.org")
            val shell = ShizukuShell.execute("id; getprop ro.build.version.release")
            runOnUiThread {
                progress.visibility = View.GONE
                syncStatus.text = "Диагностика: IP ${ip ?: "ошибка"} · Telegram ${telegram ?: "ошибка"}" +
                    (shell?.lineSequence()?.firstOrNull()?.let { " · $it" } ?: "")
            }
        }
    }

    private fun renderShell() {
        if (!::shellStatus.isInitialized) return
        shellStatus.text = when {
            ShizukuShell.isReady() -> "Расширенная диагностика: shell подключён"
            ShizukuShell.isRunning() -> "Расширенная диагностика: нажмите, чтобы разрешить VPNX"
            else -> "Расширенная диагностика: запустите Shizuku"
        }
    }

    private fun fetchText(url: String): String? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 12_000 }.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    private fun fetchCode(url: String): Int? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 12_000; instanceFollowRedirects = false }.responseCode
    }.getOrNull()

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(16, 38, 76))
        setOnClickListener { action() }
    }

    private fun params(top: Int = 0, bottom: Int = 0, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, if (height > 0) dp(height) else height).apply {
            topMargin = dp(top); bottomMargin = dp(bottom)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
