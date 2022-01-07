package com.falcofemoralis.hdrezkaapp.models

import android.util.ArrayMap
import android.util.Base64
import android.webkit.CookieManager
import com.falcofemoralis.hdrezkaapp.constants.FilmType
import com.falcofemoralis.hdrezkaapp.objects.*
import kotlinx.coroutines.*
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist
import org.jsoup.select.Elements
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


object FilmModel {
    private const val FILM_TITLE = "div.b-post__title h1"
    private const val FILM_POSTER = "div.b-sidecover a"
    private const val FILM_TABLE_INFO = "table.b-post__info tbody tr"
    private const val FILM_IMDB_RATING = "span.imdb span"
    private const val FILM_KP_RATING = "span.kp span"
    private const val FILM_WA_RATING = "span.wa span"

    private const val WATCH_ADD = "/engine/ajax/schedule_watched.php"
    private const val RATING_ADD = "/engine/ajax/rating.php"
    private const val GET_FILM_POST = "/engine/ajax/quick_content.php"
    private const val GET_STREAM_POST = "/ajax/get_cdn_series"
    private const val SEND_WATCH = "/ajax/send_save"
    private const val GET_TRAILER_VIDEO = "/engine/ajax/gettrailervideo.php"

    const val AWAITING_TEXT = "В ожидании"

    fun getMainData(film: Film): Film {
        try {
            if (film.filmId == null) {
                getMainDataByLink(film)
            } else {
                getMainDataById(film)
            }
        } catch (e: Exception) {
            throw e
        }

        film.hasMainData = true

        return film
    }

    private fun getMainDataById(film: Film): Film {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = film.filmId.toString()
        data["is_touch"] = "1"

        val doc: Document?
        try {
            doc = BaseModel.getJsoup(SettingsData.provider + GET_FILM_POST).data(data).post()
        } catch (e: Exception) {
            throw e
        }

        if (doc != null) {
            val titleEl = doc.select("div.b-content__bubble_title a")
            if (film.filmLink.isNullOrEmpty()) {
                film.filmLink = titleEl.attr("href")
            }
            film.type = getTypeByName(doc.select("i.entity").text())
            film.title = titleEl.text()
            film.ratingIMDB = doc.select("span.imdb b").text()
            film.ratingKP = doc.select("span.kp b").text()
            //    film.ratingWA = doc.select("span.wa b").text()
            val genres: ArrayList<String> = ArrayList()
            val genresEls = doc.select("div.b-content__bubble_text a")
            for (el in genresEls) {
                genres.add(el.text())
            }
            film.genres = genres
        } else {
            throw HttpStatusException("failed to get film", 400, SettingsData.provider)
        }

        return film
    }

    private fun getMainDataByLink(film: Film): Film {
        val filmPage: Document = BaseModel.getJsoup(film.filmLink).get()

        val type: String? = film.filmLink?.split("/")?.get(3)
        if (type != null) {
            film.type = getTypeByName(type)
            film.constFilmType = getConstTypeByName(type)
        }
        film.title = filmPage.select(FILM_TITLE).text()
        film.posterPath = filmPage.select("div.b-sidecover a img").attr("src")

        parseTable(filmPage, film)

        // Parse info table
        val table: Elements = filmPage.select(FILM_TABLE_INFO)
        for (tr in table) {
            val td: Elements = tr.select("td")
            if (td.size > 0) {
                val h2Els: Elements = td[0].select("h2")
                if (h2Els.size > 0) {
                    val h2: Element = h2Els[0]

                    when (h2.text()) {
                        "Рейтинги" -> {
                            film.ratingIMDB = td[1].select(FILM_IMDB_RATING).text()
                            film.ratingKP = td[1].select(FILM_KP_RATING).text()
                            //  film.ratingWA = td[1].select(FILM_WA_RATING).text()
                        }
                    }
                }
            }
        }

        return film
    }

    fun getTypeByName(name: String): String {
        return when (name) {
            "series" -> "Сериал"
            "cartoons" -> "Мультфильм"
            "films" -> "Фильм"
            "animation" -> "Аниме"
            else -> name
        }
    }

    fun getConstTypeByName(name: String): FilmType {
        return when (name) {
            "series" -> FilmType.SERIES
            "cartoons" -> FilmType.MULTFILMS
            "films" -> FilmType.FILM
            "animation" -> FilmType.ANIME
            "show" -> FilmType.TVSHOWS
            else -> FilmType.FILM
        }
    }

