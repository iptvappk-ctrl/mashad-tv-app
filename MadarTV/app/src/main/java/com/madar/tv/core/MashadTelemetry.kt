package com.madar.tv.core

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object MashadTelemetry {

    // ═══════════════════════════════════════════════════════════════════════
    // ALL CONSTANTS — 6-layer encrypted (XOR→ROT→XOR→Caesar→Nibble→PosXOR)
    // No plain-text string literals exist in this file.
    // ═══════════════════════════════════════════════════════════════════════

    private val _tpu = byteArrayOf(129.toByte(), 158.toByte(), 154.toByte(), 44, 163.toByte(), 56, 196.toByte(), 62, 205.toByte(), 64, 219.toByte(), 214.toByte(), 233.toByte(), 104, 110, 240.toByte(), 142.toByte(), 134.toByte(), 138.toByte(), 26, 158.toByte(), 33)
    private val _tdi = byteArrayOf(5, 156.toByte(), 20, 172.toByte(), 160.toByte(), 184.toByte(), 196.toByte(), 176.toByte(), 77)
    private val _tfo = byteArrayOf(10, 154.toByte(), 26, 163.toByte(), 41, 195.toByte(), 188.toByte(), 60, 205.toByte(), 71, 160.toByte(), 90, 239.toByte(), 228.toByte(), 231.toByte(), 247.toByte(), 125, 14, 7, 144.toByte())
    private val _toc = byteArrayOf(13, 159.toByte(), 27, 168.toByte(), 37, 195.toByte(), 188.toByte(), 60, 205.toByte(), 71, 160.toByte(), 211.toByte(), 234.toByte(), 232.toByte(), 100, 118)
    private val _tsu = byteArrayOf(3, 20, 27, 32, 12, 155.toByte(), 156.toByte(), 156.toByte(), 103, 103, 113, 113, 64, 67, 78, 83, 93, 166.toByte(), 171.toByte(), 176.toByte(), 50, 42, 165.toByte(), 32)
    private val _tap = byteArrayOf(135.toByte(), 22, 25, 46, 167.toByte(), 185.toByte(), 189.toByte(), 62, 203.toByte(), 199.toByte(), 88, 241.toByte(), 108, 233.toByte(), 228.toByte(), 125, 216.toByte(), 174.toByte(), 2, 156.toByte(), 19, 161.toByte(), 38, 38, 181.toByte(), 24, 67, 202.toByte(), 214.toByte(), 116, 249.toByte())
    private val _tin = byteArrayOf(166.toByte(), 154.toByte(), 16, 163.toByte(), 41, 186.toByte(), 59, 50)
    private val _tss = byteArrayOf(166.toByte(), 149.toByte(), 147.toByte(), 163.toByte(), 168.toByte(), 190.toByte(), 188.toByte(), 53, 238.toByte(), 205.toByte(), 83, 208.toByte(), 96, 104)
    private val _tse = byteArrayOf(166.toByte(), 149.toByte(), 147.toByte(), 163.toByte(), 168.toByte(), 190.toByte(), 188.toByte(), 53, 238.toByte(), 196.toByte(), 88, 82)
    private val _thb = byteArrayOf(166.toByte(), 26, 147.toByte(), 168.toByte(), 40, 48, 62, 182.toByte(), 207.toByte(), 76)
    private val _tvw = byteArrayOf(166.toByte(), 27, 149.toByte(), 170.toByte(), 166.toByte())
    private val _tsr = byteArrayOf(166.toByte(), 149.toByte(), 147.toByte(), 168.toByte(), 40, 185.toByte(), 57)

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(42, 201.toByte(), 77, 13, 58, 190.toByte())
    @Suppress("unused") private val _dc2 = byteArrayOf(139.toByte(), 3, 27, 115, 65, 51, 127, 9)
    @Suppress("unused") private val _dc3 = byteArrayOf(91, 211.toByte(), 99, 123, 19, 65, 73, 59)

    // ── Lazily resolved secrets ───────────────────────────────────────────
    private val PREFS_NAME:   String by lazy { X.s(_tpu) }
    private val KEY_DID:      String by lazy { X.s(_tdi) }
    private val KEY_FOPEN:    String by lazy { X.s(_tfo) }
    private val KEY_OCOUNT:   String by lazy { X.s(_toc) }
    private val SERVER_URL:   String by lazy { X.s(_tsu) }
    private val JSON_MT:      String by lazy { X.s(_tap) }
    private val EP_INSTALL:   String by lazy { X.s(_tin) }
    private val EP_SS:        String by lazy { X.s(_tss) }
    private val EP_SE:        String by lazy { X.s(_tse) }
    private val EP_HB:        String by lazy { X.s(_thb) }
    private val EP_VIEW:      String by lazy { X.s(_tvw) }
    private val EP_SEARCH:    String by lazy { X.s(_tsr) }

    private var prefs: SharedPreferences? = null
    private var deviceId: String = ""
    private var isFirstOpen: Boolean = false
    private var sessionStartTime: Long = 0L
    private var currentSessionId: String = ""
    private var heartbeatJob: Job? = null
    private var detectedCountry: String = "مصر"
    private var detectedDeviceType: String = "هاتف أندرويد"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        if (prefs != null) return
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Detect country and device type
        val loc = Locale.getDefault()
        detectedCountry = loc.getDisplayCountry(Locale.forLanguageTag("ar")).ifBlank { "مصر" }
        val isTv = (appContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        detectedDeviceType = if (isTv) "Android TV 📺" else "هاتف 📱"

        // Get or generate persistent unique device ID
        val savedId = prefs?.getString(KEY_DID, null)
        if (savedId == null) {
            deviceId = "m_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs?.edit()?.putString(KEY_DID, deviceId)?.apply()
            prefs?.edit()?.putLong(KEY_FOPEN, System.currentTimeMillis())?.apply()
            isFirstOpen = true
        } else {
            deviceId = savedId
            isFirstOpen = false
        }
    }

    fun onAppOpen() {
        sessionStartTime = System.currentTimeMillis()
        currentSessionId = "s_" + UUID.randomUUID().toString().replace("-", "").take(10)

        val openCount = (prefs?.getInt(KEY_OCOUNT, 0) ?: 0) + 1
        prefs?.edit()?.putInt(KEY_OCOUNT, openCount)?.apply()

        scope.launch {
            runCatching {
                if (isFirstOpen) {
                    val installJson = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                        put("deviceType", detectedDeviceType)
                        put("country", detectedCountry)
                    }
                    postToServer("$SERVER_URL${EP_INSTALL}", installJson.toString())
                }

                val sessionJson = JSONObject().apply {
                    put("sessionId", currentSessionId)
                    put("deviceId", deviceId)
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("deviceType", detectedDeviceType)
                    put("country", detectedCountry)
                    put("section", "الرئيسية 🎬")
                }
                postToServer("$SERVER_URL${EP_SS}", sessionJson.toString())
            }
        }

        startHeartbeat()
    }

    fun onAppClose() {
        stopHeartbeat()
        val closeTime = System.currentTimeMillis()
        val durationSeconds = if (sessionStartTime > 0) (closeTime - sessionStartTime) / 1000 else 0

        scope.launch {
            runCatching {
                val endJson = JSONObject().apply {
                    put("sessionId", currentSessionId)
                    put("durationSeconds", durationSeconds)
                }
                postToServer("$SERVER_URL${EP_SE}", endJson.toString())
            }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                runCatching {
                    val pingJson = JSONObject().apply {
                        put("deviceId", deviceId)
                    }
                    postToServer("$SERVER_URL${EP_HB}", pingJson.toString())
                }
                delay(30_000L)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun trackContentView(title: String, type: String, category: String, url: String) {
        // Increment local preference in SharedPreferences
        val catKey = "pref_$category"
        val count = (prefs?.getInt(catKey, 0) ?: 0) + 1
        prefs?.edit()?.putInt(catKey, count)?.apply()

        scope.launch {
            runCatching {
                val viewJson = JSONObject().apply {
                    put("title", title)
                    put("type", type)
                    put("category", category)
                    put("url", url)
                }
                postToServer("$SERVER_URL${EP_VIEW}", viewJson.toString())
            }
        }
    }

    fun trackLiveMatchView(matchTitle: String, league: String) {
        trackContentView(title = matchTitle, type = "FOOTBALL_MATCH", category = league, url = "")
    }

    fun trackSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return

        scope.launch {
            runCatching {
                val searchJson = JSONObject().apply {
                    put("query", trimmed)
                }
                postToServer("$SERVER_URL${EP_SEARCH}", searchJson.toString())
            }
        }
    }

    private fun postToServer(endpoint: String, jsonString: String) {
        try {
            val reqBody = jsonString.toRequestBody(JSON_MT.toMediaType())
            val request = Request.Builder().url(endpoint).post(reqBody).build()
            httpClient.newCall(request).execute().close()
        } catch (_: Exception) {
            // Silently fallback if server is offline
        }
    }
}
