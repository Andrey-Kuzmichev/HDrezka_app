package com.falcofemoralis.hdrezkaapp.objects

import java.io.Serializable

data class Thumbnail(
    val t1: Float,
    val t2: Float,
    val url: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) : Serializable
