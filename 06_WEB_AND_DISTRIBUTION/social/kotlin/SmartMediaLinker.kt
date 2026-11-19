package com.madar.tv.social

import org.json.JSONArray
import org.json.JSONObject

/**
 * SmartMediaLinker
 * ────────────────
 * Parses Arabic/English media titles, normalizes them, extracts
 * season/episode/part metadata, and builds series trees + franchise graphs.
 *
 * Key features:
 *  - Arabic numeral normalization (٠١٢٣ → 0123, ٠١٢٣ persian → 0123)
 *  - Season detection: "الموسم الثاني", "Season 2", "الجزء 2"
 *  - Episode detection: "الحلقة 5", "Episode 5", "ep.5"
 *  - Franchise linking: sequels, parts, volumes
 *  - Duplicate merge (same title + different server)
 *
 * Usage:
 *   val linker = SmartMediaLinker()
 *   linker.registerItem(itemJson)
 *   val tree = linker.getSeriesTree("مسلسل باب الحارة")
 */
class SmartMediaLinker {

    // ── Internal stores ──────────────────────────────────────────────────────
    private val seriesMap = mutableMapOf<String, SeriesNode>()      // normalized title → node
    private val franchiseMap = mutableMapOf<String, FranchiseNode>() // base title → franchise

    // ── Data models ──────────────────────────────────────────────────────────
    data class MediaMeta(
        val rawTitle: String,
        val normalizedTitle: String,
        val baseTitle: String,          // title without season/part info
        val seasonNumber: Int,
        val episodeNumber: Int,
        val partNumber: Int,
        val year: Int,
        val type: String,               // "series" | "movie"
        val pageUrl: String,
        val providerName: String,
        val quality: String,
        val servers: MutableList<ServerEntry> = mutableListOf(),
    )

    data class ServerEntry(
        val name: String,
        val embedUrl: String,
        val directStreamUrl: String,
        val providerName: String,
        val quality: String,
    )

    data class SeriesNode(
        val baseTitle: String,
        val seasons: MutableMap<Int, SeasonNode> = mutableMapOf(),
    )

    data class SeasonNode(
        val number: Int,
        val episodes: MutableMap<Int, MediaMeta> = mutableMapOf(),
    )

    data class FranchiseNode(
        val baseName: String,
        val parts: MutableList<MediaMeta> = mutableListOf(),
    )

    // ── Public API ───────────────────────────────────────────────────────────
    /** Register a new item (episode or movie) from JSON */
    fun registerItem(json: JSONObject) {
        val meta = parseMeta(json) ?: return
        when (meta.type) {
            "series" -> registerEpisode(meta)
            "movie"  -> registerMovie(meta)
        }
    }

    /** Register a list of items */
    fun registerAll(items: JSONArray) {
        for (i in 0 until items.length()) {
            runCatching { registerItem(items.getJSONObject(i)) }
        }
    }

    /** Get complete series tree as JSON */
    fun getSeriesTree(title: String): JSONObject {
        val key = normalizeTitle(title)
        val node = seriesMap[key] ?: return JSONObject().put("error", "Series not found")
        return seriesNodeToJson(node)
    }

    /** Get franchise info (all movie parts) */
    fun getFranchise(title: String): JSONObject {
        val key = extractBaseTitle(normalizeTitle(title))
        val node = franchiseMap[key] ?: return JSONObject().put("error", "Franchise not found")
        return franchiseNodeToJson(node)
    }

    /** Get all registered series as JSON array */
    fun getAllSeries(): JSONArray {
        val arr = JSONArray()
        seriesMap.values.forEach { arr.put(seriesNodeToJson(it)) }
        return arr
    }

    /** Get all registered franchises */
    fun getAllFranchises(): JSONArray {
        val arr = JSONArray()
        franchiseMap.values.forEach { arr.put(franchiseNodeToJson(it)) }
        return arr
    }

    /** Find the next unwatched episode after [currentEpisode] in season [season] */
    fun getNextEpisode(seriesTitle: String, season: Int, currentEpisode: Int): JSONObject? {
        val key = normalizeTitle(seriesTitle)
        val node = seriesMap[key] ?: return null
        val seasonNode = node.seasons[season] ?: return null
        val next = seasonNode.episodes[currentEpisode + 1] ?: return null
        return mediaMetaToJson(next)
    }

