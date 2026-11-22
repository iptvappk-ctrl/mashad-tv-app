package com.madar.tv.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YacineRepository {

    // ── Live Yacine API Hosts & Secrets ──────────────────────────────────
    private val LIVE_HOSTS = listOf(
        "def777.ycnapi.com",
        "tv.variety-buy.store",
        "deft.yacinelive.com"
    )
    private const val HARDCODED_KEY = "c!xZj+N9&G@Ev@vw"
    private const val UA = "okhttp/4.12.0"

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
        YacineChannel(id = "1424", name = "beIN SPORTS 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_DIGITAL_Mono.png", isHide = 0, priority = 25),
        YacineChannel(id = "1425", name = "beIN SPORTS 2", logo = "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS2_DIGITAL_Mono.png", isHide = 0, priority = 24),
        YacineChannel(id = "1426", name = "beIN SPORTS 3", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_DIGITAL_Mono.png", isHide = 0, priority = 23),
        YacineChannel(id = "1427", name = "beIN SPORTS 4", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS4_DIGITAL_Mono.png", isHide = 0, priority = 22),
        YacineChannel(id = "1428", name = "beIN SPORTS 5", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS5_DIGITAL_Mono.png", isHide = 0, priority = 21),
        YacineChannel(id = "1429", name = "beIN SPORTS 6", logo = "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS6_DIGITAL_Mono.png", isHide = 0, priority = 20),
        YacineChannel(id = "1430", name = "beIN SPORTS 7", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS7_DIGITAL_Mono.png", isHide = 0, priority = 19),
        YacineChannel(id = "1431", name = "beIN SPORTS 8", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS8.png", isHide = 0, priority = 18),
        YacineChannel(id = "1432", name = "beIN SPORTS 9", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS9.png", isHide = 0, priority = 17),
        YacineChannel(id = "1421", name = "beIN XTRA 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA1_Digital_Mono.png", isHide = 0, priority = 16),
        YacineChannel(id = "1422", name = "beIN XTRA 2", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA2_Digital_Mono.png", isHide = 0, priority = 15),
        YacineChannel(id = "1423", name = "beIN XTRA 3", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTSXTRA3.png", isHide = 0, priority = 14),
        YacineChannel(id = "1433", name = "beIN France 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_FRENCH_Digital_Mono.png", isHide = 0, priority = 13),
        YacineChannel(id = "1434", name = "beIN France 2", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS2_FRENCH_Digital_Mono.png", isHide = 0, priority = 12),
        YacineChannel(id = "1435", name = "beIN France 3", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_FRENCH_Digital_Mono.png", isHide = 0, priority = 11)
    )

    // ─── Real Match Schedule extracted from Yacine TV ────────────────────
    val REAL_YACINE_EVENTS = listOf(
        YacineEvent(
            id = "2863227690",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 1",
            startTime = 1795352400L,
            endTime = 1795359600L,
            commentary = "خليل البلوشي",
            team1 = YacineTeam(id = "3", name = "برشلونة", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/paYnEE8hcrP96neHRNofhQ_96x96.png"),
            team2 = YacineTeam(id = "11", name = "فياريال", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/WKH7Ak5cYD6Qm1EEqXzmVw_96x96.png")
        ),
        YacineEvent(
            id = "2863227689",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 1",
            startTime = 1795365000L,
            endTime = 1795372200L,
            commentary = "عصام الشوالي",
            team1 = YacineTeam(id = "42", name = "ليفربول", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/0iShHhASp5q1SL4JhtwJiw_96x96.png"),
            team2 = YacineTeam(id = "41", name = "مان يونايتد", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/udQ6ns69PctCv143h-GeYw_96x96.png")
        ),
        YacineEvent(
            id = "2863227691",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 2",
            startTime = 1795360500L,
            endTime = 1795367700L,
            commentary = "محمد مبروكي",
            team1 = YacineTeam(id = "929", name = "راسينغ سانتاندير", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/2qlWr1sH_7xAfbRZDRoK3A_96x96.png"),
            team2 = YacineTeam(id = "5", name = "ريال سوسيداد", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/w8tb1aeBfVZIj9tZXf7eZg_96x96.png")
        ),
        YacineEvent(
            id = "2863227692",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 2",
            startTime = 1795368600L,
            endTime = 1795375800L,
            commentary = "منتصر الأزهري",
            team1 = YacineTeam(id = "9", name = "اوساسونا", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/pk-hNCNpGM_JDoHHvLVF-Q_96x96.png"),
            team2 = YacineTeam(id = "685", name = "مالقا", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/cPQnNrl3qcM5x70V14TuYA_96x96.png")
        ),
        YacineEvent(
            id = "2863227697",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 2",
            startTime = 1795376700L,
            endTime = 1795383900L,
            commentary = "محمد بركات",
            team1 = YacineTeam(id = "84", name = "مارسيليا", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/WnFwNAD9wIdfewRHSq79WQ_96x96.png"),
            team2 = YacineTeam(id = "824", name = "لومان", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/d_680kHAdTDWexQw3BemxQ_96x96.png")
        ),
        YacineEvent(
            id = "2863227693",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 1",
            startTime = 1795377600L,
            endTime = 1795384800L,
            commentary = "أحمد عبده",
            team1 = YacineTeam(id = "21", name = "خيتافي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/1UDHZMdKZD15W5gus7dJyg_96x96.png"),
            team2 = YacineTeam(id = "7", name = "اتلتيكو مدريد", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/srAAE0bOnCppUrlbJpFiHQ_96x96.png")
        ),
        YacineEvent(
            id = "2863227694",
            champions = "تشامبيونشيب البطولة الإنجليزية",
            channel = "beIN SPORTS 6",
            startTime = 1795348800L,
            endTime = 1795356000L,
            commentary = "محمد السعدي",
            team1 = YacineTeam(id = "78", name = "بيرنلي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/XC5UrPpuN5yzkgCiiz9yWg_96x96.png"),
            team2 = YacineTeam(id = "79", name = "بلاكبيرن روفرز", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/9SHzswVb2HlYJfeUEkQNiQ_96x96.png")
        ),
        YacineEvent(
            id = "2863227695",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 4",
            startTime = 1795356000L,
            endTime = 1795363200L,
            commentary = "محمد السعدي",
            team1 = YacineTeam(id = "245", name = "باريس أف سي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/6L0HPrdS4FdFbafBX80QuQ_96x96.png"),
            team2 = YacineTeam(id = "100", name = "انجيه", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Be-wAQaZCDYKCGls1191zg_96x96.png")
        ),
        YacineEvent(
            id = "2863227696",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 4",
            startTime = 1795364100L,
            endTime = 1795371300L,
            commentary = "جواد بدة",
            team1 = YacineTeam(id = "85", name = "لوريان", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/bbYkAWWtD6lpK5KyGfr1vA_96x96.png"),
            team2 = YacineTeam(id = "83", name = "رين", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/guI8eg4hoTyIp6rO1opjxA_96x96.png")
        ),
        YacineEvent(
            id = "2863227688",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 5",
            startTime = 1795356000L,
            endTime = 1795363200L,
            commentary = "حسن العيدروس",
            team1 = YacineTeam(id = "699", name = "هال سيتي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/riiyZbb1JHuFQgZ3831jUQ_96x96.png"),
            team2 = YacineTeam(id = "43", name = "برايتون", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/EKIe0e-ZIphOcfQAwsuEEQ_96x96.png")
        )
    )

    val DEFAULT_CATEGORIES = listOf(
        YacineCategory(id = "4", name = "beIN SPORTS (1080P)", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 15),
        YacineCategory(id = "5", name = "beIN SPORTS (720P)", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 15),
        YacineCategory(id = "6", name = "beIN SPORTS (360P)", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 15),
        YacineCategory(id = "9", name = "ARABIC CHANNELS", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 20)
    )

    fun getCachedCategories(): List<YacineCategory> {
        if (cachedCategories.isNotEmpty()) return cachedCategories
        return DEFAULT_CATEGORIES
    }

    fun getCachedChannels(categoryId: String): List<YacineChannel> {
        cachedChannels[categoryId]?.let { return it.second }
        cachedChannels[categoryId] = Pair(System.currentTimeMillis(), BEIN_FALLBACK_CHANNELS)
        return BEIN_FALLBACK_CHANNELS
    }

    fun getCachedEvents(): List<YacineEvent> {
        if (cachedEvents.isNotEmpty()) return cachedEvents
        val rows = CatalogDiskCache.loadJson("yacine:events", EVENTS_DISK_STALE_MS)?.value?.optJSONArray("items")
        if (rows != null && rows.length() > 0) {
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
            if (cached.isNotEmpty()) {
                cachedEvents = cached
                return cached
            }
        }
        cachedEvents = REAL_YACINE_EVENTS
        return REAL_YACINE_EVENTS
    }

    // ─── Decryption ─────────────────────────────────────────────────────────────
    private fun decrypt(body: String, tHeader: String): String {
        if (body.isBlank()) return ""
        return try {
            val key = (HARDCODED_KEY + tHeader).toByteArray(Charsets.UTF_8)
            val decoded = Base64.decode(body.trim(), Base64.DEFAULT)
            val result = ByteArray(decoded.size) { i ->
                (decoded[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            String(result, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun get(endpoint: String): Pair<String, String> {
        for (host in LIVE_HOSTS) {
            try {
                val request = Request.Builder()
                    .url("https://$host/api/$endpoint")
                    .header("User-Agent", UA)
                    .header("Accept", "application/json")
                    .build()
                val response = client.newCall(request).execute()
                val tHeader = response.header("T") ?: response.header("t") ?: ""
                val body = response.body?.string() ?: ""
                response.close()
                if (body.isNotEmpty() && response.isSuccessful) {
                    return Pair(body, tHeader)
                }
            } catch (_: Exception) {
                continue
            }
        }
        return Pair("", "")
    }

    private fun decryptResponse(endpoint: String): String {
        val (body, t) = get(endpoint)
        if (body.isEmpty()) return ""
        return decrypt(body, t)
    }

    // ─── Categories ──────────────────────────────────────────────────────────────
    suspend fun getCategories(forceRefresh: Boolean = false): List<YacineCategory> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedCategories.isNotEmpty() && (now - lastCategoriesFetch < CATEGORIES_CACHE_TTL)) {
            return@withContext cachedCategories
        }
        val fetched = runCatching {
            val decrypted = decryptResponse("categories")
            if (decrypted.isNotBlank()) {
                val json = JSONObject(decrypted)
                val arr = json.getJSONArray("data")
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    YacineCategory(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        logo = obj.optString("logo", ""),
                        childCount = obj.optInt("child_count", 0)
                    )
                }.filter { it.id.isNotEmpty() }
            } else emptyList()
        }.getOrNull()

        if (!fetched.isNullOrEmpty()) {
            cachedCategories = fetched
            lastCategoriesFetch = now
            return@withContext fetched
        }
        if (cachedCategories.isNotEmpty()) return@withContext cachedCategories
        DEFAULT_CATEGORIES
    }

    // ─── Channels ────────────────────────────────────────────────────────────────
    suspend fun getChannels(categoryId: String, forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            cachedChannels[categoryId]?.let { (time, channels) ->
                if (now - time < CHANNELS_CACHE_TTL && channels.isNotEmpty()) {
                    return@withContext channels
                }
            }
        }
        val fetched = runCatching {
            val decrypted = decryptResponse("categories/$categoryId/channels")
            if (decrypted.isNotBlank()) {
                val json = JSONObject(decrypted)
                val arr = json.getJSONArray("data")
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    YacineChannel(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        logo = obj.optString("logo", ""),
                        isHide = obj.optInt("is_hide", 0),
                        priority = obj.optInt("priority", 0)
                    )
                }.filter { it.id.isNotEmpty() }
            } else emptyList()
        }.getOrNull()

        if (!fetched.isNullOrEmpty()) {
            cachedChannels[categoryId] = Pair(now, fetched)
            return@withContext fetched
        }
        cachedChannels[categoryId] = Pair(now, BEIN_FALLBACK_CHANNELS)
        BEIN_FALLBACK_CHANNELS
    }

    // ─── Events ──────────────────────────────────────────────────────────────────
    suspend fun getEvents(forceRefresh: Boolean = false): List<YacineEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedEvents.isNotEmpty() && (now - lastEventsFetch < EVENTS_CACHE_TTL)) {
            return@withContext cachedEvents
        }
        val fetchedList = runCatching {
            val decrypted = decryptResponse("events")
            if (decrypted.isNotBlank()) {
                val json = JSONObject(decrypted)
                val arr = json.getJSONArray("data")
                (0 until arr.length()).map { i ->
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
            } else emptyList()
        }.getOrNull()

        val activeList = if (!fetchedList.isNullOrEmpty()) fetchedList else REAL_YACINE_EVENTS
        cachedEvents = activeList
        lastEventsFetch = now

        runCatching {
            val rows = JSONArray().apply {
                activeList.forEach { event ->
                    fun team(team: YacineTeam) = JSONObject().put("id", team.id).put("name", team.name).put("logo", team.logo)
                    put(JSONObject()
                        .put("id", event.id).put("champions", event.champions).put("channel", event.channel)
                        .put("startTime", event.startTime).put("endTime", event.endTime).put("commentary", event.commentary)
                        .put("team1", team(event.team1)).put("team2", team(event.team2)))
                }
            }
            CatalogDiskCache.saveJson("yacine:events", JSONObject().put("items", rows))
        }

        activeList
    }

    suspend fun getSportsChannels(forceRefresh: Boolean = false): List<YacineChannel> = withContext(Dispatchers.IO) {
        val catChannels = getChannels("4", forceRefresh)
        if (catChannels.isNotEmpty()) catChannels else BEIN_FALLBACK_CHANNELS
    }

    // ─── Event Streams ────────────────────────────────────────────────────────────
    suspend fun getEventStreams(eventId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        val id = eventId.trim()
        if (id.isEmpty()) return@withContext emptyList()
        runCatching {
            val decrypted = decryptResponse("event/$id")
            if (decrypted.isNotBlank()) parseStreamPayload(decrypted) else emptyList()
        }.getOrElse { emptyList() }
    }

    private fun extractChannelNumber(channelName: String): Int {
        val clean = channelName.replace(Regex("(?i)xtra|france|french|english|news"), "")
        val m = Regex("(?i)(?:sports?|سبورت|sport|bein|hd|قناة)?\\s*(\\d+)\\b").find(clean)
            ?: Regex("\\b(\\d+)\\b").find(clean)
        return m?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    fun getStreamsForChannelName(channelName: String): List<YacineStream> {
        val name = channelName.trim().ifBlank { "beIN SPORTS 1" }
        val n = name.lowercase()
        val ch = extractChannelNumber(name)
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val ref = "https://yacinelive.com/"

        val streams: List<Triple<String, String, String>> = when {
            ch == 1 || (n.contains("bein") && n.contains("1") && !n.contains("xtra")) -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein1-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            ch == 2 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein2-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            ch == 3 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein3-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            ch == 4 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein4-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            ch == 5 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein5-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            ch == 6 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein6-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            ch == 7 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein7-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            ch == 8 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein8-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            ch == 9 -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://bein9-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            n.contains("xtra") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            n.contains("france") || n.contains("french") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-beinsports1french-samsungau.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            n.contains("english") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-beinsports1english-samsungau.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            else -> listOf(
                Triple("$name · سيرفر مباشر 1", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein1-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
        }

        return streams.map { (streamName, url, referer) ->
            YacineStream(
                name = streamName,
                url = url,
                userAgent = ua,
                referer = referer,
                urlType = 3,
                headers = mapOf(
                    "User-Agent" to ua,
                    "Referer" to referer
                )
            )
        }
    }

    suspend fun getMatchStreams(
        eventId: String,
        channelHint: String,
        leagueHint: String,
    ): List<YacineStream> = withContext(Dispatchers.IO) {
        val id = eventId.trim()
        val hint = channelHint.trim()

        // 1. Try live event streams first if eventId is a valid numeric ID
        if (id.isNotEmpty() && id.all { it.isDigit() }) {
            val eventStreams = getEventStreams(id)
            if (eventStreams.isNotEmpty()) {
                return@withContext eventStreams
            }
        }

        // 2. Resolve channel name
        val channelToUse = when {
            hint.isNotBlank() -> hint
            else -> {
                val ev = cachedEvents.firstOrNull { it.id == id } ?: REAL_YACINE_EVENTS.firstOrNull { it.id == id }
                ev?.channel?.ifBlank { "beIN SPORTS 1" } ?: "beIN SPORTS 1"
            }
        }

        // 3. Try live channel streams
        val chStreams = getChannelStreams(channelToUse)
        if (chStreams.isNotEmpty()) {
            return@withContext chStreams
        }

        // 4. Last fallback: offline static streams
        getStreamsForChannelName(channelToUse)
    }

    suspend fun getChannelStreams(channelId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        val clean = channelId.trim()
        val n = clean.lowercase()
        val chNum = extractChannelNumber(clean)

        val targetId = when {
            clean.all { it.isDigit() } && clean.length in 3..6 -> clean
            n.contains("xtra 1") || n.contains("xtra1") -> "1421"
            n.contains("xtra 2") || n.contains("xtra2") -> "1422"
            n.contains("xtra 3") || n.contains("xtra3") || n.contains("xtra") -> "1423"
            n.contains("france 1") -> "1433"
            n.contains("france 2") -> "1434"
            n.contains("france 3") -> "1435"
            chNum in 1..9 -> (1423 + chNum).toString()
            else -> {
                val matched = BEIN_FALLBACK_CHANNELS.firstOrNull { ch ->
                    val n1 = normalizeChannelName(ch.name)
                    val n2 = normalizeChannelName(clean)
                    n1 == n2 || n1.contains(n2) || n2.contains(n1)
                }
                matched?.id ?: "1424"
            }
        }

        // Try live Yacine API first
        val liveStreams = runCatching {
            val decrypted = decryptResponse("channel/$targetId")
            if (decrypted.isNotBlank()) parseStreamPayload(decrypted) else emptyList()
        }.getOrDefault(emptyList())

        if (liveStreams.isNotEmpty()) {
            return@withContext liveStreams
        }

        // Fallback to static CDN streams for the channel
        val matchedChannel = BEIN_FALLBACK_CHANNELS.firstOrNull { it.id == targetId }
        val chName = matchedChannel?.name ?: clean
        getStreamsForChannelName(chName)
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
        val browserUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val headersObj = obj.optJSONObject("headers")
            val rawUa = headersObj?.optString("User-Agent", "").orEmpty()
                .ifBlank { obj.optString("user_agent", "") }
                .ifBlank { browserUa }
            val ua = if (rawUa.startsWith("okhttp", ignoreCase = true)) browserUa else rawUa
            val rawRef = obj.optString("referer", "").ifBlank { "https://yacinelive.com/" }
            // Yacine embeds zero-width UTF-8 security tokens in referer headers.
            // Converting UTF-8 bytes to ISO-8859-1 preserves the exact byte sequence over HttpURLConnection.
            val referer = try {
                String(rawRef.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
            } catch (_: Exception) {
                rawRef
            }.ifBlank { "https://yacinelive.com/" }
            val playbackHeaderNames = setOf("user-agent", "referer", "origin", "accept", "accept-language")
            val requestHeaders = buildMap {
                headersObj?.keys()?.forEach { key ->
                    val rawVal = headersObj.optString(key).trim()
                    val safeVal = try {
                        String(rawVal.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
                    } catch (_: Exception) {
                        rawVal
                    }
                    if (key.lowercase() in playbackHeaderNames && safeVal.isNotBlank() && !key.contains("\r") && !key.contains("\n")
                        && !safeVal.contains("\r") && !safeVal.contains("\n")
                    ) put(key, safeVal)
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
