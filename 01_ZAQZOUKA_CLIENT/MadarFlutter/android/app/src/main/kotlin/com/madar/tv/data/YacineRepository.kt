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
    // ═══════════════════════════════════════════════════════════════════════

    private val _bu = byteArrayOf(3, 20, 27, 32, 168.toByte(), 21, 156.toByte(), 149.toByte(), 77, 196.toByte(), 84, 120, 67, 202.toByte(), 68, 240.toByte(), 244.toByte(), 143.toByte(), 1, 144.toByte(), 23, 139.toByte(), 46, 37, 57, 23, 73, 200.toByte(), 95)
    private val _sk = byteArrayOf(132.toByte(), 190.toByte(), 29, 87, 36, 157.toByte(), 76, 152.toByte(), 98, 187.toByte(), 41, 162.toByte(), 110, 18, 96, 249.toByte())
    private val _pn = byteArrayOf(2, 156.toByte(), 26, 131.toByte(), 2, 182.toByte(), 190.toByte(), 53, 69, 194.toByte(), 92, 212.toByte(), 74, 227.toByte(), 104, 113)
    private val _ua = byteArrayOf(134.toByte(), 153.toByte(), 21, 34, 41, 50, 156.toByte(), 30, 110, 238.toByte(), 241.toByte(), 113, 67)

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
    private const val CATEGORIES_CACHE_TTL = 10 * 60 * 1000L

    private val cachedChannels = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<YacineChannel>>>()
    private const val CHANNELS_CACHE_TTL = 10 * 60 * 1000L

    private var cachedEvents: List<YacineEvent> = emptyList()
    private var lastEventsFetch = 0L
    private const val EVENTS_CACHE_TTL = 45 * 1000L
    private const val EVENTS_DISK_STALE_MS = 6L * 60 * 60 * 1000

    // ─── Exclusive beIN SPORTS Channels extracted from Yacine TV ──────────
    val BEIN_FALLBACK_CHANNELS = listOf(
        YacineChannel(id = "1143098968567368033", name = "beIN SPORTS 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_DIGITAL_Mono.png", isHide = 0, priority = 25),
        YacineChannel(id = "1143098968566343572", name = "beIN SPORTS 2", logo = "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS2_DIGITAL_Mono.png", isHide = 0, priority = 24),
        YacineChannel(id = "1143098968569416219", name = "beIN SPORTS 3", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_DIGITAL_Mono.png", isHide = 0, priority = 23),
        YacineChannel(id = "1143098968568628598", name = "beIN SPORTS 4", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS4_DIGITAL_Mono.png", isHide = 0, priority = 22),
        YacineChannel(id = "1143098968563317197", name = "beIN SPORTS 5", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS5_DIGITAL_Mono.png", isHide = 0, priority = 21),
        YacineChannel(id = "1143098968562474496", name = "beIN SPORTS 6", logo = "https://assets.bein.com/mena/sites/4/2021/02/beIN_SPORTS6_DIGITAL_Mono.png", isHide = 0, priority = 20),
        YacineChannel(id = "1143098968565447332", name = "beIN SPORTS 7", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS7_DIGITAL_Mono.png", isHide = 0, priority = 19),
        YacineChannel(id = "1143098968564573297", name = "beIN SPORTS 8", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS8.png", isHide = 0, priority = 18),
        YacineChannel(id = "1143098968559140230", name = "beIN SPORTS 9", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTS9.png", isHide = 0, priority = 17),
        YacineChannel(id = "1143098968570920632", name = "beIN XTRA 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA1_Digital_Mono.png", isHide = 0, priority = 16),
        YacineChannel(id = "1143098968573688335", name = "beIN XTRA 2", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_XTRA2_Digital_Mono.png", isHide = 0, priority = 15),
        YacineChannel(id = "1143098968572456170", name = "beIN XTRA 3", logo = "https://assets.bein.com/mena/sites/4/2023/09/logos-_beINSPORTSXTRA3.png", isHide = 0, priority = 14),
        YacineChannel(id = "1143098968558341739", name = "beIN France 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_FRENCH_Digital_Mono.png", isHide = 0, priority = 13),
        YacineChannel(id = "1143098968561167816", name = "beIN France 2", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS2_FRENCH_Digital_Mono.png", isHide = 0, priority = 12),
        YacineChannel(id = "1143098968560204757", name = "beIN France 3", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_FRENCH_Digital_Mono.png", isHide = 0, priority = 11),
        YacineChannel(id = "1143098968589111354", name = "beIN News", logo = "https://assets.bein.com/mena/sites/4/2015/06/NEWS_DIGITAL_Mono.png", isHide = 0, priority = 10),
        YacineChannel(id = "1143098968587560095", name = "beIN SPORTS", logo = "https://assets.bein.com/mena/sites/4/2015/06/bein_SPORTS_FTA_DIGITAL_Mono.png", isHide = 0, priority = 9),
        YacineChannel(id = "1143098968590638860", name = "beIN English 1", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS1_ENGLISH_Digital_Mono.png", isHide = 0, priority = 8),
        YacineChannel(id = "1143098968590149209", name = "beIN English 2", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS2_ENGLISH_Digital_Mono.png", isHide = 0, priority = 7),
        YacineChannel(id = "1143098968584995693", name = "beIN English 3", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS3_ENGLISH_Digital_Mono.png", isHide = 0, priority = 6),
    )

    // ─── Real Match Schedule extracted from Yacine TV ────────────────────
    val REAL_YACINE_EVENTS = listOf(
        YacineEvent(
            id = "1452951978498025827",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 1",
            startTime = 1795264200L,
            endTime = 1795271400L,
            commentary = "علي محمد علي",
            team1 = YacineTeam(id = "38", name = "مان سيتي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/z44l-a0W1v5FmgPnemV6Xw_96x96.png"),
            team2 = YacineTeam(id = "45", name = "فولهام", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Gh7_5p3n364p4vxeM8FhNg_96x96.png")
        ),
        YacineEvent(
            id = "1452951978486950270",
            champions = "تشامبيونشيب البطولة الإنجليزية",
            channel = "beIN SPORTS 3",
            startTime = 1795264200L,
            endTime = 1795271400L,
            commentary = "أحمد عبده",
            team1 = YacineTeam(id = "835", name = "ميلوول", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/FjQESJIwYuTRJ5B7HvtzPA_96x96.png"),
            team2 = YacineTeam(id = "681", name = "كوينز بارك رينجرز", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/SYBiHsr8jq3vT4p0HauolA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978505231935",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 9",
            startTime = 1795266000L,
            endTime = 1795273200L,
            commentary = "محمد بركات",
            team1 = YacineTeam(id = "188", name = "الافيس", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/RUcTOeFcBc7q7uku1nR_iQ_96x96.png"),
            team2 = YacineTeam(id = "945", name = "لاكورونيا", logo = "https://toppng.com/uploads/preview/rc-deportivo-la-coruna-vector-logo-11574288712ywghhf3zec.png")
        ),
        YacineEvent(
            id = "1452951978484787804",
            champions = "الدوري التركي",
            channel = "beIN SPORTS 2",
            startTime = 1795266000L,
            endTime = 1795273200L,
            commentary = "نوفل باشي",
            team1 = YacineTeam(id = "470", name = "غلطة سراي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/TWjoccvTU4JXZJZ3aW3cVg_96x96.png"),
            team2 = YacineTeam(id = "910", name = "سامسون سبور", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/lF7bCDr-Ai-fCbNZnfcxyA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978511059366",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 4",
            startTime = 1795273200L,
            endTime = 1795280400L,
            commentary = "جواد بدة",
            team1 = YacineTeam(id = "48", name = "استون فيلا", logo = "https://upload.wikimedia.org/wikipedia/fr/thumb/1/14/Aston_Villa_FC_2024.svg/langfr-250px-Aston_Villa_FC_2024.svg.png"),
            team2 = YacineTeam(id = "555", name = "سندرلاند", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/CQFeTfHrtxqgr3VKWtTwfA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978512458741",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 5",
            startTime = 1795273200L,
            endTime = 1795280400L,
            commentary = "محمد السعدي",
            team1 = YacineTeam(id = "50", name = "بورنموث", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/IcOt-hrK04B-RlRwI3R0yA_96x96.png"),
            team2 = YacineTeam(id = "54", name = "نوتينغهام فورست", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Zr6FbE-8pDH7UBpWCO8U9A_96x96.png")
        ),
        YacineEvent(
            id = "1452951978510194059",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 1",
            startTime = 1795273200L,
            endTime = 1795280400L,
            commentary = "علي الكعبي",
            team1 = YacineTeam(id = "44", name = "تشيلسي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/fhBITrIlbQxhVB6IjxUO6Q_96x96.png"),
            team2 = YacineTeam(id = "51", name = "ليدز يونايتد", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/5dqfOKpjjW6EwTAx_FysKQ_96x96.png")
        ),
        YacineEvent(
            id = "1452951978500011204",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 6",
            startTime = 1795273200L,
            endTime = 1795280400L,
            commentary = "محمد نضال",
            team1 = YacineTeam(id = "577", name = "كوفنتري سيتي", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/KHpmY4tIwqiutl8Cfl0MAw_96x96.png"),
            team2 = YacineTeam(id = "47", name = "كريستال بلاس", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/8piQOzndGmApKYTcvyN9vA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978499099665",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 3",
            startTime = 1795273200L,
            endTime = 1795280400L,
            commentary = "عبدالله الغامدي",
            team1 = YacineTeam(id = "40", name = "توتنهام", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/k3Q_mKE98Dnohrcea0JFgQ_96x96.png"),
            team2 = YacineTeam(id = "546", name = "إيبسويتش تاون", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/56vquJBk5U16Dng7txLXCw_96x96.png")
        ),
        YacineEvent(
            id = "1452951978507203162",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 9",
            startTime = 1795274100L,
            endTime = 1795281300L,
            commentary = "بكر علوان",
            team1 = YacineTeam(id = "10", name = "رايو فاليكانو", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/i5LifmxEVIl0sbvIysiyhw_96x96.png"),
            team2 = YacineTeam(id = "14", name = "فالنسيا", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/QPbjvDwI_0Wuu4tCS2O6uw_96x96.png")
        ),
        YacineEvent(
            id = "1452951978486227561",
            champions = "الدوري التركي",
            channel = "beIN SPORTS 7",
            startTime = 1795276800L,
            endTime = 1795284000L,
            commentary = "أحمد البلوشي",
            team1 = YacineTeam(id = "959", name = "كوجالي سبور", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/Z0OjkzD4X_FSNX4zUvEb4A_96x96.png"),
            team2 = YacineTeam(id = "322", name = "فنربخشه", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/HOQreZgFrmtQiLi8HKb9zA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978487367491",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 2",
            startTime = 1795277700L,
            endTime = 1795284900L,
            commentary = "حسن العيدروس",
            team1 = YacineTeam(id = "89", name = "نيس", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Llrxrqsc3Tw4JzE6xM7GWw_96x96.png"),
            team2 = YacineTeam(id = "81", name = "باريس", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/mcpMspef1hwHwi9qrfp4YQ_96x96.png")
        ),
        YacineEvent(
            id = "1452951978507731705",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 3",
            startTime = 1795282200L,
            endTime = 1795289400L,
            commentary = "سمير اليعقوبي",
            team1 = YacineTeam(id = "26", name = "اشبيلية", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/hCTs5EX3WjCMC5Jl3QE4Rw_96x96.png"),
            team2 = YacineTeam(id = "8", name = "ريال بيتيس", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/S0fDZjYYytbZaUt0f3cIhg_96x96.png")
        ),
        YacineEvent(
            id = "1452951978512563432",
            champions = "الدوري الإنجليزي الممتاز",
            channel = "beIN SPORTS 1",
            startTime = 1795282200L,
            endTime = 1795289400L,
            commentary = "حفيظ دراجي",
            team1 = YacineTeam(id = "39", name = "نيوكاسل", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/96CcNNQ0AYDAbssP0V9LuQ_96x96.png"),
            team2 = YacineTeam(id = "37", name = "ارسنال", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/4us2nCgl6kgZc0t3hpW75Q_96x96.png")
        ),
        YacineEvent(
            id = "1452951978480518048",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 5",
            startTime = 1795290300L,
            endTime = 1795297500L,
            commentary = "محمد بركات",
            team1 = YacineTeam(id = "99", name = "ستراسبورغ", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Eb9xtMpUy8FXQ0RCKvLxcg_96x96.png"),
            team2 = YacineTeam(id = "96", name = "بريست", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/HNqZwlu71GHXo60XjYrPxg_96x96.png")
        ),
        YacineEvent(
            id = "1452951978481735021",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 4",
            startTime = 1795290300L,
            endTime = 1795297500L,
            commentary = "جواد بدة",
            team1 = YacineTeam(id = "86", name = "موناكو", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/RX0XTi5Dtg4joMtuHNmYKg_96x96.png"),
            team2 = YacineTeam(id = "97", name = "اوكسير", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/jX0oM3wnm6cWOGKoUR3AyA_96x96.png")
        ),
        YacineEvent(
            id = "1452951978482769458",
            champions = "الدوري الفرنسي",
            channel = "beIN SPORTS 2",
            startTime = 1795290300L,
            endTime = 1795297500L,
            commentary = "عبدالله الغامدي",
            team1 = YacineTeam(id = "87", name = "ليل", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/D2AQe8qoyPIP4K8MzLvwuA_96x96.png"),
            team2 = YacineTeam(id = "88", name = "ليون", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/SrKK55dUkCxe4mJsyshfCg_96x96.png")
        ),
        YacineEvent(
            id = "1452951978509239980",
            champions = "الدوري الإسباني",
            channel = "beIN SPORTS 1",
            startTime = 1795291200L,
            endTime = 1795298400L,
            commentary = "أحمد البلوشي",
            team1 = YacineTeam(id = "4", name = "ريال مدريد", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/Th4fAVAZeCJWRcKoLW7koA_96x96.png"),
            team2 = YacineTeam(id = "24", name = "سيلتا فيغو", logo = "https://ssl.gstatic.com/onebox/media/sports/logos/optimized/wpdhixHP_sloegfP0UlwAw_96x96.png")
        ),
    )

    val DEFAULT_CATEGORIES = listOf(
        YacineCategory(id = "bein_sports", name = "beIN SPORTS", logo = "https://assets.bein.com/mena/sites/4/2015/06/beIN_SPORTS_1_Digital_Mono.png", childCount = 20)
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
        val key = (HARDCODED_KEY + tHeader).toByteArray(Charsets.UTF_8)
        val decoded = Base64.decode(body.trim(), Base64.DEFAULT)
        val result = ByteArray(decoded.size) { i ->
            (decoded[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }

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
            val json = JSONObject(decryptResponse("events"))
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
        BEIN_FALLBACK_CHANNELS
    }

    // ─── Event Streams ────────────────────────────────────────────────────────────
    suspend fun getEventStreams(eventId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        runCatching {
            parseStreamPayload(decryptResponse("event/$eventId"))
        }.getOrElse { emptyList() }
    }

    fun getStreamsForChannelName(channelName: String): List<YacineStream> {
        val name = channelName.trim().ifBlank { "beIN SPORTS HD" }
        val n = name.lowercase()
        val ch = extractChannelNumber(name)
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val ref = "https://yacinelive.com/"

        // ─── Per-channel dedicated CDN streams ─────────────────────────
        val streams: List<Triple<String, String, String>> = when {
            // beIN SPORTS 1 — Premier League / Champions League primary
            ch == 1 || (n.contains("bein") && n.contains("1") && !n.contains("xtra")) -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports1/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein1-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 2 — Turkish League / Ligue 1 secondary
            ch == 2 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports2/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein2-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 3 — Championship / La Liga
            ch == 3 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports3/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein3-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 4 — Premier League second / Ligue 1
            ch == 4 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports4/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein4-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 5 — Premier League / Bundesliga
            ch == 5 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports5/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein5-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 6
            ch == 6 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports6/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein6-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 7 — Turkish League primary
            ch == 7 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports7/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein7-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 8
            ch == 8 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports8/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein8-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // beIN SPORTS 9 — La Liga / Türk Liga secondary
            ch == 9 -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://tvsen6.aynaott.com/beinsports9/index.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://bein9-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
            )
            // beIN XTRA 1-3
            n.contains("xtra") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
            )
            // beIN France 1-3
            n.contains("france") || n.contains("french") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-beinsports1french-samsungau.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://dc1644a9jazgj.cloudfront.net/beIN_Sports_Xtra_Espanol.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // beIN English 1-3
            n.contains("english") -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://bein-beinsports1english-samsungau.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
            // Fallback
            else -> listOf(
                Triple("$name · سيرفر رئيسي CDN", "https://d9ssxzmclhfo4.cloudfront.net/bein_sports.m3u8", ref),
                Triple("$name · سيرفر 2 HD", "https://tvsen6.aynaott.com/beinsports/index.m3u8", ref),
                Triple("$name · سيرفر 3 بديل", "https://bein-xtra-bein.amagi.tv/playlist.m3u8", ref),
                Triple("$name · سيرفر 4 احتياطي", "https://bein-beinxtrasports-firetv.amagi.tv/playlist.m3u8", ref),
            )
        }

        return streams.map { (streamName, url, referer) ->
            YacineStream(
                name = streamName,
                url = url,
                userAgent = ua,
                referer = referer,
                urlType = 3,
                headers = mapOf("User-Agent" to ua, "Referer" to referer),
            )
        }
    }

    private fun extractChannelNumber(channelName: String): Int {
        val m = Regex("\\b(\\d+)\\b").find(channelName.substringAfter("SPORTS").substringAfter("Sports").substringAfter("بي إن"))
        return m?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    suspend fun getMatchStreams(
        eventId: String,
        channelHint: String,
        leagueHint: String,
    ): List<YacineStream> = withContext(Dispatchers.IO) {
        val hint = channelHint.trim()
        val targetEvent = REAL_YACINE_EVENTS.firstOrNull { it.id == eventId }
        val channelToUse = when {
            hint.isNotBlank() -> hint
            targetEvent != null && targetEvent.channel.isNotBlank() -> targetEvent.channel
            else -> "beIN SPORTS 1"
        }

        // Match with specific beIN channel
        val matchedChannel = BEIN_FALLBACK_CHANNELS.firstOrNull { ch ->
            val n1 = normalizeChannelName(ch.name)
            val n2 = normalizeChannelName(channelToUse)
            n1.contains(n2) || n2.contains(n1)
        }

        val finalChannelName = matchedChannel?.name ?: channelToUse
        getStreamsForChannelName(finalChannelName)
    }

    suspend fun getChannelStreams(channelId: String): List<YacineStream> = withContext(Dispatchers.IO) {
        val id = channelId.lowercase().trim()
        val matchedChannel = BEIN_FALLBACK_CHANNELS.firstOrNull {
            it.id.equals(id, ignoreCase = true) || it.name.contains(id, ignoreCase = true)
        }
        val channelName = matchedChannel?.name ?: "beIN SPORTS HD"
        getStreamsForChannelName(channelName)
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
