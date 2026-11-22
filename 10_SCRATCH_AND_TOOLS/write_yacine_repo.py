import json

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\kt_channels.txt', 'r', encoding='utf-8') as f:
    kt_channels = f.read()

with open(r'c:\Users\hassa\Desktop\all project\iptv app\10_SCRATCH_AND_TOOLS\kt_events.txt', 'r', encoding='utf-8') as f:
    kt_events = f.read()

kotlin_code = f'''package com.madar.tv.data

import android.util.Base64
import com.madar.tv.core.X
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YacineRepository {{

    // ═══════════════════════════════════════════════════════════════════════
    // Legacy encoded constants; values are recoverable from the client APK.
    // ═══════════════════════════════════════════════════════════════════════

    private val _bu = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 77, 196.toByte(), 84, 120, 67, 202.toByte(), 68, 240.toByte(), 244.toByte(), 143.toByte(), 1, 144.toByte(), 23, 139.toByte(), 46, 37, 57, 23, 73, 200.toByte(), 95)
    private val _sk = byteArrayOf(132.toByte(), 190.toByte(), 29, 87, 36, 157.toByte(), 76, 152.toByte(), 98, 187.toByte(), 41, 162.toByte(), 110, 18, 96, 249.toByte())
    private val _pn = byteArrayOf(2, 156.toByte(), 26, 131.toByte(), 2, 182.toByte(), 190.toByte(), 53, 69, 194.toByte(), 92, 212.toByte(), 74, 227.toByte(), 104, 113)
    private val _ua = byteArrayOf(134.toByte(), 153.toByte(), 21, 34, 41, 50, 156.toByte(), 30, 110, 238.toByte(), 241.toByte(), 113, 67)

    // ── Lazily resolved secrets ───────────────────────────────────────────
    private val BASE_URL: String by lazy {{ X.s(_bu) }}
    private val HARDCODED_KEY: String by lazy {{ X.s(_sk) }}
    private val PKG_NAME: String by lazy {{ X.s(_pn) }}
    private val UA: String by lazy {{ X.s(_ua) }}

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
    private const val CATEGORIES_CACHE_TTL = 10 * 60 * 1000L

    private val cachedChannels = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<YacineChannel>>>()
    private const val CHANNELS_CACHE_TTL = 10 * 60 * 1000L

    private var cachedEvents: List<YacineEvent> = emptyList()
    private var lastEventsFetch = 0L
    private const val EVENTS_CACHE_TTL = 45 * 1000L
    private const val EVENTS_DISK_STALE_MS = 6L * 60 * 60 * 1000

    // ─── Exclusive beIN SPORTS Channels extracted from Yacine TV ──────────
    val BEIN_FALLBACK_CHANNELS = listOf(
{kt_channels}
    )

    // ─── Real Match Schedule extracted from Yacine TV ────────────────────
    val REAL_YACINE_EVENTS = listOf(
{kt_events}
    )

    val DEFAULT_CATEGORIES = listOf(
        YacineCategory(id = "bein_sports", name = "beIN SPORTS", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 20)
    )

    fun getCachedCategories(): List<YacineCategory> {{
        if (cachedCategories.isNotEmpty()) return cachedCategories
        return DEFAULT_CATEGORIES
    }}

    fun getCachedChannels(categoryId: String): List<YacineChannel> {{
        cachedChannels[categoryId]?.let {{ return it.second }}
        cachedChannels[categoryId] = Pair(System.currentTimeMillis(), BEIN_FALLBACK_CHANNELS)
        return BEIN_FALLBACK_CHANNELS
    }}

    fun getCachedEvents(): List<YacineEvent> {{
        if (cachedEvents.isNotEmpty()) return cachedEvents
        val rows = CatalogDiskCache.loadJson("yacine:events", EVENTS_DISK_STALE_MS)?.value?.optJSONArray("items")
        if (rows != null && rows.length() > 0) {{
            val cached = buildList {{
                for (i in 0 until rows.length()) {{
                    val row = rows.optJSONObject(i) ?: continue
                    val team1 = row.optJSONObject("team1") ?: JSONObject()
                    val team2 = row.optJSONObject("team2") ?: JSONObject()
                    add(YacineEvent(
                        row.optString("id"), row.optString("champions"), row.optString("channel"),
                        row.optLong("startTime"), row.optLong("endTime"), row.optString("commentary"),
                        YacineTeam(team1.optString("id"), team1.optString("name"), team1.optString("logo")),
                        YacineTeam(team2.optString("id"), team2.optString("name"), team2.optString("logo")),
                    ))
                }}
            }}
            if (cached.isNotEmpty()) {{
                cachedEvents = cached
                return cached
            }}
        }}
        cachedEvents = REAL_YACINE_EVENTS
        return REAL_YACINE_EVENTS
    }}

    // ─── Decryption ─────────────────────────────────────────────────────────────
    private fun decrypt(body: String, tHeader: String): String {{
        val key = (HARDCODED_KEY + tHeader).toByteArray(Charsets.UTF_8)
        val decoded = Base64.decode(body.trim(), Base64.DEFAULT)
        val result = ByteArray(decoded.size) {{ i ->
            (decoded[i].toInt() xor key[i % key.size].toInt()).toByte()
        }}
        return String(result, Charsets.UTF_8)
    }}

    private fun get(endpoint: String): Pair<String, String> {{
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
    }}

    private fun decryptResponse(endpoint: String): String {{
        val (body, t) = get(endpoint)
        return decrypt(body, t)
    }}

    // ─── Categories ──────────────────────────────────────────────────────────────
    suspend fun getCategories(forceRefresh: Boolean = false): List<YacineCategory> = withContext(Dispatchers.IO) {{
        DEFAULT_CATEGORIES
    }}

    // ─── Channels ────────────────────────────────────────────────────────────────
    suspend fun getChannels(categoryId: String, forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {{
        val now = System.currentTimeMillis()
        if (!forceRefresh) {{
            cachedChannels[categoryId]?.let {{ (time, channels) ->
                if (now - time < CHANNELS_CACHE_TTL && channels.isNotEmpty()) {{
                    return@withContext channels
                }}
            }}
        }}
        cachedChannels[categoryId] = Pair(now, BEIN_FALLBACK_CHANNELS)
        BEIN_FALLBACK_CHANNELS
    }}

    // ─── Events ──────────────────────────────────────────────────────────────────
    suspend fun getEvents(forceRefresh: Boolean = false): List<YacineEvent> = withContext(Dispatchers.IO) {{
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedEvents.isNotEmpty() && (now - lastEventsFetch < EVENTS_CACHE_TTL)) {{
            return@withContext cachedEvents
        }}
        val fetchedList = runCatching {{
            val json = JSONObject(decryptResponse("events"))
            val arr = json.getJSONArray("data")
            (0 until arr.length()).map {{ i ->
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
            }}.filter {{ it.id.isNotEmpty() }}
        }}.getOrNull()

        val activeList = if (!fetchedList.isNullOrEmpty()) fetchedList else REAL_YACINE_EVENTS
        cachedEvents = activeList
        lastEventsFetch = now

        runCatching {{
            val rows = JSONArray().apply {{
                activeList.forEach {{ event ->
                    fun team(team: YacineTeam) = JSONObject().put("id", team.id).put("name", team.name).put("logo", team.logo)
                    put(JSONObject()
                        .put("id", event.id).put("champions", event.champions).put("channel", event.channel)
                        .put("startTime", event.startTime).put("endTime", event.endTime).put("commentary", event.commentary)
                        .put("team1", team(event.team1)).put("team2", team(event.team2)))
                }}
            }}
            CatalogDiskCache.saveJson("yacine:events", JSONObject().put("items", rows))
        }}

        activeList
    }}

    suspend fun getSportsChannels(forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {{
        BEIN_FALLBACK_CHANNELS
    }}

    // ─── Event Streams ────────────────────────────────────────────────────────────
    suspend fun getEventStreams(eventId: String): List<YacineStream> = withContext(Dispatchers.IO) {{
        runCatching {{
            parseStreamPayload(decryptResponse("event/$eventId"))
        }}.getOrElse {{ emptyList() }}
    }}

    fun getStreamsForChannelName(channelName: String): List<YacineStream> {{
        val name = channelName.trim().ifBlank {{ "beIN SPORTS HD" }}
        return listOf(
            YacineStream(
                name = "$name · سيرفر رئيسي 1 فائق السرعة CDN",
                url = "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                referer = "https://yacinelive.com/",
                urlType = 3,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            ),
            YacineStream(
                name = "$name · سيرفر مباشر 2 CDN",
                url = "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                referer = "https://yacinelive.com/",
                urlType = 3,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            ),
            YacineStream(
                name = "$name · سيرفر بديل 3 CDN",
                url = "https://bein-xtra-bein.amagi.tv/playlist.m3u8",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                referer = "https://yacinelive.com/",
                urlType = 3,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            ),
            YacineStream(
                name = "$name · سيرفر احتياطي 4",
                url = "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                referer = "https://yacinelive.com/",
                urlType = 3,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            )
        )
    }}

    suspend fun getMatchStreams(
        eventId: String,
        channelHint: String,
        leagueHint: String,
    ): List<YacineStream> = withContext(Dispatchers.IO) {{
        val hint = channelHint.trim()
        val targetEvent = REAL_YACINE_EVENTS.firstOrNull {{ it.id == eventId }}
        val channelToUse = when {{
            hint.isNotBlank() -> hint
            targetEvent != null && targetEvent.channel.isNotBlank() -> targetEvent.channel
            else -> "beIN SPORTS 1"
        }}

        // Match with specific beIN channel
        val matchedChannel = BEIN_FALLBACK_CHANNELS.firstOrNull {{ ch ->
            val n1 = normalizeChannelName(ch.name)
            val n2 = normalizeChannelName(channelToUse)
            n1.contains(n2) || n2.contains(n1)
        }}

        val finalChannelName = matchedChannel?.name ?: channelToUse
        getStreamsForChannelName(finalChannelName)
    }}

    suspend fun getChannelStreams(channelId: String): List<YacineStream> = withContext(Dispatchers.IO) {{
        val id = channelId.lowercase().trim()
        val matchedChannel = BEIN_FALLBACK_CHANNELS.firstOrNull {{
            it.id.equals(id, ignoreCase = true) || it.name.contains(id, ignoreCase = true)
        }}
        val channelName = matchedChannel?.name ?: "beIN SPORTS HD"
        getStreamsForChannelName(channelName)
    }}

    private fun parseStreamPayload(raw: String): List<YacineStream> {{
        val clean = raw.trim()
        val rows = when {{
            clean.startsWith("[") -> JSONArray(clean)
            clean.startsWith("{{") -> {{
                val json = JSONObject(clean)
                json.optJSONArray("data") ?: json.optJSONArray("streams") ?: JSONArray()
            }}
            else -> JSONArray()
        }}
        return parseStreams(rows)
    }}

    private fun normalizeChannelName(value: String): String = value
        .lowercase()
        .replace(Regex("[\\\\u064B-\\\\u065F\\\\u0670\\\\u0640]"), "")
        .replace(Regex("[\\\\u0622\\\\u0623\\\\u0625]"), "ا")
        .replace(Regex("[^a-z0-9\\\\u0600-\\\\u06ff]"), "")

    private fun parseStreams(arr: JSONArray): List<YacineStream> {{
        val browserUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        return (0 until arr.length()).map {{ i ->
            val obj = arr.getJSONObject(i)
            val headersObj = obj.optJSONObject("headers")
            val rawUa = headersObj?.optString("User-Agent", "").orEmpty()
                .ifBlank {{ obj.optString("user_agent", "") }}
                .ifBlank {{ browserUa }}
            val ua = if (rawUa.startsWith("okhttp", ignoreCase = true)) browserUa else rawUa
            val referer = obj.optString("referer", "").ifBlank {{ "https://yacinelive.com/" }}
            val playbackHeaderNames = setOf("user-agent", "referer", "origin", "accept", "accept-language")
            val requestHeaders = buildMap {{
                headersObj?.keys()?.forEach {{ key ->
                    val value = headersObj.optString(key).trim()
                    if (key.lowercase() in playbackHeaderNames && value.isNotBlank() && !key.contains("\\r") && !key.contains("\\n")
                        && !value.contains("\\r") && !value.contains("\\n")
                    ) put(key, value)
                }}
                put("User-Agent", ua)
                put("Referer", referer)
            }}
            YacineStream(
                name = obj.optString("name", "HD"),
                url = obj.optString("url", ""),
                userAgent = ua,
                referer = referer,
                urlType = obj.optInt("url_type", 3),
                headers = requestHeaders,
            )
        }}.filter {{ it.url.isNotEmpty() && !it.url.contains("rex.new-redirect.online") }}
    }}
}}
'''

target_path = r'c:\Users\hassa\Desktop\all project\iptv app\01_ZAQZOUKA_CLIENT\MadarFlutter\android\app\src\main\kotlin\com\madar\tv\data\YacineRepository.kt'
with open(target_path, 'w', encoding='utf-8') as f:
    f.write(kotlin_code)

print("Successfully written YacineRepository.kt! File size:", len(kotlin_code))
