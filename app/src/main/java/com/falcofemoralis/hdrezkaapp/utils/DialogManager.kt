package com.falcofemoralis.hdrezkaapp.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

object DialogManager {
    fun getDialog(context: Context, titleResId: Int?, isCancelable: Boolean = true): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        if (titleResId != null) {
            builder.setTitle(context.getString(titleResId))
        }
        builder.setCancelable(isCancelable)

        return builder
    }
}