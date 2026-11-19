package com.madar.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "follow_prefs")

object FollowRepository {

    private val FOLLOWED_KEY    = stringPreferencesKey("followed_series")
    private val SEEN_MOVIES_KEY = stringPreferencesKey("seen_movies")
    private val SEEN_SERIES_KEY = stringPreferencesKey("seen_series")

    // ── Follow/Unfollow Series ────────────────────────────────────────────────

    fun getFollowedSeries(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            parseJsonArray(prefs[FOLLOWED_KEY] ?: "[]")
        }

    suspend fun toggleFollow(context: Context, pageUrl: String): Boolean {
        var isNowFollowed = false
        context.dataStore.edit { prefs ->
            val current = parseJsonArray(prefs[FOLLOWED_KEY] ?: "[]").toMutableSet()
            if (current.contains(pageUrl)) {
                current.remove(pageUrl)
                isNowFollowed = false
            } else {
                current.add(pageUrl)
                isNowFollowed = true
            }
            prefs[FOLLOWED_KEY] = buildJsonArray(current)
        }
        return isNowFollowed
    }

    suspend fun isFollowed(context: Context, pageUrl: String): Boolean {
        val prefs = context.dataStore.data.first()
        return parseJsonArray(prefs[FOLLOWED_KEY] ?: "[]").contains(pageUrl)
    }

    // ── Track Seen Content (to detect NEW items) ──────────────────────────────

    suspend fun getSeenMovieUrls(context: Context): Set<String> {
        val prefs = context.dataStore.data.first()
        return parseJsonArray(prefs[SEEN_MOVIES_KEY] ?: "[]")
    }

    suspend fun getSeenSeriesUrls(context: Context): Set<String> {
        val prefs = context.dataStore.data.first()
        return parseJsonArray(prefs[SEEN_SERIES_KEY] ?: "[]")
    }

    suspend fun saveSeenMovies(context: Context, urls: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SEEN_MOVIES_KEY] = buildJsonArray(urls)
        }
    }

    suspend fun saveSeenSeries(context: Context, urls: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SEEN_SERIES_KEY] = buildJsonArray(urls)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseJsonArray(json: String): Set<String> {
        return try {
            val arr = JSONArray(json)
            buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
        } catch (_: Exception) { emptySet() }
    }

    private fun buildJsonArray(set: Set<String>): String {
        val arr = JSONArray()
        set.forEach { arr.put(it) }
        return arr.toString()
    }
}