    private fun parseTable(document: Document, film: Film) {
        val table: Elements = document.select(FILM_TABLE_INFO)
        // Parse info table
        for (tr in table) {
            val td: Elements = tr.select("td")
            if (td.size > 0) {
                val h2Els: Elements = td[0].select("h2")
                if (h2Els.size > 0) {
                    val h2: Element = h2Els[0]

                    when (h2.text()) {
                        "Рейтинги" -> {
                            film.ratingWA = td[1].select(FILM_WA_RATING).text()
                        }
                        "Дата выхода" -> {
                            film.date = td[1].ownText()
                            film.year = td[1].select("a").text()
                        }
                        "Страна" -> {
                            val countries: ArrayList<String> = ArrayList()
                            for (el in td[1].select("a")) {
                                countries.add(el.text())
                            }
                            film.countries = countries
                        }
                    }
                }
            }
        }
    }

    fun getAdditionalData(film: Film): Film {
        val document: Document = BaseModel.getJsoup(film.filmLink)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider) + "; allowed_comments=1;")
            .get()
        film.origTitle = document.select("div.b-post__origtitle").text()
        film.description = document.select("div.b-post__description_text").text()
        film.votesIMDB = document.select("span.imdb i").text()
        film.votesKP = document.select("span.kp i").text()
        film.votesWA = document.select("span.wa i").text()
        film.runtime = document.select("td[itemprop=duration]").text()
        film.filmId = document.select("div.b-userset__fav_holder").attr("data-post_id").toInt()

        parseTable(document, film)

        val hrRatingEl = document.select("div.b-post__rating")
        film.ratingHR = hrRatingEl.select("span.num").text()
        film.votesHR = hrRatingEl.select("span.votes span").text()
        film.isHRratingActive = hrRatingEl.select("div.b-post__rating_wrapper").isNullOrEmpty()

        val posterElement: Element = document.select(FILM_POSTER)[0]
        film.fullSizePosterPath = posterElement.attr("href")
        film.posterPath = posterElement.select("img").attr("src")

        val actors: ArrayList<Actor> = ArrayList()
        val directors: ArrayList<Actor> = ArrayList()
        val personsElements: Elements = document.select("div.persons-list-holder")
        for (el in personsElements) {
            val els: Elements = el.select("span.item")

            if (el.select("span.inline h2").text() == "В ролях актеры") {
                for (actorElement in els) {
                    val pEl = actorElement.select("span.person-name-item")
                    val id: String = pEl.attr("data-id")
                    if (id.isNotEmpty()) {
                        actors.add(Actor(id.toInt(), pEl.attr("data-pid").toInt()))
                    }
                }
            } else {
                for (directorElement in els) {
                    var name = directorElement.select("span a span").text()
                    val idText = directorElement.select("span").attr("data-id")
                    val pidText = directorElement.select("span").attr("data-pid")

                    if (name.isEmpty()) {
                        name = directorElement.text()
                    }
                    if (idText.isNotEmpty() && pidText.isNotEmpty()) {
                        val actor = Actor(idText.toInt(), pidText.toInt())
                        actor.name = name
                        directors.add(actor)
                    }
                }
            }
        }
        film.directors = directors
        film.actors = actors

        val seriesSchedule: ArrayList<Pair<String, ArrayList<Schedule>>> = ArrayList()
        val seasonsElements: Elements = document.select("div.b-post__schedule div.b-post__schedule_block")
        for (block in seasonsElements) {
            var header: String = block.select("div.b-post__schedule_block_title div.title").text()
            val toReplace = "${film.title} - даты выхода серий "

            if (header.contains(toReplace)) {
                header = header.replace("${film.title} - даты выхода серий ", "").dropLast(1)
            }

            val listSchedule: ArrayList<Schedule> = ArrayList()
            val list: Elements = block.select("div.b-post__schedule_list table tbody tr")
            for (el in list) {
                if (el.select("td").hasClass("load-more")) {
                    continue
                }

                val episode: String = el.select("td.td-1").text()
                val name: String = el.select("td.td-2 b").text()
                val watch: Element? = el.selectFirst("td.td-3 i")
                var isWatched = false
                var watchId: Int? = null

                if (watch != null) {
                    isWatched = watch.hasClass("watched")
                    watchId = watch.attr("data-id").toInt()
                }

                val date: String = el.select("td.td-4").text()
                val nextEpisodeIn: String = el.select("td.td-5").text()

                val schedule = Schedule(episode, name, date, isWatched)
                if (nextEpisodeIn.isNotEmpty()) {
                    schedule.nextEpisodeIn = nextEpisodeIn
                }

                schedule.watchId = watchId

                listSchedule.add(schedule)
            }

            seriesSchedule.add(Pair(header, listSchedule))
        }
        film.seriesSchedule = seriesSchedule

        val collectionFilms: ArrayList<Film> = ArrayList()
        val collectionElements: Elements = document.select("div.b-post__partcontent_item")
        for (el in collectionElements) {
            val subFilm: Film

            val a = el.select("div.title a")
            if (a.size > 0) {
                subFilm = Film(a.attr("href"))
                subFilm.title = a.text()
            } else {
                subFilm = Film()
                subFilm.title = el.select("div.title").text()
            }

            subFilm.year = el.select("div.year").text()
            subFilm.ratingKP = el.select("div.rating i").text()
            collectionFilms.add(subFilm)
        }
        film.collection = collectionFilms

        val relatedFilms: ArrayList<Film> = ArrayList()
        val relatedElements = document.select("div.b-content__inline_item")
        for (el in relatedElements) {
            val id = el.attr("data-id")
            val cover: String = el.select("div.b-content__inline_item-cover a img").attr("src")
            val a = el.select("div.b-content__inline_item-link a")
            val link: String = a.attr("href")
            val title: String = a.text()
            val misc: String = el.select("div.misc").text()

            val relatedFilm = Film(id.toInt())
            relatedFilm.filmLink = link
            relatedFilm.posterPath = cover
            relatedFilm.title = title
            relatedFilm.relatedMisc = misc
            relatedFilms.add(relatedFilm)
        }
        film.related = relatedFilms

        val bookmarks: ArrayList<Bookmark> = ArrayList()
        val bookmarkElements = document.select("div.hd-label-row")
        for (el in bookmarkElements) {
            val name = "${el.select("label")[0].ownText()} (${el.select("b").text()})"
            val isChecked = el.select("input").attr("checked") == "checked"
            val catId = el.select("input").attr("value")

            val bookmark = Bookmark(catId, "", name, 0)
            bookmark.isChecked = isChecked
            bookmarks.add(bookmark)
        }
        film.bookmarks = bookmarks

        // get streams
        film.isMovieTranslation = document.select("meta[property=og:type]").first().attr("content").equals("video.movie")
        val filmTranslations: ArrayList<Voice> = ArrayList()
        val els = document.select(".b-translator__item")
        for (el in els) {
            val country = el.select("img").attr("alt")
            var name = el.attr("title")
            if (country != null && country.isNotEmpty()) {
                name += "($country)"
            }
            val voice = Voice(name, el.attr("data-translator_id"))
            voice.isAds = el.attr("data-ads")
            voice.isCamrip = el.attr("data-camrip")
            voice.isDirector = el.attr("data-director")
            filmTranslations.add(voice)

            if (el.hasClass("active")) {
                film.lastVoiceId = voice.id
            }
        }

        // no film translations
        val stringedDoc = document.toString()
        try {
            if (filmTranslations.size == 0) {

                if (film.isMovieTranslation!!) {
                    val index = stringedDoc.indexOf("initCDNMoviesEvents")
                    val subString = stringedDoc.substring(stringedDoc.indexOf("{\"id\"", index), stringedDoc.indexOf("});", index) + 1)
                    val jsonObject = JSONObject(subString)
                    val trans = Voice(parseSteams(jsonObject.getString("streams")))
                    trans.subtitles = parseSubtitles(jsonObject.getString("subtitle"))
                    getThumbnails(jsonObject.getString("thumbnails"), trans)
                    filmTranslations.add(trans)
                } else {
                    val startIndex = stringedDoc.indexOf("initCDNSeriesEvents")
                    var endIndex = stringedDoc.indexOf("{\"id\"", startIndex)
                    var startObjIndex = endIndex
                    if (endIndex == -1) {
                        endIndex = stringedDoc.indexOf("{\"url\"", startIndex)
                        startObjIndex = endIndex
                    }
                    var endObjIndex = stringedDoc.indexOf("});")

                    val subString = stringedDoc.substring(startIndex, endIndex)
                    val trans = Voice(subString.split(",")[1], parseSeasons(document))
                    val jsonObject = JSONObject(stringedDoc.substring(startObjIndex, endObjIndex + 1))
                    trans.streams = parseSteams(jsonObject.getString("streams"))
                    if (jsonObject.has("subtitle")) {
                        trans.subtitles = parseSubtitles(jsonObject.getString("subtitle"))
                    }
                    try {
                        getThumbnails(jsonObject.getString("thumbnails"), trans)
                    } catch (e: Exception) {
                        // ignore
                    }
                    filmTranslations.add(trans)
                }
            }
        } catch (e: Exception) {
            film.isPendingRelease = true
        }

        val episodes = document.select("li.b-simple_episode__item")
        for (episode in episodes) {
            if (episode.hasClass("active")) {
                film.lastSeason = episode.attr("data-season_id")
                film.lastEpisode = episode.attr("data-episode_id")
                break
            }
        }
        film.translations = filmTranslations

        if (film.isMovieTranslation == false && SettingsData.autoPlayNextEpisode == true) {
            try {
                val firstIndex = stringedDoc.indexOf("\$(function () { sof.tv.initCDNSeriesEvents")
                val secondIndex = stringedDoc.indexOf("; \$(function ()")
                val autoswitch = stringedDoc.substring(firstIndex, secondIndex)
                film.autoswitch = autoswitch
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            film.filmId?.let { film.youtubeLink = getTrailerVideo(it) }
        } catch (e: Exception) {
            // ignore
        }

        film.hasAdditionalData = true

        return film
    }

    data class Counter(private var c: Int) {
        var count = c
    }

    fun getFilmsData(films: ArrayList<Film>, filmsPerPage: Int, callback: (ArrayList<Film>) -> Unit) {
        val filmsToLoad: ArrayList<Film> = ArrayList()
        try {
            for ((index, film) in (films.clone() as ArrayList<Film>).withIndex()) {
                filmsToLoad.add(film)
                films.removeAt(0)

                if (index == filmsPerPage - 1) {
                    break
                }
            }
        } catch (e: Error) {
            e.printStackTrace()
        }

        val counter = Counter(0)
        val loadedFilms = arrayOfNulls<Film?>(filmsPerPage)
        for ((index, film) in filmsToLoad.withIndex()) {
            startFilmLoad(counter, loadedFilms, filmsToLoad, index, film, callback)
        }
    }

    private fun startFilmLoad(counter: Counter, loadedFilms: Array<Film?>, filmsToLoad: ArrayList<Film>, index: Int, film: Film, callback: (ArrayList<Film>) -> Unit) {
        GlobalScope.launch {
            try {
                loadedFilms[index] = getMainData(film)

                counter.count++

                if (counter.count >= filmsToLoad.size) {
                    val list: ArrayList<Film> = ArrayList()
                    for (item in loadedFilms) {
                        if (item != null) {
                            list.add(item)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        callback(list)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                delay(50)
                startFilmLoad(counter, loadedFilms, filmsToLoad, index, film, callback)
            }
        }
    }

    fun getFilmPosterByLink(filmLink: String): String? {
        val filmPage: Document = BaseModel.getJsoup(filmLink).get()
        val posterElements = filmPage.select(FILM_POSTER)
        return if (posterElements.size > 0) {
            val posterElement: Element = posterElements[0]
            posterElement.select("img").attr("src")
        } else {
            null
        }
    }

    fun postWatch(watchId: Int) {
        val result: Element? = BaseModel.getJsoup(SettingsData.provider + WATCH_ADD)
            .data("id", watchId.toString())
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider) + "; allowed_comments=1;")
            .post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            val isSuccess: Boolean = jsonObject.getBoolean("success")
            if (!isSuccess) {
                throw HttpStatusException("failed to post watch because: ${jsonObject.getString("message")}", 400, SettingsData.provider)
            }
        }
    }

    fun postRating(film: Film, rating: Float) {
        val result: String = BaseModel.getJsoup(SettingsData.provider + RATING_ADD + "?news_id=${film.filmId}&go_rate=${rating}&skin=hdrezka")
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .execute()
            .body()

        val jsonObject = JSONObject(result)
        val isSuccess: Boolean = jsonObject.getBoolean("success")

        if (isSuccess) {
            film.ratingHR = jsonObject.getString("num")
            film.votesHR = jsonObject.getString("votes")
            film.isHRratingActive = !film.isHRratingActive!!
        } else {
            throw HttpStatusException("failed to post comment", 400, SettingsData.provider)
        }
    }

    fun getStreamsByTranslationId(filmId: Number, translation: Voice) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = filmId.toString()
        data["translator_id"] = translation.id
        data["is_camrip"] = translation.isCamrip
        data["is_ads"] = translation.isAds
        data["is_director"] = translation.isDirector
        data["action"] = "get_movie"

        val unixTime = System.currentTimeMillis()
        val result: Document? = BaseModel.getJsoup(SettingsData.provider + GET_STREAM_POST + "/?t=$unixTime").data(data).post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            if (jsonObject.getBoolean("success")) {
                translation.streams = parseSteams(jsonObject.getString("url"))
                translation.subtitles = parseSubtitles(jsonObject.getString("subtitle"))
                val thumbnailsUrl = jsonObject.getString("thumbnails")
                if (thumbnailsUrl.isNotEmpty() && thumbnailsUrl != "false") {
                    getThumbnails(thumbnailsUrl, translation)
                }
            } else {
                throw HttpStatusException("failed to get stream in object", 405, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to get stream", 405, SettingsData.provider)
        }
    }

    fun getThumbnails(thumbnailsUrl: String, translation: Voice) {
        if (thumbnailsUrl != "false") {
            val result: Document? = BaseModel.getJsoup(SettingsData.provider + thumbnailsUrl).post()

            if (result != null) {
                val responseText: String = result.select("body").text()

                if (responseText.isNotEmpty()) {
                    translation.thumbnails = ArrayList()

                    val rows = responseText.split(" ")
                    if (rows[0].indexOf("WEBVTT") > -1) {
                        for ((i, row) in rows.withIndex()) {
                            if (row.indexOf("-->") > -1) {
                                val t1 = timeVtt(rows[i - 1])
                                val t2 = timeVtt(rows[i + 1])

                                var x_url = ""
                                if (i < rows.size - 1) {
                                    x_url = rows[i + 2]
                                    if (x_url.indexOf("http") != 0 && x_url.indexOf("//") != 0) {
                                        x_url = thumbnailsUrl.substring(0, thumbnailsUrl.lastIndexOf("/") + if (x_url.indexOf("/") == 0) 0 else 1) + x_url
                                    }
                                }

                                if (x_url.indexOf("xywh") > 0) {
                                    val xy = x_url.substring(x_url.indexOf("xywh") + 5)
                                    val xywh = xy.split(",")

                                    translation.thumbnails!!.add(Thumbnail(t1, t2, x_url, xywh[0].toInt(), xywh[1].toInt(), xywh[2].toInt(), xywh[3].toInt()))
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    fun br2nl(html: String?): String? {
        if (html == null) return html
        val document = Jsoup.parse(html)
        document.outputSettings(Document.OutputSettings().prettyPrint(false)) //makes html() preserve linebreaks and spacing
        document.select("br").append("\\n")
        document.select("p").prepend("\\n\\n")
        val s = document.html().replace("\\\\n".toRegex(), "\n")
        return Jsoup.clean(s, "", Whitelist.none(), Document.OutputSettings().prettyPrint(false))
    }

    private fun timeVtt(srt: String): Float {
        val tmp: ArrayList<String> = ArrayList()

        for (s in srt.split(":")) {
            tmp.add(s)
        }

        var out: Float = 0f
        if (tmp.size == 2) {
            tmp.add(0, "00")
        }

        if (tmp[0] != "00") {
            out += (tmp[0].toFloat() * 3600)
        }

        if (tmp[1] != "00") {
            out += tmp[1].toFloat() * 60
        }

        out += tmp[2].substring(0, 2).toFloat() * 1
        out += tmp[2].substring(2).toFloat() * 1

        return out
    }

    fun parseSteams(streams: String?): ArrayList<Stream> {
        val parsedStreams: ArrayList<Stream> = ArrayList()

        if (streams != null && streams.isNotEmpty()) {
            val decodedStreams = decodeUrl(streams)
            val split: Array<String> = decodedStreams.split(",")?.toTypedArray()

            for (str in split) {
                try {
                    if (str.contains(" or ")) {
                        parsedStreams.add(Stream(str.split(" or ").toTypedArray()[1], str.substring(1, str.indexOf("]"))))
                    } else {
                        parsedStreams.add(Stream(str.substring(str.indexOf("]") + 1), str.substring(1, str.indexOf("]"))))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return parsedStreams
    }

    private fun decodeUrl(str: String): String {
        return try {
            if (!str.startsWith("#h")) {
                return str
            }
            var replace = str.replace("#h", "")
            var i = 0
            while (i < 20 && replace.contains("//_//")) {
                val indexOf = replace.indexOf("//_//")
                if (indexOf > -1) {
                    replace = replace.replace(replace.substring(indexOf, indexOf + 21), "")
                }
                i++
            }
            String(Base64.decode(replace, 0))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            str
        }
    }


    private fun decodeStreams(str: String): String {
        val bk = arrayOf("$$#!!@#!@##", "^^^!@##!!##", "####^!!##!@@", "@@@@@!##!^^^", "$$!!@$$@^!@#$$@")
        val file3_separator = "//_//"
        var a = str.removeRange(0, 2)

        for (i in 4 downTo -1 + 1) {
            if (bk[i] !== "") {
                a = a.replace(file3_separator + decoder(bk[i]), "")
            }
        }
        //WzM2MHBdaHR0cDovL3N0cmVhbS52b2lkYm9vc3QuY2MvNC82LzAvOC83LzQvM2Y3NWVmNmI4MTkzOTY0NzdkNzQ0NDI1ZWY5NzQ1YTY6MjAyMjAxMDYxNDozNWU3YWRkYi04NTgwLTQzYmMtYjkwNy00YWZlNzk2ZjQ4YWQveDhweXEubXA0OmhsczptYW5pZmVzdC5tM3U4IG9yIGh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzViNTVlZjI0ODk5NjExZDMyNTJlZGIxOGEzNTYwZTc3OjIwMjIwMTA2MTQ6MzVlN2FkZGItODU4MC00M2JjLWI5MDctNGFmZTc5NmY0OGFkLzQvNi8wLzgvNy80L3g4cHlxLm1wNCxbNDgwcF1odHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy80LzYvMC84LzcvNC8zZjc1ZWY2YjgxOTM5NjQ3N2Q3NDQ0MjVlZjk3NDVhNjoyMDIyMDEwNjE0OjM1ZTdhZGRiLTg1ODAtNDNiYy1iOTA3LTRhZmU3OTZmNDhhZC9ibWE4MC5tcDQ6aGxzOm1hbmlmZXN0Lm0zdTggb3IgaHR0cDovL3N0cmVhbS52b2lkYm9vc3QuY2MvOGZlYTI4ZDFkMTc2MDFlZDAwYWZjNjU4YmUxOGE0OTI6MjAyMjAxMDYxNDozNWU3YWRkYi04NTgwLTQzYmMtYjkwNy00YWZlNzk2ZjQ4YWQvNC82LzAvOC83LzQvYm1hODAubXA0LFs3MjBwXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzNmNzVlZjZiODE5Mzk2NDc3ZDc0NDQyNWVmOTc0NWE2OjIwMjIwMTA2MTQ6MzVlN2FkZGItODU4MC00M2JjLWI5MDctNGFmZTc5NmY0OGFkLzBiejc3Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy8yODMyOTY2OWY4MGNkYmMxY2E0MTcxNWI3Mjg1YzEwZToyMDIyMDEwNjE0OjM1ZTdhZGRiLTg1ODAtNDNiYy1iOTA3LTRhZmU3OTZmNDhhZC80LzYvMC84LzcvNC8wYno3Ny5tcDQsWzEwODBwXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzNmNzVlZjZiODE5Mzk2NDc3ZDc0NDQyNWVmOTc0NWE2OjIwMjIwMTA2MTQ6MzVlN2FkZGItODU4MC00M2JjLWI5MDctNGFmZTc5NmY0OGFkL3llc2s2Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy9kYTIzNmJkNGVhMjgxODUwNmM4ZGVlMzhjMGQ2ZGQxMToyMDIyMDEwNjE0OjM1ZTdhZGRiLTg1ODAtNDNiYy1iOTA3LTRhZmU3OTZmNDhhZC80LzYvMC84LzcvNC95ZXNrNi5tcDQsWzEwODBwIFVsdHJhXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzNmNzVlZjZiODE5Mzk2NDc3ZDc0NDQyNWVmOTc0NWE2OjIwMjIwMTA2MTQ6MzVlN2FkZGItODU4MC00M2JjLWI5MDctNGFmZTc5NmY0OGFkL3JpNDV1Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy8xMjUyNTg3MmRhNjI1YjE3NTU3NDhlZWM5OTk1MGMyYjoyMDIyMDEwNjE0OjM1ZTdhZGRiLTg1ODAtNDNiYy1iOTA3LTRhZmU3OTZmNDhhZC80LzYvMC84LzcvNC9yaTQ1dS5tcDQ="
        //WzM2MHBdaHR0cDovL3N0cmVhbS52b2lkYm9vc3QuY2MvNC82LzAvOC83LzQvMjEyOGU2ZGRlYTQ3MDMxNzhhM2YzZTY3ZWEwMjViNGE6MjAyMjAxMDYxNDpWR2RXWlhodlJ6RjJRbXd4ZWpSNmVFVTBUamxSYmxoTWNtWnlTblU1UzNKdE5FcFJlVzQyUmk4cmJ6Y3lSM2h0ZGt0TVpuSnBSSGRuVURWRVNubHJTelJZV1hsNVlVRmtRM2hFUVhsMlRHTkJlbkZyY2tFOVBRPT0veDhweXEubXA0OmhsczptYW5pZmVzdC5tM3U4IG9yIGh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQ0NDlmNDI0NzIzZmU4Nzg4Y2Q5MjQxZTlmNzNlODQ4OjIwMjIwMTA2MTQ6VkdkV1pYaHZSekYyUW13eGVqUjZlRVUwVGpsUmJsaE1jbVp5U25VNVMzSnRORXB//_//QEBAQEAhIyMhXl5eSZVc0MlJpOHJiemN5UjNodGRrdE1abkpwUkhkblVEVkVTbmxyU3pSWVdYbDVZVUZrUTNoRVFYbDJUR05CZW5GcmNrRTlQUT09LzQvNi8wLzgvNy80L3g4cHlxLm1wNCxbNDgwcF1odHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy80LzYvMC84LzcvNC8yMTI4ZTZkZGVhNDcwMzE3OGEzZjNlNjdlYTAyNWI0YToyMDIyMDEwNjE0OlZHZFdaWGh2UnpGMlFtd3hlalI2ZUVVMFRqbFJibGhNY21aeVNuVTVTM0p0TkVwUmVXNDJSaThyYnpjeVIzaHRka3RNWm5KcFJIZG5VRFZFU25sclN6UllXWGw1WVVGa1EzaEVRWGwyVEdOQmVuRnJja0U5UFE9PS9ibWE4MC5tcDQ6aGxzOm1hbmlmZXN0Lm0zdTggb3IgaHR0cDovL3N0cmVhbS52b2lkYm9vc3QuY2MvZTQ1Y2U3NGY1MDczNGFjOTVkMTUwNjQyMmE1ZTg3ZGQ6MjAyMjAxMDYxNDpWR2RXWlhodlJ6RjJRbXd4ZWpSNmVFVTBUamxSYmxoTWNtWnlTblU1UzNKdE5FcFJlVzQyUmk4cmJ6Y3lSM2h0ZGt0TVpuSnBSSGRuVURWRVNubHJTelJZV1hsNVlVRmtRM2hFUVhsMlRHTkJlbkZyY2tFOVBRPT0vNC82LzAvOC83LzQvYm1//_//Xl5eIUAjIyEhIyM=hODAubXA0LFs3MjBwXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzIxMjhlNmRkZWE0NzAzMTc4YTNmM2U2N2VhMDI1YjRhOjIwMjIwMTA2MTQ6VkdkV1pYaHZSekYyUW13eGVqUjZlRVUwVGpsUmJsaE1jbVp5U25VNVMzSnRORXBSZVc0MlJpOHJiemN5UjNodGRrdE1abkpwUkhkblVEVkVTbmxyU3pSWVdYbDVZVUZrUTNoRVFYbDJUR05CZW5GcmNrRTlQUT09LzBiejc3Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy9hZWNm//_//IyMjI14hISMjIUBANGRjODUwMmI5NzQ5OTlmMjQxN2NhOGRjOWU5ZDoyMDIyMDEwNjE0OlZHZFdaWGh2UnpGMlFtd3hlalI2ZUVVMFRq//_//JCQhIUAkJEBeIUAjJCRAbFJibGhNY21aeVNuVTVTM0p0TkVwUmVXNDJSaThyYnpjeVIzaHRka3RNWm5KcFJIZG5VRFZFU25sclN6UllXWGw1WVVGa1EzaEVRWGwyVEdOQmVuRnJja0U5UFE9PS80LzYvMC84LzcvNC8wYno3Ny5tcDQsWzEwODBwXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzIxMjhlNmRkZWE0NzAzMTc4YTNmM2U2N2VhMD//_//JCQjISFAIyFAIyM=I1YjRhOjIwMjIwMTA2MTQ6VkdkV1pYaHZSekYyUW13eGVqUjZlRVUwVGpsUmJsaE1jbVp5U25VNVMzSnRORXBSZVc0MlJpOHJiemN5UjNodGRrdE1abkpwUkhkblVEVkVTbmxyU3pSWVdYbDVZVUZrUTNoRVFYbDJUR05CZW5GcmNrRTlQUT09L3llc2s2Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy81ZTE2MzMyMDIzODhlZjBmYWFlMTIwNWJjZWNlNWM1MDoyMDIyMDEwNjE0OlZHZFdaWGh2UnpGMlFtd3hlalI2ZUVVMFRqbFJibGhNY21aeVNuVTVTM0p0TkVwUmVXNDJSaThyYnpjeVIzaHRka3RNWm5KcFJIZG5VRFZFU25sclN6UllXWGw1WVVGa1EzaEVRWGwyVEdOQmVuRnJja0U5UFE9PS80LzYvMC84LzcvNC95ZXNrNi5tcDQsWzEwODBwIFVsdHJhXWh0dHA6Ly9zdHJlYW0udm9pZGJvb3N0LmNjLzQvNi8wLzgvNy80LzIxMjhlNmRkZWE0NzAzMTc4YTNmM2U2N2VhMDI1YjRhOjIwMjIwMTA2MTQ6VkdkV1pYaHZSekYyUW13eGVqUjZlRVUwVGpsUmJsaE1jbVp5U25VNVMzSnRORXBSZVc0MlJpOHJiemN5UjNodGRrdE1abkpwUkhkblVEVkVTbmxyU3pSWVdYbDVZVUZrUTNoRVFYbDJUR05CZW5GcmNrRTlQUT09L3JpNDV1Lm1wNDpobHM6bWFuaWZlc3QubTN1OCBvciBodHRwOi8vc3RyZWFtLnZvaWRib29zdC5jYy8xNTI3OTEyZGY1N2U1MDhhMmJkMTZjNzhlYTMyMTMzMToyMDIyMDEwNjE0OlZHZFdaWGh2UnpGMlFtd3hlalI2ZUVVMFRqbFJibGhNY21aeVNuVTVTM0p0TkVwUmVXNDJSaThyYnpjeVIzaHRka3RNWm5KcFJIZG5VRFZFU25sclN6UllXWGw1WVVGa1EzaEVRWGwyVEdOQmVuRnJja0U5UFE9PS80LzYvMC84LzcvNC9yaTQ1dS5tcDQ=
/*       var decoded = ""
        for (s in decodedStr.split("")) {
           for(c in s){
               decoded += "%" + ("00" + c.code).dropLast(2)
           }
        }
        decoded = URLDecoder.decode(decoded, "UTF-8")*/

        return String(Base64.decode(a, 0), StandardCharsets.UTF_8);
    }

    private fun decoder(s: String): String {
      //  val n = URLEncoder.encode(s)

        // 24 24 24 40 ... 5E
 /*       fun toSolidBytes(mr: MatchResult): CharSequence {
            return Integer.parseInt("0x${mr.value}").toChar().toString()
        }
        n.replace(Regex("/%([0-9A-F]{2})/g"), ::toSolidBytes)*/

        val t = String(Base64.encode(s.toByteArray(), 0), StandardCharsets.UTF_8)
        return t
    }

    fun getSeasons(filmId: Number, translation: Voice): Voice {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = filmId.toString()
        data["translator_id"] = translation.id
        data["action"] = "get_episodes"

        val unixTime = System.currentTimeMillis()
        val result: Document? = BaseModel.getJsoup(SettingsData.provider + GET_STREAM_POST + "/?t=$unixTime").data(data).post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            if (jsonObject.getBoolean("success")) {
                translation.seasons = parseSeasons(Jsoup.parse(jsonObject.getString("episodes")))
                return translation
            } else {
                throw HttpStatusException("failed to get seasons in object", 405, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to get seasons", 405, SettingsData.provider)
        }
    }

    private fun parseSeasons(document: Document): LinkedHashMap<String, ArrayList<String>> {
        val seasonList: LinkedHashMap<String, ArrayList<String>> = LinkedHashMap()
        val seasons = document.select("ul.b-simple_episodes__list")

        for ((i, season) in seasons.withIndex()) {
            val n = season.attr("id").replace("simple-episodes-list-", "")
            val episodesList: ArrayList<String> = ArrayList()
            val episodes = season.select("li.b-simple_episode__item")
            for (episode in episodes) {
                episodesList.add(episode.attr("data-episode_id"))
            }

            seasonList[n] = episodesList
        }

        return seasonList
    }

    fun getStreamsByEpisodeId(translation: Voice, filmId: Int, season: String, episode: String) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = filmId.toString()
        data["translator_id"] = translation.id
        data["season"] = season
        data["episode"] = episode
        data["action"] = "get_stream"

        val unixTime = System.currentTimeMillis()
        val result: Document? = BaseModel.getJsoup(SettingsData.provider + GET_STREAM_POST + "/?t=$unixTime").data(data).post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            if (jsonObject.getBoolean("success")) {
                translation.subtitles = parseSubtitles(jsonObject.getString("subtitle"))
                translation.streams = parseSteams(jsonObject.getString("url"))
                val thumbnailsUrl = jsonObject.getString("thumbnails")
                if (thumbnailsUrl.isNotEmpty()) {
                    getThumbnails(thumbnailsUrl, translation)
                }
            } else {
                throw HttpStatusException("failed to get stream in object", 405, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to get stream", 405, SettingsData.provider)
        }
    }

    private fun parseSubtitles(subtitles: String?): ArrayList<Subtitle>? {
        if (subtitles != null && subtitles.isNotEmpty() && subtitles != "false") {
            val parsedSubtitles: ArrayList<Subtitle> = ArrayList()

            val split: Array<String> = subtitles.split(",").toTypedArray()
            for (str in split) {
                if (str.contains(" or ")) {
                    parsedSubtitles.add(Subtitle(str.split(" or ").toTypedArray()[1], str.substring(1, str.indexOf("]"))))
                } else {
                    parsedSubtitles.add(Subtitle(str.substring(str.indexOf("]") + 1), str.substring(1, str.indexOf("]"))))
                }
            }

            return parsedSubtitles
        } else {
            return null
        }
    }

    fun saveWatch(filmId: Int, translation: Voice) {
        val data: ArrayMap<String, String> = ArrayMap()
        data["post_id"] = filmId.toString()
        data["translator_id"] = translation.id.toString()
        data["season"] = translation.selectedEpisode?.first ?: "0"
        data["episode"] = translation.selectedEpisode?.second ?: "0"
        data["current_time"] = "1"
        //data["duration"] = "1"

        val unixTime = System.currentTimeMillis()
        val result: Document? = BaseModel.getJsoup(SettingsData.provider + SEND_WATCH + "/?t=$unixTime")
            .data(data)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            if (!jsonObject.getBoolean("success")) {
                throw HttpStatusException("failed to save watch", 400, SettingsData.provider)
            }
        } else {
            throw HttpStatusException("failed to save watch", 400, SettingsData.provider)
        }
    }

    fun getTrailerVideo(filmId: Int): String? {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = filmId.toString()

        val result: Document? = BaseModel.getJsoup(SettingsData.provider + GET_TRAILER_VIDEO)
            .data(data)
            .header("Cookie", CookieManager.getInstance().getCookie(SettingsData.provider))
            .post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            val jsonObject = JSONObject(bodyString)

            if (jsonObject.getBoolean("success")) {
                val code = jsonObject.getString("code")
                val startIndex = code.indexOf("https://")
                val endIndex = code.indexOf("lay=1") + 5
                val ytlink = code.substring(startIndex, endIndex)
                return ytlink
            } else {
                return null
            }
        } else {
            throw HttpStatusException("failed to get youtube trailer", 400, SettingsData.provider)
        }
    }
}