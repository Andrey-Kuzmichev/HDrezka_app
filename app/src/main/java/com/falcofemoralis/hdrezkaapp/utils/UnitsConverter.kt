package com.falcofemoralis.hdrezkaapp.utils

import android.content.Context
import android.util.TypedValue

object UnitsConverter {
    fun getPX(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}