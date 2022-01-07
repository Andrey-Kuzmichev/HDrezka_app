package com.falcofemoralis.hdrezkaapp.models

import android.content.Context
import android.util.ArrayMap
import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.objects.UserData
import com.falcofemoralis.hdrezkaapp.utils.CookieStorage
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object UserModel {
    private const val USER_PAGE: String = "/user/"
    private const val LOGIN_AJAX: String = "/ajax/login/"
    private const val REGISTER_AJAX: String = "/engine/ajax/quick_register.php"

    fun getUserAvatarLink(): String? {
        val userId: String? = CookieStorage.getCookie(SettingsData.provider, "dle_user_id")
        val doc: Document = BaseModel.getJsoup(SettingsData.provider + USER_PAGE + userId)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .get()
        val str = doc.select("div.b-userprofile__avatar_holder img").attr("src")

        return if (str.contains("//static") && str.contains("http") && str.contains("noavatar")) {
            // has protocol, is noavatar
            null
        } else if (!str.contains("//static") && !str.contains("http") && str.contains("noavatar")) {
            // no protocol, is noavatar
            null
        } else if (str.contains("//static") && str.contains("http")) {
            // has protocol, no noavatar
            null
/*        } else if (!str.contains("//static") && !str.contains("http")) {
            // no protocol, no noavatar
            null*/
        } else if (str.contains("http") && str.contains("upload")) {
            // has protocol, user avatar
            str
        } else if (!str.contains("http") && str.contains("upload")) {
            // no protocol, user avatar
            SettingsData.provider + str
        } else {
            null
        }
    }

    fun login(name: String, password: String, context: Context) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["login_name"] = name
        data["login_password"] = password
        data["login_not_save"] = "0"

        val res: Connection.Response = BaseModel.getJsoup(SettingsData.provider + LOGIN_AJAX)
            .data(data)
            .method(Connection.Method.POST)
            .execute()

        val doc = res.parse()

        if (doc != null) {
            val bodyString: String = doc.select("body").text()
            val jsonObject = JSONObject(bodyString)

            val isSuccess: Boolean = jsonObject.getBoolean("success")
            if (isSuccess) {
                UserData.setCookies(res.cookie("dle_user_id"), res.cookie("dle_password"), res.cookie("PHPSESSID"), context, true)
            } else {
                val msg = jsonObject.getString("message")
                throw Exception(msg)
            }
        } else {
            throw HttpStatusException("failed to login", 400, SettingsData.provider)
        }
    }

    fun register(email: String, username: String, password: String, context: Context) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["data"] = "email=$email&prevent_autofill_name=&name=$username&prevent_autofill_password1=&password1=$password&rules=1&submit_reg=submit_reg&do=register"

        val res: Connection.Response = BaseModel.getJsoup(SettingsData.provider + REGISTER_AJAX)
            .data(data)
            .method(Connection.Method.POST)
            .execute()

        val doc = res.parse()

        val scriptTag = doc.select("script")
        if (scriptTag.size > 0) {
            val scriptValue = scriptTag[0].html()

            if ((scriptValue.contains("location") || scriptValue.isEmpty()) && res.hasCookie("dle_user_id") && res.hasCookie("dle_password")) {
                UserData.setCookies(res.cookie("dle_user_id"), res.cookie("dle_password"), null, context, true)
            } else {
                val toParse = scriptValue.replace("\$('#register-popup-errors').html('", "").replace("').show();", "")

                val parsedDoc = Jsoup.parse(toParse)
                val errorsEls = parsedDoc.select("li")
                val errorText = StringBuilder()
                for ((i, errorEl) in errorsEls.withIndex()) {
                    errorText.append(errorEl.text())
                    if (i != errorsEls.size - 1) {
                        errorText.append("\n\n")
                    }
                }
                throw Exception(errorText.toString())
            }
        }
    }
}