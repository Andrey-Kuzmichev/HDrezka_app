package com.falcofemoralis.hdrezkaapp.models

import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.objects.WatchLater
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

object WatchLaterModel {
    private const val MAIN_PAGE = "/continue/"

    fun getWatchLaterList(): ArrayList<WatchLater> {
        val document: Document = BaseModel.getJsoup(SettingsData.provider + MAIN_PAGE)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .get()

        val watchLaterList: ArrayList<WatchLater> = ArrayList()

        val els: Elements = document.select("div.b-videosaves__list_item")
        if (els.size > 0) {
            els.removeFirst()

            for (el in els) {
                val id = el.attr("id").replace("videosave-", "")
                val date = el.select("div.date").text()
                val a = el.select("div.title a")
                val name = "${a.text()} ${el.select("div.title small").text()}"
                val filmLInk = a.attr("href")
                val info = el.select("div.info")[0].ownText()
                val additionalInfo = el.select("div.info span.info-holder a").text()

                watchLaterList.add(WatchLater(id, date, filmLInk, name, info, "$info $additionalInfo"))
            }
        }

        return watchLaterList
    }

    fun removeItem(id: String) {
        BaseModel.getJsoup(SettingsData.provider + "/engine/ajax/cdn_saves_remove.php")
            .data("id", id)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .post()
    }
}