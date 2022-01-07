package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IMsg

interface BookmarksView : IMsg, IConnection {
    fun setBookmarksSpinner(bookmarksNames: ArrayList<String>)

    fun setNoBookmarks()
}