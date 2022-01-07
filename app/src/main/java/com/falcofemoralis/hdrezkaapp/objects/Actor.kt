package com.falcofemoralis.hdrezkaapp.objects

import java.io.Serializable

data class Actor(
    val id: Int,
    val pid: Int
) : Serializable {
    var age: String? = null
    var diedOnAge: String? = null
    var birthday: String? = null
    var birthplace: String? = null
    var careers: String? = null
    var deathday: String? = null
    var deathplace: String? = null
    var link: String? = null
    var name: String? = null
    var nameOrig: String? = null
    var photo: String? = null
    var personCareerFilms: ArrayList<Pair<String, ArrayList<Film>>>? = null
    var hasMainData = false
}