package com.falcofemoralis.hdrezkaapp.objects

import android.util.ArrayMap

data class Category(
    var categories: ArrayMap<Pair<String, String>, ArrayList<Pair<String, String>>>, // key = <headerName, headerLink>, value = <genreName, genreLink>
    var years: ArrayList<String>
)