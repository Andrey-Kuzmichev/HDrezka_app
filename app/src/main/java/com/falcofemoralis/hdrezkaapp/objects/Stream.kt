package com.falcofemoralis.hdrezkaapp.objects

import java.io.Serializable

data class Stream(
    val url: String,
    val quality: String
): Serializable
