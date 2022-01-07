package com.falcofemoralis.hdrezkaapp.objects

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.falcofemoralis.hdrezkaapp.utils.CookieStorage
import com.falcofemoralis.hdrezkaapp.utils.FileManager
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

object UserData {
    private const val USER_FILE: String = "user"
    private const val USER_AVATAR: String = "avatar"
    private const val USER_ID: String = "id"
    private const val USER_HASH: String = "hash"
    private const val SESSION_ID: String = "session"
    private const val SERIES_UPDATES_FILE = "series_updates"

    var isLoggedIn: Boolean? = null
    var avatarLink: String? = null

    fun init(context: Context) {
        val data: String? = FileManager.readFile(USER_FILE, context)
        isLoggedIn = if (data != null) {
            data == "1"
        } else {
            false
        }

        avatarLink = FileManager.readFile(USER_AVATAR, context)

        if (isLoggedIn == true) {
            try {
                val dle_user_id = CookieStorage.getCookie(SettingsData.provider, "dle_user_id")
                if (dle_user_id.isNullOrEmpty()) {
                    loadCookies(context)
                }
            } catch (e: Exception) {
                loadCookies(context)
            }
        }
    }

    fun setLoggedIn(context: Context) {
        isLoggedIn = true
        FileManager.writeFile(USER_FILE, "1", false, context)
    }

    fun setAvatar(avatarLink: String?, context: Context) {
        if (avatarLink != null) {
            this.avatarLink = avatarLink
            FileManager.writeFile(USER_AVATAR, avatarLink, false, context)
        }
    }

    fun setCookies(user_id: String?, password: String?, session: String?, context: Context, isSave: Boolean) {
        val cm = CookieManager.getInstance()

        user_id?.let { cm.setCookie(SettingsData.provider, "dle_user_id=${it}") }
        password?.let { cm.setCookie(SettingsData.provider, "dle_password=${it}") }
        session?.let { cm.setCookie(SettingsData.provider, "PHPSESSID=${it}") }
        cm.acceptCookie()

        if (isSave) {
            saveCookies(user_id, password, session, context)
        }
    }

    private fun saveCookies(user_id: String?, password: String?, session: String?, context: Context) {
        user_id?.let { FileManager.writeFile(USER_ID, it, false, context) }
        password?.let { FileManager.writeFile(USER_HASH, it, false, context) }
        session?.let { FileManager.writeFile(SESSION_ID, it, false, context) }
    }

    private fun loadCookies(context: Context) {
        val id = FileManager.readFile(USER_ID, context)
        val hash = FileManager.readFile(USER_HASH, context)
        val session = FileManager.readFile(SESSION_ID, context)

        setCookies(id, hash, session, context, false)
    }

    fun reset(context: Context) {
        isLoggedIn = false
        val cm = CookieManager.getInstance()
        cm.setCookie(SettingsData.provider, null)
        cm.removeAllCookies(null)
        cm.flush()
        WebStorage.getInstance().deleteAllData()
        FileManager.writeFile(USER_FILE, "0", false, context)
        FileManager.writeFile(USER_AVATAR, "", false, context)
        FileManager.writeFile(USER_ID, "", false, context)
        FileManager.writeFile(USER_HASH, "", false, context)
        FileManager.writeFile(SESSION_ID, "", false, context)

        avatarLink = null
    }

    fun saveUserSeriesUpdates(list: ArrayList<SeriesUpdateItem>?, context: Context){
        FileManager.writeFile(SERIES_UPDATES_FILE, Gson().toJson(list), false, context)
    }

    fun getUserSeriesUpdates(context: Context) : ArrayList<SeriesUpdateItem> {
        val str = FileManager.readFile(SERIES_UPDATES_FILE, context)
        val myType = object : TypeToken<List<SeriesUpdateItem>>() {}.type
        val list = Gson().fromJson<List<SeriesUpdateItem>>(str, myType)
        return if(list == null){
            ArrayList()
        } else {
            list as ArrayList<SeriesUpdateItem>
        }
    }
}