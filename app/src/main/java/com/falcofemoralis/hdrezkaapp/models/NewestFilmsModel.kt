package com.falcofemoralis.hdrezkaapp.models

import android.util.ArrayMap
import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.constants.FilmType
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SeriesUpdateItem
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document

object NewestFilmsModel {
    private const val NEW = "/engine/ajax/get_newest_slider_content.php"
    val SORTS: ArrayList<String> = arrayListOf("newest_slider_content", "last", "popular", "soon", "watching")
    val TYPES: ArrayList<String> = arrayListOf("0", "1", "2", "3", "82") // all, films, serials, multfilms, anime

    fun getNewestFilms(page: Int, sort: String, type: String): ArrayList<Film> {
        var link = SettingsData.provider + "/page/$page/?filter=$sort"

        // fixed site bug
        if (type != "0") {
            link += "&genre=$type"
        }

        val doc: Document = BaseModel.getJsoup(link).get()
        return FilmsListModel.getFilmsFromPage(doc)
    }

    fun getNewFilms(type: String): ArrayList<Film> {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = type

        val result: Document? = BaseModel.getJsoup(SettingsData.provider + NEW).data(data).post()
        if (result != null) {
            return FilmsListModel.getFilmsFromPage(result)
        } else {
            throw HttpStatusException("failed to get new films", 404, SettingsData.provider)
        }
    }

    fun getSeriesUpdates(): LinkedHashMap<String, ArrayList<SeriesUpdateItem>> {
        val doc: Document = BaseModel.getJsoup(SettingsData.provider)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .get()

        val seriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>> = LinkedHashMap()

        val blocks = doc.select("div.b-seriesupdate__block")
        if (blocks.size > 0) {
            for (block in blocks) {
                var date = ""
                val dateItem = block.select("div.b-seriesupdate__block_date")
                if (dateItem != null) {
                    date = dateItem.text().replace(" развернуть", "")
                }

                val series: ArrayList<SeriesUpdateItem> = ArrayList()
                val seriesItems = block.select("li.b-seriesupdate__block_list_item")
                for (item in seriesItems) {
                    val title = item.select("div.cell a.b-seriesupdate__block_list_link").text()
                    val link = item.select("div.cell a.b-seriesupdate__block_list_link").attr("href")
                    val season = item.select("span.season").text()
                    val episodeItems = item.select("span.cell")
                    var episode = ""
                    if (episodeItems.size > 0) {
                        episode = episodeItems[0].ownText()
                    }
                    val voice = item.select("span.cell i").text()
                    val isUserWatch = item.hasClass("tracked")

                    if (link.isNotEmpty() && title.isNotEmpty()) {
                        series.add(SeriesUpdateItem(link, title, season, episode, voice, isUserWatch))
                    }
                }

                seriesUpdates[date] = series
            }
        }

        return seriesUpdates
    }
}