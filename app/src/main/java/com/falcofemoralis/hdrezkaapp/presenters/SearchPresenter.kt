package com.falcofemoralis.hdrezkaapp.presenters

import android.content.Context
import com.falcofemoralis.hdrezkaapp.constants.AdapterAction
import com.falcofemoralis.hdrezkaapp.constants.DeviceType
import com.falcofemoralis.hdrezkaapp.interfaces.IProgressState
import com.falcofemoralis.hdrezkaapp.models.FilmModel
import com.falcofemoralis.hdrezkaapp.models.SearchModel
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper.catchException
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.FilmsListView
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.SearchView
import kotlinx.coroutines.*
import org.jsoup.HttpStatusException

class SearchPresenter(private val searchView: SearchView, private val filmsListView: FilmsListView, private val context: Context) {
    var activeSearchFilms: ArrayList<Film> = ArrayList()
    var activeListFilms: ArrayList<Film> = ArrayList()

    private var loadedListFilms: ArrayList<Film> = ArrayList()
    private var currentPage: Int = 1
    private var isLoading: Boolean = false
    private var query: String = ""
    private var token = ""

    fun initFilms() {
        filmsListView.setFilms(activeListFilms)
    }

    fun getFilms(text: String) {
        GlobalScope.launch {
            try {
                activeSearchFilms = SearchModel.getFilmsListByQuery(text)

                if (SettingsData.deviceType == DeviceType.TV) {
                    withContext(Dispatchers.Main) {
                        setQuery(text)
                    }
                } else {
                    val searchFilms: ArrayList<String> = ArrayList()
                    for (film in activeSearchFilms) {
                        searchFilms.add("${film.title} ${film.additionalInfo} ${film.ratingIMDB}")
                    }

                    withContext(Dispatchers.Main) {
                        searchView.redrawSearchFilms(searchFilms)
                    }
                }
            } catch (e: Exception) {
                catchException(e, searchView)
                return@launch
            }
        }
    }

    fun setQuery(text: String) {
        query = text

        activeSearchFilms.clear()
        val itemsCount = activeListFilms.size
        activeListFilms.clear()
        loadedListFilms.clear()
        filmsListView.redrawFilms(0, itemsCount, AdapterAction.DELETE, ArrayList())
        currentPage = 1
        isLoading = false
        token = text
        getNextFilms()
    }

    fun getNextFilms() {
        if (isLoading) {
            return
        }

        isLoading = true
        filmsListView.setProgressBarState(IProgressState.StateType.LOADING)

        GlobalScope.launch {
            try {
                fun completeSearch() {
                    GlobalScope.launch {
                        withContext(Dispatchers.Main) {
                            addFilms(loadedListFilms)
                            loadedListFilms.clear()
                        }
                    }
                }

                fun checkFilmsPage() {
                    // no more films
                    if (loadedListFilms.size == 0) {
                        throw HttpStatusException("List end", 404, SettingsData.provider)
                    }
                    currentPage++
                }

                if (loadedListFilms.size == 0) {
                    loadedListFilms = SearchModel.getFilmsFromSearchPage(query, currentPage)

                    // blocked, retry with another search
                    if (loadedListFilms.size == 0 && currentPage == 1) {
                        loadedListFilms = SearchModel.getFilmsListByQuery(query)

                        FilmModel.getFilmsData(loadedListFilms, loadedListFilms.size) { films ->
                            loadedListFilms = films
                            checkFilmsPage()
                            completeSearch()
                        }

                        return@launch
                    }

                    checkFilmsPage()
                }

                completeSearch()
            } catch (e: Exception) {
                if (e is HttpStatusException) {
                    if (e.statusCode != 404) {
                        catchException(e, searchView)
                    }
                    isLoading = false
                    withContext(Dispatchers.Main) {
                        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
                    }
                } else {
                    catchException(e, searchView)
                }

                return@launch
            }
        }
    }


    private fun addFilms(films: ArrayList<Film>) {
        isLoading = false
        val itemsCount = activeListFilms.size
        activeListFilms.addAll(films)
        filmsListView.redrawFilms(itemsCount, films.size, AdapterAction.ADD, films)
        filmsListView.setProgressBarState(IProgressState.StateType.LOADED)
    }
}