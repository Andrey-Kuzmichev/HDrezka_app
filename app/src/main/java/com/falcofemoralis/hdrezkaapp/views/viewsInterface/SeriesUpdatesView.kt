package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import com.falcofemoralis.hdrezkaapp.constants.HintType
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SeriesUpdateItem

interface SeriesUpdatesView: IConnection {
    fun setList(films: LinkedHashMap<String, ArrayList<Film>>)

    fun setHint(hintType: HintType)

    fun updateDialog(seriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>>)

    fun resetBadge()
}