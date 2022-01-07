package com.falcofemoralis.hdrezkaapp.objects

import java.io.Serializable

open class Voice : Serializable {
    constructor(name: String, id: String) {
        this.name = name
        this.id = id
    }

    constructor(streams: ArrayList<Stream>) {
        this.streams = streams
    }

    constructor(id: String, seasons: LinkedHashMap<String, ArrayList<String>>) {
        this.id = id
        this.seasons = seasons
    }

    var name: String? = null
    var id: String? = null
    var streams: ArrayList<Stream>? = null
    var seasons: LinkedHashMap<String, ArrayList<String>>? = null
    var isCamrip: String = "0"
    var isDirector: String = "0"
    var isAds: String = "0"
    var selectedEpisode: Pair<String, String>? = null
    var subtitles: ArrayList<Subtitle>? = null
    var thumbnails: ArrayList<Thumbnail>? = null
}

