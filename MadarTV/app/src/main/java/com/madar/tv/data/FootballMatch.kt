package com.madar.tv.data

data class FootballMatch(
    val id: String,
    val leagueName: String,
    val leagueCode: String,
    val homeTeam: String,
    val homeTeamArabic: String,
    val homeLogo: String,
    val homeScore: String = "",
    val awayTeam: String,
    val awayTeamArabic: String,
    val awayLogo: String,
    val awayScore: String = "",
    val timeOrStatus: String,
    val state: String, // "pre", "in", "post"
    val matchDate: String,
)
