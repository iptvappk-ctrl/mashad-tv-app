package com.madar.tv.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/** Small app-private stale-while-revalidate cache for catalog data. */
object CatalogDiskCache {
    data class Snapshot<T>(val value: T, val savedAt: Long)

    private const val CACHE_FOLDER = "catalog-cache-v1"
    private const val MAX_STALE_MS = 14L * 24 * 60 * 60 * 1000
    private const val MAX_FILES = 180
    private const val MAX_BYTES = 40L * 1024 * 1024

    @Volatile private var root: File? = null
    private val writes = Any()
    private val tempCounter = AtomicLong()

    fun initialize(context: Context) {
        val directory = File(context.noBackupFilesDir, CACHE_FOLDER)
        if (!directory.exists()) directory.mkdirs()
        root = directory
    }

    fun loadItems(key: String, maxStaleMs: Long = MAX_STALE_MS): Snapshot<List<ArabseedItem>>? {
        val snapshot = read(key, maxStaleMs) ?: return null
        val rows = snapshot.payload.optJSONArray("items") ?: return null
        val items = buildList(rows.length()) {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index)?.toArabseedItem() ?: continue
                add(item)
            }
        }
        return if (items.isEmpty()) null else Snapshot(items, snapshot.savedAt)
    }

    fun saveItems(key: String, items: List<ArabseedItem>) {
        if (items.isEmpty()) return
        val rows = JSONArray()
        items.forEach { rows.put(it.toJson()) }
        write(key, JSONObject().put("items", rows))
    }

    fun loadDetail(key: String, maxStaleMs: Long = MAX_STALE_MS): Snapshot<ArabseedDetail>? {
        val snapshot = read(key, maxStaleMs) ?: return null
        val payload = snapshot.payload
        val item = payload.optJSONObject("item")?.toArabseedItem() ?: return null
        val serverRows = payload.optJSONArray("servers") ?: JSONArray()
        val servers = buildList(serverRows.length()) {
            for (index in 0 until serverRows.length()) {
                val row = serverRows.optJSONObject(index) ?: continue
                val url = row.optString("embedUrl")
                if (url.isNotBlank()) {
                    add(ArabseedServer(row.optString("name"), url))
                }
            }
        }
        val castRows = payload.optJSONArray("cast") ?: JSONArray()
        val cast = buildList(castRows.length()) {
            for (index in 0 until castRows.length()) castRows.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
        if (servers.isEmpty()) return null
        return Snapshot(
            ArabseedDetail(item, servers, payload.nullableString("preResolvedStream"), cast),
            snapshot.savedAt,
        )
    }

    fun saveDetail(key: String, detail: ArabseedDetail) {
        if (detail.servers.isEmpty()) return
        // Embed and resolved stream URLs can expire or carry session tokens. Keep only
        // their stable server pages in the offline metadata cache.
        val servers = JSONArray().apply {
            detail.servers.forEach { server -> put(JSONObject().put("name", server.name).put("embedUrl", server.embedUrl)) }
        }
        val cast = JSONArray().apply { detail.cast.forEach(::put) }
        write(
            key,
            JSONObject()
                .put("item", detail.item.toJson())
                .put("servers", servers)
                .put("cast", cast)
                .put("preResolvedStream", JSONObject.NULL),
        )
    }

    fun loadSeasonBundle(key: String, maxStaleMs: Long = MAX_STALE_MS): Snapshot<ArabseedSeasonBundle>? {
        val snapshot = read(key, maxStaleMs) ?: return null
        val seasonRows = snapshot.payload.optJSONArray("seasons") ?: JSONArray()
        val seasons = buildList(seasonRows.length()) {
            for (seasonIndex in 0 until seasonRows.length()) {
                val season = seasonRows.optJSONObject(seasonIndex) ?: continue
                val episodeRows = season.optJSONArray("episodes") ?: JSONArray()
                val episodes = buildList(episodeRows.length()) {
                    for (episodeIndex in 0 until episodeRows.length()) {
                        val episode = episodeRows.optJSONObject(episodeIndex) ?: continue
                        val url = episode.optString("pageUrl")
                        if (url.isNotBlank()) add(
                            ArabseedEpisode(
                                number = episode.optInt("number"),
                                title = episode.optString("title"),
                                pageUrl = url,
                                seasonNumber = episode.optInt("seasonNumber", season.optInt("number", 1)),
                                providerName = episode.optString("providerName"),
                            ),
                        )
                    }
                }
                add(ArabseedSeason(
                    number = season.optInt("number", 1),
                    title = season.optString("title"),
                    episodes = episodes,
                    pageUrl = season.optString("pageUrl"),
                ))
            }
        }
        val partRows = snapshot.payload.optJSONArray("relatedParts") ?: JSONArray()
        val parts = buildList(partRows.length()) {
            for (index in 0 until partRows.length()) {
                partRows.optJSONObject(index)?.toArabseedItem()?.let(::add)
            }
        }
        return Snapshot(ArabseedSeasonBundle(seasons, parts), snapshot.savedAt)
    }

    fun saveSeasonBundle(key: String, bundle: ArabseedSeasonBundle) {
        val seasons = JSONArray().apply {
            bundle.seasons.forEach { season ->
                val episodes = JSONArray().apply {
                    season.episodes.forEach { episode ->
                        put(
                            JSONObject()
                                .put("number", episode.number)
                                .put("title", episode.title)
                                .put("pageUrl", episode.pageUrl)
                                .put("seasonNumber", episode.seasonNumber)
                                .put("providerName", episode.providerName),
                        )
                    }
                }
                put(
                    JSONObject()
                        .put("number", season.number)
                        .put("title", season.title)
                        .put("pageUrl", season.pageUrl)
                        .put("episodes", episodes),
                )
            }
        }
        val parts = JSONArray().apply { bundle.relatedParts.forEach { put(it.toJson()) } }
        write(key, JSONObject().put("seasons", seasons).put("relatedParts", parts))
    }

    fun loadJson(key: String, maxStaleMs: Long = MAX_STALE_MS): Snapshot<JSONObject>? {
        val snapshot = read(key, maxStaleMs) ?: return null
        return Snapshot(snapshot.payload, snapshot.savedAt)
    }

    fun saveJson(key: String, payload: JSONObject) = write(key, payload)

    private data class RawSnapshot(val payload: JSONObject, val savedAt: Long)

    private fun read(key: String, maxStaleMs: Long): RawSnapshot? {
        val file = fileFor(key) ?: return null
        if (!file.isFile) return null
        return runCatching {
            val envelope = JSONObject(file.readText(Charsets.UTF_8))
            val savedAt = envelope.optLong("savedAt", 0L)
            if (savedAt <= 0L || System.currentTimeMillis() - savedAt > maxStaleMs) return null
            file.setLastModified(System.currentTimeMillis())
            RawSnapshot(envelope.getJSONObject("payload"), savedAt)
        }.getOrNull()
    }

    private fun write(key: String, payload: JSONObject) {
        val target = fileFor(key) ?: return
        synchronized(writes) {
            runCatching {
                val temporary = File(target.parentFile, "${target.name}.${tempCounter.incrementAndGet()}.tmp")
                val envelope = JSONObject().put("savedAt", System.currentTimeMillis()).put("payload", payload)
                temporary.writeText(envelope.toString(), Charsets.UTF_8)
                if (target.exists()) target.delete()
                if (!temporary.renameTo(target)) {
                    target.writeText(envelope.toString(), Charsets.UTF_8)
                    temporary.delete()
                }
                target.setLastModified(System.currentTimeMillis())
                trim()
            }
        }
    }

    private fun fileFor(key: String): File? {
        val directory = root ?: return null
        if (!directory.exists() && !directory.mkdirs()) return null
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return File(directory, "$digest.json")
    }

    private fun trim() {
        val files = root?.listFiles { file -> file.isFile && file.extension == "json" }?.sortedBy(File::lastModified)
            ?: return
        var totalBytes = files.sumOf(File::length)
        var remaining = files.size
        for (file in files) {
            if (remaining <= MAX_FILES && totalBytes <= MAX_BYTES) break
            val size = file.length()
            if (file.delete()) {
                remaining--
                totalBytes -= size
            }
        }
    }

    private fun ArabseedItem.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("arabicTitle", arabicTitle)
        .put("posterUrl", posterUrl ?: JSONObject.NULL)
        .put("year", year)
        .put("quality", quality)
        .put("rating", rating)
        .put("type", type.name)
        .put("pageUrl", pageUrl)
        .put("category", category)
        .put("overview", overview)
        .put("providerName", providerName)
        .put("parentSeriesTitle", parentSeriesTitle ?: JSONObject.NULL)
        .put("parentSeriesPageUrl", parentSeriesPageUrl ?: JSONObject.NULL)
        .put("latestEpisodeLabel", latestEpisodeLabel)

    private fun JSONObject.toArabseedItem(): ArabseedItem? {
        val pageUrl = optString("pageUrl")
        val type = runCatching { ArabseedType.valueOf(optString("type", ArabseedType.MOVIE.name)) }
            .getOrDefault(ArabseedType.MOVIE)
        if (pageUrl.isBlank()) return null
        return ArabseedItem(
            id = optString("id", pageUrl.hashCode().toString()),
            title = optString("title"),
            arabicTitle = optString("arabicTitle", optString("title")),
            posterUrl = nullableString("posterUrl"),
            year = optString("year"),
            quality = optString("quality"),
            rating = optString("rating"),
            type = type,
            pageUrl = pageUrl,
            category = optString("category"),
            overview = optString("overview"),
            providerName = optString("providerName", "عرب سيد"),
            parentSeriesTitle = nullableString("parentSeriesTitle"),
            parentSeriesPageUrl = nullableString("parentSeriesPageUrl"),
            latestEpisodeLabel = optString("latestEpisodeLabel"),
        )
    }

    private fun ArabseedServer.toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("embedUrl", embedUrl)
        .put("directStreamUrl", directStreamUrl ?: JSONObject.NULL)

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)
}
