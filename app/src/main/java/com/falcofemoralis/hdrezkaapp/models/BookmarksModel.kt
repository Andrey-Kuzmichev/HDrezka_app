package com.falcofemoralis.hdrezkaapp.models

import android.util.ArrayMap
import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.objects.Bookmark
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

object BookmarksModel {
    private const val BOOKMARKS_PAGE = "/favorites/"
    private const val POST_URL = "/ajax/favorites/"

    fun getBookmarksList(): ArrayList<Bookmark> {
        val document: Document = BaseModel.getJsoup(SettingsData.provider + BOOKMARKS_PAGE)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .get()

        val bookmarks: ArrayList<Bookmark> = ArrayList()
        val tableEls: Elements = document.select("div.b-favorites_content__cats_list_item")
        for (el in tableEls) {
            val catId: String = el.attr("data-cat_id")
            val link: String = el.select("a.b-favorites_content__cats_list_link").attr("href")
            val name: String = el.select("span.name").text()
            val amount: Number = el.select("span.num-holder b").text().toInt()

            bookmarks.add(Bookmark(catId, link, name, amount))
        }

        return bookmarks

    }

    fun getFilmsFromBookmarkPage(link: String, page: Int, sort: String?, show: String?): ArrayList<Film> {
        var url = "${link}/page/${page}/"
        url += if (sort != null && sort.isNotEmpty()) {
            "?filter=${sort}"
        } else {
            "?filter=added"
        }
        if (show != null && show.isNotEmpty() && show != "0") {
            url += "&genre=${show}"
        }

        val doc: Document = BaseModel.getJsoup(url).header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider)).get()
        return FilmsListModel.getFilmsFromPage(doc)
    }

    fun postCatalog(name: String): Bookmark {
        val data: ArrayMap<String, String> = ArrayMap()
        data["name"] = name
        data["action"] = "add_cat"

        val result: Element? = BaseModel.getJsoup(SettingsData.provider + POST_URL)
            .data(data)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            val isSuccess: Boolean = jsonObject.getBoolean("success")
            if (isSuccess) {
                val id = jsonObject.getString("id")
                val catName = jsonObject.getString("name") + " (1)"
                return Bookmark(id, "", catName, 1)
            } else {
                throw HttpStatusException("failed to post catalog because: ${jsonObject.getString("message")}", 400, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to post catalog because body is empty", 404, SettingsData.provider)
        }
    }

    fun postBookmark(filmId: Int, catId: String) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["post_id"] = filmId.toString()
        data["cat_id"] = catId
        data["action"] = "add_post"

        BaseModel.getJsoup(SettingsData.provider + POST_URL)
            .data(data)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .post()
    }
}