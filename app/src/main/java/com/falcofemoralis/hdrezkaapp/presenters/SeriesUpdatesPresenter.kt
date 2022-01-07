package com.falcofemoralis.hdrezkaapp.presenters

import android.content.Context
import com.falcofemoralis.hdrezkaapp.constants.HintType
import com.falcofemoralis.hdrezkaapp.models.FilmModel
import com.falcofemoralis.hdrezkaapp.models.NewestFilmsModel
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SeriesUpdateItem
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.objects.UserData
import com.falcofemoralis.hdrezkaapp.utils.ExceptionHelper
import com.falcofemoralis.hdrezkaapp.views.viewsInterface.SeriesUpdatesView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException

class SeriesUpdatesPresenter(private val seriesUpdatesView: SeriesUpdatesView) {
    var seriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>>? = null
    var userSeriesUpdates: LinkedHashMap<String, ArrayList<SeriesUpdateItem>>? = null
    var savedSeriesUpdates: ArrayList<SeriesUpdateItem>? = null

    fun initFilmsList() {
        if (UserData.isLoggedIn == true) {
            GlobalScope.launch {
                try {
                    userSeriesUpdates?.let {
                        val overall = it.size
                        var downloadListsCount = 0
                        val loadedList: LinkedHashMap<String, ArrayList<Film>> = LinkedHashMap()
                        for ((date, list) in userSeriesUpdates!!) {
                            loadedList[date] = ArrayList()
                        }

                        for ((date, seriesUpdatesList) in it) {
                            val filmsList: ArrayList<Film> = ArrayList()
                            for (item in seriesUpdatesList) {
                                val film = Film(SettingsData.provider + item.filmLink)
                                film.subInfo = "${item.season} - ${item.episode}"
                                if (!item.voice.isNullOrEmpty()) {
                                    film.subInfo += "\n${item.voice}"
                                }
                                filmsList.add(film)
                            }

                            FilmModel.getFilmsData(filmsList, filmsList.size) { films ->
                                loadedList[date] = films

                                downloadListsCount++
                                if (downloadListsCount == overall) {
                                    GlobalScope.launch {
                                        withContext(Dispatchers.Main) {
                                            if (films.size > 0) {
                                                seriesUpdatesView.setList(loadedList)
                                            } else {
                                                seriesUpdatesView.setHint(HintType.EMPTY)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is HttpStatusException) {
                        if (e.statusCode == 503) {
                            //actorView.showMsg(IConnection.ErrorType.PARSING_ERROR)
                        } else {
                            ExceptionHelper.catchException(e, seriesUpdatesView)
                        }
                    } else {
                        ExceptionHelper.catchException(e, seriesUpdatesView)
                    }
                }
            }
        } else {
            seriesUpdatesView.setHint(HintType.NOT_AUTH)
        }
    }

    fun saveUserUpdatesList(context: Context) {
        if (UserData.isLoggedIn == true) {
            try {
                UserData.saveUserSeriesUpdates(savedSeriesUpdates, context)
                seriesUpdatesView.resetBadge()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initUserUpdatesData(_context: Context, updateNotifyBadge: (n: Int) -> Unit, createNotifyBtn: () -> Unit) {
        userSeriesUpdates = LinkedHashMap()

        GlobalScope.launch {
            try {
                var badgeCount = 0

                if (UserData.isLoggedIn == true) {
                    seriesUpdates = NewestFilmsModel.getSeriesUpdates()
                    savedSeriesUpdates = UserData.getUserSeriesUpdates(_context)

                    if (seriesUpdates != null && seriesUpdates!!.size > 0) {
                        for ((date, list) in seriesUpdates!!) {
                            val userList: ArrayList<SeriesUpdateItem> = ArrayList()
                            for (item in list) {
                                if (item.isUserWatch) {
                                    userList.add(item)
                                }
                            }

                            if (userList.size > 0) {
                                userSeriesUpdates!![date] = userList
                            }
                        }

                        for ((date, list) in userSeriesUpdates!!) {
                            for (item in list) {
                                if (savedSeriesUpdates != null && !savedSeriesUpdates!!.contains(item)) { // не содержит
                                    badgeCount++
                                    savedSeriesUpdates?.add(item)
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    updateNotifyBadge(badgeCount)
                    createNotifyBtn()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initAllUpdatesList() {
        GlobalScope.launch {
            try {
                if (seriesUpdates == null) {
                    seriesUpdates = NewestFilmsModel.getSeriesUpdates()
                } else {
                    Thread.sleep(50) // fix
                }

                withContext(Dispatchers.Main) {
                    if (seriesUpdates != null) {
                        seriesUpdatesView.updateDialog(seriesUpdates!!)
                    }
                }
            } catch (e: Exception) {
                ExceptionHelper.catchException(e, seriesUpdatesView)
            }
        }
    }
}