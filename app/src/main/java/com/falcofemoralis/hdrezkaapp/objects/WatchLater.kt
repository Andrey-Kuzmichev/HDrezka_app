package com.falcofemoralis.hdrezkaapp.objects

data class WatchLater(
    val id: String,
    val date: String,
    val filmLInk: String,
    val name: String,
    val info: String,
    val additionalInfo: String
) {
    var posterPath: String? = null
}
