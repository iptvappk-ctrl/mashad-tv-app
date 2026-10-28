package com.madar.tv.core

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/** Optional, aggregate product analytics. No device IDs, stream URLs, or raw searches are sent. */
object MashadTelemetry {
    @Volatile
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (analytics != null) return
        analytics = runCatching {
            val appContext = context.applicationContext
            if (FirebaseApp.getApps(appContext).isEmpty()) null
            else FirebaseAnalytics.getInstance(appContext)
        }.getOrNull()
    }

    fun trackContentView(title: String, type: String, category: String, url: String) {
        log(
            "content_open",
            "content_title" to title,
            "content_type" to type,
            "content_category" to category,
        )
    }

    fun trackLiveMatchView(matchTitle: String, league: String) {
        trackContentView(matchTitle, "FOOTBALL_MATCH", league, "")
    }

    /** Counts completed searches without uploading the text the person searched for. */
    fun trackSearchQuery(query: String) {
        val length = query.trim().length
        if (length < 2) return
        val bucket = when {
            length <= 5 -> "2_5"
            length <= 10 -> "6_10"
            else -> "11_plus"
        }
        log("search", "query_length_bucket" to bucket)
    }

    fun trackPlaybackStarted(type: String, title: String = "") {
        log("playback_start", "content_type" to type, "content_title" to title)
    }

    fun trackPlaybackError(type: String) {
        log("playback_error", "content_type" to type)
    }

    private fun log(name: String, vararg values: Pair<String, String>) {
        val client = analytics ?: return
        val params = Bundle().apply {
            values.forEach { (key, rawValue) ->
                val value = rawValue
                    .filterNot { it.isISOControl() }
                    .trim()
                    .take(100)
                if (value.isNotEmpty()) putString(key, value)
            }
        }
        runCatching { client.logEvent(name, params) }
    }
}
