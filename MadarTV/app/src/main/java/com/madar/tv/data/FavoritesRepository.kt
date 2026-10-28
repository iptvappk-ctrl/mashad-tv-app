package com.madar.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// Separate DataStore for favorites (avoids conflicts with follow_prefs)
private val Context.favDataStore by preferencesDataStore(name = "favorites_prefs")

object FavoritesRepository {

    private val FAVORITES_KEY = stringPreferencesKey("favorites_items")

    // ── Favorites Flow (reactive) ─────────────────────────────────────────────

    fun getFavoritesFlow(context: Context): Flow<List<ArabseedItem>> =
        context.favDataStore.data.map { prefs ->
            parseItemsJson(prefs[FAVORITES_KEY] ?: "[]")
        }

    // ── One-shot reads ────────────────────────────────────────────────────────

    suspend fun getFavorites(context: Context): List<ArabseedItem> {
        val prefs = context.favDataStore.data.first()
        return parseItemsJson(prefs[FAVORITES_KEY] ?: "[]")
    }

    suspend fun isFavorite(context: Context, pageUrl: String): Boolean {
        val prefs = context.favDataStore.data.first()
        return parseItemsJson(prefs[FAVORITES_KEY] ?: "[]").any { it.pageUrl == pageUrl }
    }

    // ── Toggle favorite ───────────────────────────────────────────────────────

    suspend fun toggleFavorite(context: Context, item: ArabseedItem): Boolean {
        var isNowFavorite = false
        context.favDataStore.edit { prefs ->
            val current = parseItemsJson(prefs[FAVORITES_KEY] ?: "[]").toMutableList()
            val idx = current.indexOfFirst { it.pageUrl == item.pageUrl }
            if (idx >= 0) {
                current.removeAt(idx)
                isNowFavorite = false
            } else {
                current.add(0, item) // Add to top
                isNowFavorite = true
            }
            prefs[FAVORITES_KEY] = buildItemsJson(current)
        }
        return isNowFavorite
    }

    suspend fun removeFavorite(context: Context, pageUrl: String) {
        context.favDataStore.edit { prefs ->
            val current = parseItemsJson(prefs[FAVORITES_KEY] ?: "[]").toMutableList()
            current.removeAll { it.pageUrl == pageUrl }
            prefs[FAVORITES_KEY] = buildItemsJson(current)
        }
    }

    // ── JSON Helpers ──────────────────────────────────────────────────────────

    private fun parseItemsJson(json: String): List<ArabseedItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ArabseedItem(
                    id           = obj.optString("id"),
                    title        = obj.optString("title"),
                    arabicTitle  = obj.optString("arabicTitle"),
                    posterUrl    = obj.optString("posterUrl").ifEmpty { null },
                    year         = obj.optString("year"),
                    quality      = obj.optString("quality"),
                    rating       = obj.optString("rating"),
                    type         = if (obj.optString("type") == "MOVIE") ArabseedType.MOVIE else ArabseedType.SERIES,
                    pageUrl      = obj.optString("pageUrl"),
                    category     = obj.optString("category"),
                    overview     = obj.optString("overview"),
                    providerName = obj.optString("providerName", "عرب سيد"),
                    parentSeriesTitle = obj.optString("parentSeriesTitle").ifBlank { null },
                    parentSeriesPageUrl = obj.optString("parentSeriesPageUrl").ifBlank { null },
                    latestEpisodeLabel = obj.optString("latestEpisodeLabel"),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun buildItemsJson(items: List<ArabseedItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id",           item.id)
                put("title",        item.title)
                put("arabicTitle",  item.arabicTitle)
                put("posterUrl",    item.posterUrl ?: "")
                put("year",         item.year)
                put("quality",      item.quality)
                put("rating",       item.rating)
                put("type",         item.type.name)
                put("pageUrl",      item.pageUrl)
                put("category",     item.category)
                put("overview",     item.overview)
                put("providerName", item.providerName)
                put("parentSeriesTitle", item.parentSeriesTitle ?: "")
                put("parentSeriesPageUrl", item.parentSeriesPageUrl ?: "")
                put("latestEpisodeLabel", item.latestEpisodeLabel)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
