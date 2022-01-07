package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import com.falcofemoralis.hdrezkaapp.constants.AdapterAction
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IMsg
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.WatchLater

interface WatchLaterView : IMsg, IProgressState, IConnection {
    fun setWatchLaterList(list: ArrayList<WatchLater>)

    fun redrawWatchLaterList(from: Int, count: Int, action: AdapterAction)
}