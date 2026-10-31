package com.madar.tv.data

data class YacineCategory(
    val id: String,
    val name: String,
    val logo: String,
    val childCount: Int
)

data class YacineChannel(
    val id: String,
    val name: String,
    val logo: String,
    val isHide: Int,
    val priority: Int
)

data class YacineTeam(
    val id: String,
    val name: String,
    val logo: String
)

data class YacineEvent(
    val id: String,
    val champions: String,
    val channel: String,
    val startTime: Long,
    val endTime: Long,
    val commentary: String,
    val team1: YacineTeam,
    val team2: YacineTeam
)

data class YacineStream(
    val name: String,
    val url: String,
    val userAgent: String,
    val referer: String,
    val urlType: Int,
    val headers: Map<String, String> = emptyMap(),
)
