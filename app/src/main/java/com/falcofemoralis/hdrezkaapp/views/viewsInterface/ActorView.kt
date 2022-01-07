package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import android.util.ArrayMap
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.objects.Actor
import com.falcofemoralis.hdrezkaapp.objects.Film

interface ActorView : IConnection {
    fun setBaseInfo(actor: Actor)

    fun setCareersList(careers: ArrayList<Pair<String, ArrayList<Film>>>)
}