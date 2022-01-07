package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import android.util.ArrayMap
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection

interface CategoriesView : IConnection{
    fun showList()

    fun showFilters()

    fun setFilters(years: ArrayList<String>, genres: ArrayList<String>)
}