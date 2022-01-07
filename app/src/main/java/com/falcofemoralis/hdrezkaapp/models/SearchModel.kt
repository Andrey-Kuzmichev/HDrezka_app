package com.falcofemoralis.hdrezkaapp.models

import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.constants.FilmType
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.net.URLEncoder

object SearchModel {
    private const val SEARCH_URL = "/engine/ajax/search.php"

    fun getFilmsListByQuery(text: String): ArrayList<Film> {
        val doc: Document = BaseModel.getJsoup(SettingsData.provider + SEARCH_URL).data("q", text).post()

        val films: ArrayList<Film> = ArrayList()
        val els: Elements = doc.select("li")

        for (el in els) {
            val link: String = el.select("a").attr("href")
            val film = Film(link)
            film.title = el.select("span.enty").text()
            film.ratingKP = el.select("span.rating i").text()
           // film.subInfo = el.select("a")[0].ownText()

            val defaultType = FilmType.FILM
            val als =  el.select("a")
            if(als.size > 0){
                val data = als[0].attr("href").replace("http://", "").replace("https://", "").split("/")
                if (data.size > 1) {
                    film.constFilmType = FilmModel.getConstTypeByName(data[1])
                    film.type = FilmModel.getTypeByName(data[1])
                } else {
                    film.constFilmType = defaultType
                }
            } else{
                film.constFilmType = defaultType
            }

            films.add(film)
        }

        return films
    }

    fun getFilmsFromSearchPage(query: String, page: Int): ArrayList<Film> {
        val doc: Document = BaseModel.getJsoup(SettingsData.provider + "/search/?do=search&subaction=search&q=${URLEncoder.encode(query, "UTF-8")}&page=$page")
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .get()

        return FilmsListModel.getFilmsFromPage(doc)
    }
}