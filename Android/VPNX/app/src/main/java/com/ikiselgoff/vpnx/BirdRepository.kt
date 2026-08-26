package com.ikiselgoff.vpnx

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class BirdProfile(val id: String, val title: String, val config: JSONObject)
data class SyncResult(val changed: Boolean, val count: Int)

object BirdRepository {
    const val SUBSCRIPTION_URL = "https://moonshard.org/_DDgzQApDZfjQ2JA"
    private const val PREFS = "vpnx"
    private const val KEY_SNAPSHOT = "bird_snapshot"
    private const val KEY_SELECTED = "selected_profile"
    private const val KEY_SYNCED_AT = "synced_at"

    private val headers = mapOf(
        "X-HWID" to "1233d4ebec70a307",
        "X-Device-Locale" to "ru",
        "Accept-Language" to "ru",
        "Cache-Control" to "no-cache",
        "Pragma" to "no-cache",
        "X-Ver-OS" to "14",
        "X-Device-model" to "SM-X205",
        "User-Agent" to "Happ/4.7.1/android/2604040151590",
        "X-Device-OS" to "Android",
        "X-App-Version" to "4.7.1"
    )

    @Synchronized
    fun seedFromAssets(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_SNAPSHOT)) return false

        val payload = context.assets.open("bird-bootstrap.json").bufferedReader().use { it.readText() }
        val profiles = validate(payload)
        val selected = profiles.firstOrNull { it.id == "bird-auto-wifi" }?.id ?: profiles.first().id
        check(
            prefs.edit()
                .putString(KEY_SNAPSHOT, payload)
                .putString(KEY_SELECTED, selected)
                .commit()
        ) { "Unable to save bundled BIRD snapshot" }
        return true
    }

    @Synchronized
    fun sync(context: Context): SyncResult {
        val connection = URL(SUBSCRIPTION_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = true
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        val payload = connection.inputStream.bufferedReader().use { it.readText() }
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        val profiles = validate(payload)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val changed = prefs.getString(KEY_SNAPSHOT, null) != payload
        val editor = prefs.edit().putString(KEY_SNAPSHOT, payload).putLong(KEY_SYNCED_AT, System.currentTimeMillis())
        val selected = prefs.getString(KEY_SELECTED, null)
        if (selected == null || profiles.none { it.id == selected }) {
            editor.putString(KEY_SELECTED, profiles.firstOrNull { it.id == "bird-auto-wifi" }?.id ?: profiles.first().id)
        }
        check(editor.commit()) { "Unable to save BIRD snapshot" }
        return SyncResult(changed, profiles.size)
    }

    fun profiles(context: Context): List<BirdProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SNAPSHOT, null) ?: return emptyList()
        return parse(raw)
    }

    fun selected(context: Context): BirdProfile? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SELECTED, null)
        return profiles(context).firstOrNull { it.id == id } ?: profiles(context).firstOrNull()
    }

    fun select(context: Context, id: String) {
        require(profiles(context).any { it.id == id })
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SELECTED, id).apply()
    }

    fun syncedAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_SYNCED_AT, 0L)

    private fun parse(raw: String): List<BirdProfile> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val config = array.getJSONObject(index)
            val title = config.optString("remarks", "BIRD ${index + 1}")
            BirdProfile(stableId(title), title, config)
        }
    }

    private fun validate(raw: String): List<BirdProfile> {
        val parsed = JSONArray(raw)
        check(parsed.length() > 0) { "BIRD returned no profiles" }
        for (index in 0 until parsed.length()) {
            val config = parsed.getJSONObject(index)
            check(config.optJSONArray("inbounds")?.length() ?: 0 > 0) { "Invalid inbounds" }
            check(config.optJSONArray("outbounds")?.length() ?: 0 > 0) { "Invalid outbounds" }
        }
        return parse(raw)
    }

    private fun stableId(title: String): String {
        val aliases = linkedMapOf(
            "Auto WiFi" to "bird-auto-wifi", "Auto LTE" to "bird-auto-lte",
            "Нидерланды" to "bird-nl", "Германия" to "bird-de",
            "Соединенные Штаты" to "bird-us", "Франция" to "bird-fr",
            "Россия" to "bird-ru", "Швейцария" to "bird-ch",
            "Финляндия" to "bird-fi", "Казахстан" to "bird-kz",
            "Гонконг" to "bird-hk", "Индия" to "bird-in"
        )
        aliases.entries.firstOrNull { title.contains(it.key) }?.let { return it.value }
        val digest = MessageDigest.getInstance("SHA-256").digest(title.toByteArray())
        return "bird-" + digest.take(6).joinToString("") { "%02x".format(it) }
    }
}
