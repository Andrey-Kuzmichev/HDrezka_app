package com.falcofemoralis.hdrezkaapp.objects

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.constants.GridLayoutSizes
import com.falcofemoralis.hdrezkaapp.utils.FileManager

object SettingsData {
    const val PROVIDER_FILE = "provider"
    const val UIMODE_FILE = "uimode"

    var deviceType: DeviceType? = null
    var provider: String? = null
    var mainScreen: Int? = null
    var isPlayer: Boolean? = null
    var isMaxQuality: Boolean? = null
    var isPlayerChooser: Boolean? = null
    var isExternalDownload: Boolean? = null
    var filmsInRow: Int? = null
    var isAutorotate: Boolean? = null
    var autoPlayNextEpisode: Boolean? = null
    var defaultQuality: String? = null
    var isSubtitlesDownload: Boolean? = null
    var isCheckNewVersion: Boolean? = null
    var isAltLoading: Boolean? = null
    var defaultSort: Int? = null
    var isSelectSubtitle: Boolean? = null
    var selectedPlayerPackage: String? = null
    var useragent: String? = null

    fun initProvider(context: Context) {
        if (provider == null || provider == "") {
            try {
                provider = FileManager.readFile(PROVIDER_FILE, context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initDeviceType(context: Context) {
        var str: String? = null
        try {
            str = FileManager.readFile(UIMODE_FILE, context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        deviceType = if (!str.isNullOrEmpty()) {
            DeviceType.values()[str.toInt()]
        } else {
            null
        }
    }

    fun setUIMode(type: DeviceType, context: Context) {
        deviceType = type

        try {
            FileManager.writeFile(UIMODE_FILE, type.ordinal.toString(), false, context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun init(context: Context) {
        val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(context)

        mainScreen = prefs?.getString("screens", "0")?.toInt()
        isPlayer = prefs?.getBoolean("isPlayer", false)
        isMaxQuality = prefs?.getBoolean("isMaxQuality", false)
        isPlayerChooser = prefs?.getBoolean("isPlayerChooser", false)
        isExternalDownload = prefs?.getBoolean("isExternalDownload", false)
        isAutorotate = prefs?.getBoolean("isAutorotate", true)
        autoPlayNextEpisode = prefs?.getBoolean("autoPlayNextEpisode", true)
        isSubtitlesDownload = prefs?.getBoolean("isSubtitlesDownload", true)
        isCheckNewVersion = prefs?.getBoolean("isCheckNewVersion", true)
        isAltLoading = prefs?.getBoolean("isAltLoading", false)
        (prefs?.getString("filmsInRow", (if (deviceType == DeviceType.TV) GridLayoutSizes.TV else GridLayoutSizes.MOBILE).toString())).let {
            if (it != null) {
                filmsInRow = it.toInt()
            }
        }
        var defq = prefs?.getString("defaultQuality", null)
        if (defq == "Авто") {
            defq = null
        }
        defaultQuality = defq
        defaultSort = prefs?.getString("defaultSort", "1")?.toInt()
        isSelectSubtitle = prefs?.getBoolean("isSelectSubtitles", true)
        // selectedPlayerPackage = prefs?.getString("selectedPlayerPackage", null)

        try {
            // Mozilla/5.0 (Linux; Android 12; Pixel 4a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.104 Mobile Safari/537.36
            useragent = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.71 Safari/537.36"
        } catch (e: Exception) {
            useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36"
            e.printStackTrace()
        }
    }

    fun setProvider(newProvider: String, context: Context, updateSettings: Boolean) {
        if (updateSettings) {
            val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(context)
            prefs?.edit()?.putString("ownProvider", newProvider)?.apply()
        }

        try {
            FileManager.writeFile(PROVIDER_FILE, newProvider, false, context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        provider = newProvider
    }
}