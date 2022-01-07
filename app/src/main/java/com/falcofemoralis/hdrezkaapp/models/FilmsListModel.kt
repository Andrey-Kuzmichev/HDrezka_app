package com.falcofemoralis.hdrezkaapp.models

import com.falcofemoralis.hdrezkaapp.constants.FilmType
import com.falcofemoralis.hdrezkaapp.objects.Film
import org.jsoup.nodes.Document

object FilmsListModel {
    const val FILMS = "div.b-content__inline_item"
    const val FILM_IMG = "div.b-content__inline_item-cover a img"
    const val SUB_INFO = "div.b-content__inline_item-cover a span.info"
    const val FILM_TITLE = "div.b-content__inline_item-link a"

    fun getFilmsFromPage(doc: Document): ArrayList<Film> {
        val films: ArrayList<Film> = ArrayList()

        for (el in doc.select(FILMS)) {
           // val film = Film(el.attr("data-id").toInt())
            val film = Film(el.attr("data-url"))

            if (film.filmLink.isNullOrEmpty()) {
                film.filmLink = el.select("div.b-content__inline_item-cover a").attr("href")

                if (film.filmLink.isNullOrEmpty()) {
                    film.filmLink = el.select("div.b-content__inline_item-link a").attr("href")
                }
            }
            film.title = el.select(FILM_TITLE).text()
            film.posterPath = el.select(FILM_IMG).attr("src")
            film.subInfo = el.select(SUB_INFO).text()

            val text = el.select("div.b-content__inline_item-link div").text()
            val separated = text.split(",")

            film.year = separated[0]
            film.countries = ArrayList()
            film.countries!!.add(separated[1].drop(1))

            if (separated.size > 2) {
                film.genres = ArrayList()
                film.genres!!.add(separated[2].drop(1))
            }

            film.ratingKP = el.select("i.b-category-bestrating").text()

            val catEl = el.select("span.cat")
            film.type = catEl.select("i.entity")[0].ownText()
            film.constFilmType =
                if (catEl.hasClass("films")) FilmType.FILM
                else if (catEl.hasClass("series")) FilmType.SERIES
                else if (catEl.hasClass("cartoons")) FilmType.MULTFILMS
                else if (catEl.hasClass("animation")) FilmType.ANIME
                else if (catEl.hasClass("show")) FilmType.TVSHOWS
                else FilmType.FILM

            films.add(film)
        }

        return films
    }
}