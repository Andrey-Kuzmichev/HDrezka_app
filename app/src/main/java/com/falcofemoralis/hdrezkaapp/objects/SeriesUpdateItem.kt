package com.falcofemoralis.hdrezkaapp.objects

data class SeriesUpdateItem(
    val filmLink: String,
    val title: String,
    val season: String,
    val episode: String,
    val voice: String?,
    val isUserWatch: Boolean
)
