package com.falcofemoralis.hdrezkaapp.objects

class DatabaseSaveItem(
    val filmId: Int,
) {
    var translationId: String? = null
    var season: String? = null
    var episode: String? = null
    var lastTime: Long? = null
}