package com.madar.tv.data

import android.util.Base64
import com.madar.tv.core.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YacineRepository {

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy encoded constants; values are recoverable from the client APK.
    // Pipeline: XOR(K1)→ROT-L(3)→XOR(K2)→Caesar(+17)→Nibble-Swap→PosXOR(K3)
    // ═══════════════════════════════════════════════════════════════════════

    private val _bu = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 77, 196.toByte(), 84, 120, 67, 202.toByte(), 68, 240.toByte(), 244.toByte(), 143.toByte(), 1, 144.toByte(), 23, 139.toByte(), 46, 37, 57, 23, 73, 200.toByte(), 95)
    private val _sk = byteArrayOf(132.toByte(), 190.toByte(), 29, 87, 36, 157.toByte(), 76, 152.toByte(), 98, 187.toByte(), 41, 162.toByte(), 110, 18, 96, 249.toByte())
    private val _pn = byteArrayOf(2, 156.toByte(), 26, 131.toByte(), 2, 182.toByte(), 190.toByte(), 53, 69, 194.toByte(), 92, 212.toByte(), 74, 227.toByte(), 104, 113)
    private val _ua = byteArrayOf(134.toByte(), 153.toByte(), 21, 34, 41, 50, 156.toByte(), 30, 110, 238.toByte(), 241.toByte(), 113, 67)

    // ── Decoys ────────────────────────────────────────────────────────────
    @Suppress("unused") private val _dc1 = byteArrayOf(19, 243.toByte(), 107, 91, 99, 235.toByte(), 161.toByte(), 73)
    @Suppress("unused") private val _dc2 = byteArrayOf(139.toByte(), 3, 27, 115, 65, 51, 127, 9, 73, 211.toByte())
    @Suppress("unused") private val _dc3 = byteArrayOf(91, 211.toByte(), 99, 123, 19, 65, 73, 59, 51, 115, 27)

    // ── Lazily resolved secrets ───────────────────────────────────────────
    private val BASE_URL: String by lazy { X.s(_bu) }
    private val HARDCODED_KEY: String by lazy { X.s(_sk) }
    private val PKG_NAME: String by lazy { X.s(_pn) }
    private val UA: String by lazy { X.s(_ua) }

    private val connectionPool = okhttp3.ConnectionPool(16, 5, TimeUnit.MINUTES)

    private val client = OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // ─── Caches for High Concurrency ─────────────────────────────────────────────
    private var cachedCategories: List<YacineCategory> = emptyList()
    private var lastCategoriesFetch = 0L
    private const val CATEGORIES_CACHE_TTL = 10 * 60 * 1000L // 10 minutes

    private val cachedChannels = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<YacineChannel>>>()
    private const val CHANNELS_CACHE_TTL = 10 * 60 * 1000L // 10 minutes

    private var cachedEvents: List<YacineEvent> = emptyList()
    private var lastEventsFetch = 0L
    private const val EVENTS_CACHE_TTL = 45 * 1000L // 45 seconds

    // ─── Decryption ─────────────────────────────────────────────────────────────
    private fun decrypt(body: String, tHeader: String): String {
        val key = (HARDCODED_KEY + tHeader).toByteArray(Charsets.UTF_8)
        val decoded = Base64.decode(body.trim(), Base64.DEFAULT)
        val result = ByteArray(decoded.size) { i ->
            (decoded[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }

    // ─── HTTP Helper ─────────────────────────────────────────────────────────────
    private fun get(endpoint: String): Pair<String, String> {
        val request = Request.Builder()
            .url("$BASE_URL/$endpoint")
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .header("x-android-package", PKG_NAME)
            .build()
        val response = client.newCall(request).execute()
        val tHeader = response.header("T") ?: response.header("t") ?: ""
        val body = response.body?.string() ?: ""
        response.close()
        return Pair(body, tHeader)
    }

    private fun decryptResponse(endpoint: String): String {
        val (body, t) = get(endpoint)
        return decrypt(body, t)
    }

    // ─── Categories ──────────────────────────────────────────────────────────────
    suspend fun getCategories(forceRefresh: Boolean = false): List<YacineCategory> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedCategories.isNotEmpty() && (now - lastCategoriesFetch < CATEGORIES_CACHE_TTL)) {
            return@withContext cachedCategories
        }
        runCatching {
            val json = JSONObject(decryptResponse("categories"))
            val arr = json.getJSONArray("data")
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                YacineCategory(
                    id = obj.optString("id", ""),
                    name = obj.getString("name"),
                    logo = obj.optString("logo", ""),
                    childCount = obj.optInt("child_count", 0)
                )
            }
            if (list.isNotEmpty()) {
                cachedCategories = list
                lastCategoriesFetch = now
            }
            list
        }.getOrElse { cachedCategories }
    }

    // ─── Channels in a category ───────────────────────────────────────────────────
    suspend fun getChannels(categoryId: String, forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            cachedChannels[categoryId]?.let { (time, channels) ->
                if (now - time < CHANNELS_CACHE_TTL && channels.isNotEmpty()) {
                    return@withContext channels
                }
            }
        }
        runCatching {
            val raw = decryptResponse("categories/$categoryId/channels")
            // Response is a JSON array
            val arr = if (raw.trim().startsWith("[")) JSONArray(raw)
                      else JSONObject(raw).getJSONArray("data")
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                YacineChannel(
                    id = obj.optString("id", ""),
                    name = obj.getString("name"),
                    logo = obj.optString("logo", ""),
                    isHide = obj.optInt("is_hide", 0),
                    priority = obj.optInt("priority", 0)
                )
            }.filter { it.isHide == 0 }
            if (list.isNotEmpty()) {
                cachedChannels[categoryId] = Pair(now, list)
            }
            list
        }.getOrElse { cachedChannels[categoryId]?.second ?: emptyList() }
    }

    // ─── Events ──────────────────────────────────────────────────────────────────
    suspend fun getEvents(forceRefresh: Boolean = false): List<YacineEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedEvents.isNotEmpty() && (now - lastEventsFetch < EVENTS_CACHE_TTL)) {
            return@withContext cachedEvents
        }
        runCatching {
            val json = JSONObject(decryptResponse("events"))
            val arr = json.getJSONArray("data")
            val allEvents = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val t1 = obj.optJSONObject("team_1") ?: JSONObject()
                val t2 = obj.optJSONObject("team_2") ?: JSONObject()
                YacineEvent(
                    id = obj.optString("id", ""),
                    champions = obj.optString("champions", ""),
                    channel = obj.optString("channel", ""),
                    startTime = obj.optLong("start_time", 0L),
                    endTime = obj.optLong("end_time", 0L),
                    commentary = obj.optString("commentary", ""),
                    team1 = YacineTeam(
                        id = t1.optString("id", ""),
                        name = t1.optString("name", ""),
                        logo = t1.optString("logo", "")
                    ),
                    team2 = YacineTeam(
                        id = t2.optString("id", ""),
                        name = t2.optString("name", ""),
                        logo = t2.optString("logo", "")
                    )
                )
            }.filter { it.id.isNotEmpty() }
            val currentSec = now / 1000
            val sorted = allEvents.sortedWith(
                compareBy<YacineEvent> {
                    if (currentSec in it.startTime..it.endTime) 0 else 1
                }.thenBy { it.startTime }
            )
            cachedEvents = sorted
            lastEventsFetch = now
            sorted
        }.getOrElse { cachedEvents }
    }

    // ─── Sports Channels Helper (beIN SPORTS / Alkass) ───────────────────────────
    suspend fun getSportsChannels(): List<YacineChannel> = withContext(Dispatchers.IO) {
        runCatching {
            val cats = getCategories()
            val sportsCat = cats.firstOrNull { it.name.contains("beIN SPORTS", ignoreCase = true) }
                ?: cats.firstOrNull { it.name.contains("SPORTS", ignoreCase = true) }
                ?: cats.firstOrNull()
            if (sportsCat != null) {
                getChannels(sportsCat.id)
            } else emptyList()
        }.getOrElse { emptyList() }
    }

    // ─── Event Streams ────────────────────────────────────────────────────────────
    suspend fun getEventStreams(eventId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(decryptResponse("event/$eventId"))
            parseStreams(json.getJSONArray("data"))
        }.getOrElse { emptyList() }
    }

    // ─── Channel Streams ──────────────────────────────────────────────────────────
    suspend fun getChannelStreams(channelId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(decryptResponse("channel/$channelId"))
            parseStreams(json.getJSONArray("data"))
        }.getOrElse { emptyList() }
    }

    private fun parseStreams(arr: JSONArray): List<YacineStream> {
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val headersObj = obj.optJSONObject("headers")
            val ua = headersObj?.optString("User-Agent", "")
                ?: obj.optString("user_agent", UA)
            YacineStream(
                name = obj.optString("name", "HD"),
                url = obj.optString("url", ""),
                userAgent = ua.ifEmpty { UA },
                referer = obj.optString("referer", ""),
                urlType = obj.optInt("url_type", 3)
            )
        }.filter { it.url.isNotEmpty() }
    }
}
