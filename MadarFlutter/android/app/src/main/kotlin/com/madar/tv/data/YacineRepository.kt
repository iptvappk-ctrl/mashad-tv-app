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
    private const val EVENTS_DISK_STALE_MS = 6L * 60 * 60 * 1000

    fun getCachedCategories(): List<YacineCategory>? {
        if (cachedCategories.isNotEmpty()) return cachedCategories
        val rows = CatalogDiskCache.loadJson("yacine:categories")?.value?.optJSONArray("items") ?: return null
        val cached = buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                add(YacineCategory(row.optString("id"), row.optString("name"), row.optString("logo"), row.optInt("childCount")))
            }
        }
        if (cached.isNotEmpty()) cachedCategories = cached
        return cached.takeIf { it.isNotEmpty() }
    }

    fun getCachedChannels(categoryId: String): List<YacineChannel>? {
        cachedChannels[categoryId]?.let { return it.second }
        val rows = CatalogDiskCache.loadJson("yacine:channels:$categoryId")?.value?.optJSONArray("items") ?: return null
        val cached = buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                add(YacineChannel(row.optString("id"), row.optString("name"), row.optString("logo"), row.optInt("isHide"), row.optInt("priority")))
            }
        }
        if (cached.isNotEmpty()) cachedChannels[categoryId] = System.currentTimeMillis() to cached
        return cached.takeIf { it.isNotEmpty() }
    }

    fun getCachedEvents(): List<YacineEvent>? {
        if (cachedEvents.isNotEmpty()) return cachedEvents
        val rows = CatalogDiskCache.loadJson("yacine:events", EVENTS_DISK_STALE_MS)?.value?.optJSONArray("items") ?: return null
        val cached = buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val team1 = row.optJSONObject("team1") ?: JSONObject()
                val team2 = row.optJSONObject("team2") ?: JSONObject()
                add(YacineEvent(
                    row.optString("id"), row.optString("champions"), row.optString("channel"),
                    row.optLong("startTime"), row.optLong("endTime"), row.optString("commentary"),
                    YacineTeam(team1.optString("id"), team1.optString("name"), team1.optString("logo")),
                    YacineTeam(team2.optString("id"), team2.optString("name"), team2.optString("logo")),
                ))
            }
        }
        if (cached.isNotEmpty()) cachedEvents = cached
        return cached.takeIf { it.isNotEmpty() }
    }

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
                val rows = JSONArray().apply {
                    list.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("logo", it.logo).put("childCount", it.childCount)) }
                }
                CatalogDiskCache.saveJson("yacine:categories", JSONObject().put("items", rows))
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
                val rows = JSONArray().apply {
                    list.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("logo", it.logo).put("isHide", it.isHide).put("priority", it.priority)) }
                }
                CatalogDiskCache.saveJson("yacine:channels:$categoryId", JSONObject().put("items", rows))
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
            val rows = JSONArray().apply {
                sorted.forEach { event ->
                    fun team(team: YacineTeam) = JSONObject().put("id", team.id).put("name", team.name).put("logo", team.logo)
                    put(JSONObject()
                        .put("id", event.id).put("champions", event.champions).put("channel", event.channel)
                        .put("startTime", event.startTime).put("endTime", event.endTime).put("commentary", event.commentary)
                        .put("team1", team(event.team1)).put("team2", team(event.team2)))
                }
            }
            CatalogDiskCache.saveJson("yacine:events", JSONObject().put("items", rows))
            sorted
        }.getOrElse { cachedEvents }
    }

    // ─── Sports Channels Helper (beIN SPORTS / Alkass) ───────────────────────────
    suspend fun getSportsChannels(forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {
        runCatching {
            val cats = getCategories(forceRefresh)
            val sportsCat = cats.firstOrNull { it.name.contains("beIN SPORTS", ignoreCase = true) }
                ?: cats.firstOrNull { it.name.contains("SPORTS", ignoreCase = true) }
                ?: cats.firstOrNull()
            if (sportsCat != null) {
                getChannels(sportsCat.id, forceRefresh)
            } else emptyList()
        }.getOrElse { emptyList() }
    }

    // ─── Event Streams ────────────────────────────────────────────────────────────
    suspend fun getEventStreams(eventId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        runCatching {
            parseStreamPayload(decryptResponse("event/$eventId"))
        }.getOrElse { emptyList() }
    }

    /** Prefer event-specific streams, then include the event's scheduled TV channel as a
     * working alternate. Some events publish no event stream even while their channel is live. */
    suspend fun getMatchStreams(
        eventId: String,
        channelHint: String,
        leagueHint: String,
    ): List<YacineStream> = withContext(Dispatchers.IO) {
        val eventStreams = runCatching { getEventStreams(eventId) }.getOrDefault(emptyList())
        val channels = runCatching { getSportsChannels() }.getOrDefault(emptyList())
        val hint = channelHint.trim()
        val normalizedHint = normalizeChannelName(hint)
        val target = channels.firstOrNull { channel ->
            val name = normalizeChannelName(channel.name)
            normalizedHint.isNotBlank() && name.isNotBlank() &&
                (name.contains(normalizedHint) || normalizedHint.contains(name))
        } ?: run {
            val league = leagueHint.lowercase()
            val preferredNumber = when {
                league.contains("فرنسي") || league.contains("ligue 1") -> "4"
                league.contains("ألماني") || league.contains("bundesliga") -> "5"
                league.contains("إسباني") || league.contains("la liga") -> "3"
                league.contains("إنجليزي") || league.contains("premier") -> "1"
                else -> ""
            }
            channels.firstOrNull { channel ->
                preferredNumber.isNotBlank() && channel.name.contains("beIN", true) &&
                    channel.name.contains(preferredNumber)
            } ?: channels.firstOrNull { it.name.contains("beIN", true) }
                ?: channels.firstOrNull()
        }
        val channelStreams = target?.let { channel ->
            runCatching { getChannelStreams(channel.id) }.getOrDefault(emptyList())
                .map { stream -> stream.copy(name = "${channel.name} · ${stream.name}") }
        }.orEmpty()
        (eventStreams + channelStreams)
            .filter { it.url.isNotBlank() }
            .distinctBy { it.url.trim() }
    }

    // ─── Channel Streams ──────────────────────────────────────────────────────────
    suspend fun getChannelStreams(channelId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        runCatching {
            parseStreamPayload(decryptResponse("channel/$channelId"))
        }.getOrElse { emptyList() }
    }

    private fun parseStreamPayload(raw: String): List<YacineStream> {
        val clean = raw.trim()
        val rows = when {
            clean.startsWith("[") -> JSONArray(clean)
            clean.startsWith("{") -> {
                val json = JSONObject(clean)
                json.optJSONArray("data") ?: json.optJSONArray("streams") ?: JSONArray()
            }
            else -> JSONArray()
        }
        return parseStreams(rows)
    }

    private fun normalizeChannelName(value: String): String = value
        .lowercase()
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace(Regex("[\\u0622\\u0623\\u0625]"), "ا")
        .replace(Regex("[^a-z0-9\\u0600-\\u06ff]"), "")

    private fun parseStreams(arr: JSONArray): List<YacineStream> {
        val browserUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val headersObj = obj.optJSONObject("headers")
            val rawUa = headersObj?.optString("User-Agent", "").orEmpty()
                .ifBlank { obj.optString("user_agent", "") }
                .ifBlank { browserUa }
            val ua = if (rawUa.startsWith("okhttp", ignoreCase = true)) browserUa else rawUa
            val referer = obj.optString("referer", "").ifBlank { "https://yacinelive.com/" }
            val playbackHeaderNames = setOf("user-agent", "referer", "origin", "accept", "accept-language")
            val requestHeaders = buildMap {
                headersObj?.keys()?.forEach { key ->
                    val value = headersObj.optString(key).trim()
                    if (key.lowercase() in playbackHeaderNames && value.isNotBlank() && !key.contains("\r") && !key.contains("\n")
                        && !value.contains("\r") && !value.contains("\n")
                    ) put(key, value)
                }
                put("User-Agent", ua)
                put("Referer", referer)
            }
            YacineStream(
                name = obj.optString("name", "HD"),
                url = obj.optString("url", ""),
                userAgent = ua,
                referer = referer,
                urlType = obj.optInt("url_type", 3),
                headers = requestHeaders,
            )
        }.filter { it.url.isNotEmpty() && !it.url.contains("rex.new-redirect.online") }
    }
}
