package com.falcofemoralis.hdrezkaapp.utils

import android.webkit.CookieManager

object CookieStorage {
    fun getCookie(siteName: String?, cookieName: String?): String? {
        var cookieValue: String? = null
        val cookieManager: CookieManager = CookieManager.getInstance()
        val cookies: String = cookieManager.getCookie(siteName)
        val temp = cookies.split(";").toTypedArray()
        for (ar1 in temp) {
            if (ar1.contains(cookieName!!)) {
                val temp1 = ar1.split("=").toTypedArray()
                cookieValue = temp1[1]
                break
            }
        }
        return cookieValue
    }
}