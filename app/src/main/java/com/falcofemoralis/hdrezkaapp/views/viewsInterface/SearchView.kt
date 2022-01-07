package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import com.falcofemoralis.hdrezkaapp.interfaces.IConnection

interface SearchView : IConnection {
    fun redrawSearchFilms(films: ArrayList<String>)
}