package com.falcofemoralis.hdrezkaapp.views.viewsInterface

import android.widget.ImageView
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IMsg
import com.falcofemoralis.hdrezkaapp.objects.*
import com.falcofemoralis.hdrezkaapp.views.elements.CommentEditor

interface FilmView : IConnection, CommentEditor.ICommentEditor {
    fun setFilmBaseData(film: Film)

    fun setFilmRatings(film: Film)

    fun setActors(actors: ArrayList<Actor>?)

    fun setDirectors(directors: ArrayList<Actor>)

    fun setCountries(countries: ArrayList<String>)

    fun setGenres(genres: ArrayList<String>)

    fun setFullSizeImage(posterPath: String)

    fun setPlayer(link: String)

    fun setSchedule(schedule: ArrayList<Pair<String, ArrayList<Schedule>>>)

    fun setCollection(collection: ArrayList<Film>)

    fun setRelated(collection: ArrayList<Film>)

    fun setBookmarksList(bookmarks: ArrayList<Bookmark>)

    fun setCommentsList(list: ArrayList<Comment>, filmId: String)

    fun redrawComments()

    fun setCommentsProgressState(state: Boolean)

    fun setCommentEditor(filmId: String)

    fun setShareBtn(title: String, link: String)

    fun changeWatchState(state: Boolean, btn: ImageView)

    fun updateBookmarksPager()

    fun updateWatchPager()

    fun updateBookmarksFilmsPager()

    fun setHRrating(rating: Float, isActive: Boolean)

    fun showTranslations(translations: ArrayList<Voice>, isDownload: Boolean, isMovie: Boolean)

    fun showStreams(streams: ArrayList<Stream>, filmTitle: String, title: String, isDownload: Boolean, translation: Voice)

    fun openStream(stream: Stream, filmTitle: String, title: String, isDownload: Boolean, translation: Voice)

    fun hideActors()

    fun setTrailer(link: String?)

    fun showMsg(msgType: IConnection.ErrorType)
}