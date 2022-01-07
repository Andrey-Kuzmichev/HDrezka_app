package com.falcofemoralis.hdrezkaapp.presenters

import com.falcofemoralis.hdrezkaapp.constants.AdapterAction
import com.falcofemoralis.hdrezkaapp.interfaces.IConnection
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmsListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException

class FilmsListPresenter(
    private val filmsListView: FilmsListView,
    private val view: IConnection,
    private val iFilmsList: IFilmsList,
) {
    interface IFilmsList {
        fun getMoreFilms(): ArrayList<Film>
    }

    private var isLoading: Boolean = false // loading condition
    val filmList: ArrayList<Film> = ArrayList()
    val allFilms: ArrayList<Film> = ArrayList() // all loaded films
    val activeFilms: ArrayList<Film> = ArrayList() // current active films
    private var token = ""

    fun setToken(token: String) {
        this.token = token
    }

    fun getNextFilms() {
        if (isLoading) {
            return
        }

        isLoading = true
        filmsListView.setProgressBarState(IProgressState.StateType.LOADING)
        GlobalScope.launch {
            try {
                if (filmList.size == 0) {
                    filmList.addAll(iFilmsList.getMoreFilms())
                }

                val tokenTmp = token
                if (tokenTmp == token) {
                    allFilms.addAll(filmList)

                    withContext(Dispatchers.Main) {
                        addFilms(filmList)
                        filmList.clear()
                    }
                }
            } catch (e: Exception) {
                if (e is HttpStatusException) {
                    if (e.statusCode != 404 || e.statusCode == 503) {
                        ExceptionHelper.catchException(e, view)
                    }

                    isLoading = false
                    withContext(Dispatchers.Main) {
                        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
                    }
                    return@launch
                } else {
                    ExceptionHelper.catchException(e, view)
                }
            }
        }
    }

    private fun addFilms(films: ArrayList<Film>) {
        isLoading = false

        activeFilms.addAll(films)
        filmsListView.redrawFilms(activeFilms.size, films.size, AdapterAction.ADD, films)
        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
    }

    fun reset() {
        isLoading = false
        allFilms.clear()
        val itemsCount = activeFilms.size
        activeFilms.clear()
        filmsListView.redrawFilms(0, itemsCount, AdapterAction.DELETE, ArrayList())
    }
}