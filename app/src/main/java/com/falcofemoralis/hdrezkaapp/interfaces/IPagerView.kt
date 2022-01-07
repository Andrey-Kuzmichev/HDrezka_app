package com.falcofemoralis.hdrezkaapp.interfaces

import com.falcofemoralis.hdrezkaapp.constants.UpdateItem

interface IPagerView {
    fun updatePager()

    fun redrawPage(item: UpdateItem)
}