    // ── Internal ─────────────────────────────────────────────────────────────
    private fun registerEpisode(meta: MediaMeta) {
        val key = normalizeTitle(meta.baseTitle)
        val series = seriesMap.getOrPut(key) { SeriesNode(meta.baseTitle) }
        val season = series.seasons.getOrPut(meta.seasonNumber) { SeasonNode(meta.seasonNumber) }

        val existing = season.episodes[meta.episodeNumber]
        if (existing != null) {
            // Merge servers
            meta.servers.forEach { newServer ->
                if (existing.servers.none { it.embedUrl == newServer.embedUrl }) {
                    existing.servers.add(newServer)
                }
            }
        } else {
            season.episodes[meta.episodeNumber] = meta
        }
    }

    private fun registerMovie(meta: MediaMeta) {
        if (meta.partNumber <= 0) return
        val key = extractBaseTitle(normalizeTitle(meta.rawTitle))
        val franchise = franchiseMap.getOrPut(key) { FranchiseNode(meta.baseTitle) }
        if (franchise.parts.none { it.partNumber == meta.partNumber }) {
            franchise.parts.add(meta)
            franchise.parts.sortBy { it.partNumber }
        }
    }

    // ── Title parsing ────────────────────────────────────────────────────────
    fun parseMeta(json: JSONObject): MediaMeta? {
        val rawTitle = json.optString("title").trim()
        if (rawTitle.isEmpty()) return null

        val normalized = normalizeTitle(rawTitle)
        val season = extractSeasonNumber(normalized)
        val episode = extractEpisodeNumber(normalized)
        val part = extractPartNumber(normalized)
        val base = extractBaseTitle(normalized)
        val type = if (json.optString("type") == "movie" && episode <= 0) "movie" else "series"

        val servers = mutableListOf<ServerEntry>()
        val serversJson = json.optJSONArray("servers")
        if (serversJson != null) {
            for (i in 0 until serversJson.length()) {
                val s = serversJson.optJSONObject(i) ?: continue
                servers.add(
                    ServerEntry(
                        name = s.optString("name"),
                        embedUrl = s.optString("embedUrl"),
                        directStreamUrl = s.optString("directStreamUrl"),
                        providerName = s.optString("providerName"),
                        quality = s.optString("quality", "HD"),
                    )
                )
            }
        }

        return MediaMeta(
            rawTitle = rawTitle,
            normalizedTitle = normalized,
            baseTitle = base,
            seasonNumber = if (season > 0) season else 1,
            episodeNumber = episode,
            partNumber = part,
            year = extractYear(rawTitle),
            type = type,
            pageUrl = json.optString("pageUrl"),
            providerName = json.optString("providerName"),
            quality = json.optString("quality"),
            servers = servers,
        )
    }

