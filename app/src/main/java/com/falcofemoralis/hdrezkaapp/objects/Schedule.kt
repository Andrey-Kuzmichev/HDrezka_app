package com.falcofemoralis.hdrezkaapp.objects

import java.io.Serializable

data class Schedule(
    val episode: String,
    val name: String,
    val date: String,
    var isWatched: Boolean
) : Serializable {
    var nextEpisodeIn: String? = null
    var watchId: Int? = null
}