    // ── Normalization helpers ─────────────────────────────────────────────────
    fun normalizeTitle(title: String): String {
        var t = title.lowercase()
        // Convert Arabic-Indic numerals ٠١٢٣٤٥٦٧٨٩
        val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        for (i in 0..9) {
            t = t.replace(arabicDigits[i].toString(), i.toString())
            t = t.replace(persianDigits[i].toString(), i.toString())
        }
        // Normalize hamza variations
        t = t.replace(Regex("[أإآ]"), "ا")
        t = t.replace("ة", "ه")
        t = t.replace("ى", "ي")
        // Remove diacritics
        t = t.replace(Regex("[\u064B-\u065F]"), "")
        // Collapse spaces
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    private fun extractSeasonNumber(title: String): Int {
        // "الموسم الثاني" / "الموسم 2" / "Season 2" / "s02"
        val ordinals = mapOf(
            "الاول" to 1, "الثاني" to 2, "الثالث" to 3,
            "الرابع" to 4, "الخامس" to 5, "السادس" to 6,
            "السابع" to 7, "الثامن" to 8, "التاسع" to 9, "العاشر" to 10
        )
        val ordinalRx = Regex("""(?:الموسم|موسم|season)\s+(ال\w+|\d+)""")
        val m1 = ordinalRx.find(title)
        if (m1 != null) {
            val g = m1.groupValues[1]
            return g.toIntOrNull() ?: ordinals.entries.firstOrNull { g.contains(it.key) }?.value ?: 0
        }
        val numRx = Regex("""s(\d{1,2})e\d+""")
        val m2 = numRx.find(title)
        if (m2 != null) return m2.groupValues[1].toInt()
        return 0
    }

    private fun extractEpisodeNumber(title: String): Int {
        // "الحلقة 5" / "Episode 5" / "s01e05" / "ep5" / "ep.5"
        val patterns = listOf(
            Regex("""(?:الحلقه|الحلقة|حلقه|حلقة|episode|ep\.?)\s*(\d+)"""),
            Regex("""s\d{1,2}e(\d{1,3})"""),
            Regex("""e(\d{2,3})\b"""),
        )
        for (rx in patterns) {
            val m = rx.find(title) ?: continue
            return m.groupValues[1].toIntOrNull() ?: continue
        }
        return 0
    }

    private fun extractPartNumber(title: String): Int {
        val rx = Regex("""(?:الجزء|جزء|part|pt\.?)\s*(\d+|الثاني|الثالث|الرابع)""")
        val m = rx.find(title) ?: return 0
        val g = m.groupValues[1]
        return g.toIntOrNull() ?: mapOf(
            "الثاني" to 2, "الثالث" to 3, "الرابع" to 4
        )[g] ?: 0
    }

    private fun extractBaseTitle(title: String): String {
        // Remove season/episode/part/year markers
        return title
            .replace(Regex("""(?:الموسم|موسم|season)\s+[\w\d]+"""), "")
            .replace(Regex("""(?:الجزء|جزء|part|pt\.?)\s+[\w\d]+"""), "")
            .replace(Regex("""(?:الحلقه|الحلقة|حلقه|حلقة|episode|ep\.?)\s*\d+"""), "")
            .replace(Regex("""s\d{1,2}e\d{1,3}"""), "")
            .replace(Regex("""\(\d{4}\)"""), "")
            .replace(Regex("""[:\-–|]+$"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractYear(title: String): Int {
        val m = Regex("""\b(19|20)\d{2}\b""").find(title) ?: return 0
        return m.value.toIntOrNull() ?: 0
    }

    // ── JSON serializers ─────────────────────────────────────────────────────
    private fun seriesNodeToJson(node: SeriesNode): JSONObject {
        val seasonsArr = JSONArray()
        node.seasons.toSortedMap().forEach { (_, season) ->
            val epArr = JSONArray()
            season.episodes.toSortedMap().forEach { (_, ep) ->
                epArr.put(mediaMetaToJson(ep))
            }
            seasonsArr.put(
                JSONObject()
                    .put("number", season.number)
                    .put("episodeCount", season.episodes.size)
                    .put("episodes", epArr)
            )
        }
        return JSONObject()
            .put("baseTitle", node.baseTitle)
            .put("seasonCount", node.seasons.size)
            .put("seasons", seasonsArr)
    }

    private fun franchiseNodeToJson(node: FranchiseNode): JSONObject {
        val partsArr = JSONArray()
        node.parts.forEach { partsArr.put(mediaMetaToJson(it)) }
        return JSONObject()
            .put("baseName", node.baseName)
            .put("partCount", node.parts.size)
            .put("parts", partsArr)
    }

    private fun mediaMetaToJson(meta: MediaMeta): JSONObject {
        val serversArr = JSONArray()
        meta.servers.forEach { s ->
            serversArr.put(
                JSONObject()
                    .put("name", s.name)
                    .put("embedUrl", s.embedUrl)
                    .put("directStreamUrl", s.directStreamUrl)
                    .put("providerName", s.providerName)
                    .put("quality", s.quality)
            )
        }
        return JSONObject()
            .put("title", meta.rawTitle)
            .put("baseTitle", meta.baseTitle)
            .put("season", meta.seasonNumber)
            .put("episode", meta.episodeNumber)
            .put("part", meta.partNumber)
            .put("year", meta.year)
            .put("type", meta.type)
            .put("pageUrl", meta.pageUrl)
            .put("quality", meta.quality)
            .put("servers", serversArr)
    }
}
