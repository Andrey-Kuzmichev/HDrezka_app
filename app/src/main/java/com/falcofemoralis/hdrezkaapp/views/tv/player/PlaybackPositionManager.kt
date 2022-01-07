package com.falcofemoralis.hdrezkaapp.views.tv.player

import android.content.Context
import com.falcofemoralis.hdrezkaapp.objects.DatabaseSaveItem
import com.falcofemoralis.hdrezkaapp.utils.FileManager
import com.google.common.reflect.TypeToken
import com.google.gson.Gson

class PlaybackPositionManager(private val context: Context, private val isSeries: Boolean) {
    private val FILMS_POSITION_FILE_NAME = "films_position"
    private var loadedFilms: ArrayList<DatabaseSaveItem> = ArrayList()
    private val gson = Gson()

    init {
        val str = FileManager.readFile(FILMS_POSITION_FILE_NAME, context)
        val myType = object : TypeToken<List<DatabaseSaveItem>>() {}.type
        loadedFilms.clear()
        val items = gson.fromJson<List<DatabaseSaveItem>>(str, myType)
        if(items != null && items.isNotEmpty()){
            for(item in items){
                loadedFilms.add(item)
            }
        }
    }

    fun updateTime(filmId: Int?, transId: String?, lastTime: Long) {
        val item: DatabaseSaveItem? = getItem(filmId, transId)

        if (item != null) {
            item.lastTime = lastTime
            updateItem(item)
            updateFile()
        }
    }

    fun updateTime(filmId: Int?, transId: String?, season: String?, episode: String?, lastTime: Long) {
        val item: DatabaseSaveItem? = getItem(filmId, transId, season, episode)

        if (item != null) {
            item.lastTime = lastTime
            updateItem(item)
            updateFile()
        }
    }

    private fun updateItem(databaseSaveItem: DatabaseSaveItem) {
        for (item in loadedFilms) {
            if (isSeries) {
                if (item.filmId == databaseSaveItem.filmId &&
                    item.translationId == databaseSaveItem.translationId &&
                    item.season == databaseSaveItem.season &&
                    item.episode == databaseSaveItem.episode
                ) {
                    item.lastTime = databaseSaveItem.lastTime
                    return
                }
            } else {
                if (item.filmId == databaseSaveItem.filmId &&
                    item.translationId == databaseSaveItem.translationId
                ) {
                    item.lastTime = databaseSaveItem.lastTime
                    return
                }
            }
        }

        loadedFilms.add(databaseSaveItem)
    }

    private fun updateFile() {
        FileManager.writeFile(FILMS_POSITION_FILE_NAME, gson.toJson(loadedFilms), false, context)
    }

    fun getLastTime(filmId: Int?, transId: String?): Long {
        val item: DatabaseSaveItem? = getItem(filmId, transId)
        return if (item != null) {
            getTime(item)
        } else {
            0L
        }
    }

    fun getLastTime(filmId: Int?, transId: String?, season: String?, episode: String?): Long {
        val item: DatabaseSaveItem? = getItem(filmId, transId, season, episode)
        return if (item != null) {
            getTime(item)
        } else {
            0L
        }
    }

    private fun getTime(databaseSaveItem: DatabaseSaveItem): Long {
        for (item in loadedFilms) {
            if (isSeries) {
                if (item.filmId == databaseSaveItem.filmId &&
                    item.translationId == databaseSaveItem.translationId &&
                    item.season == databaseSaveItem.season &&
                    item.episode == databaseSaveItem.episode
                ) {
                    return item.lastTime ?: 0L
                }
            } else {
                if (item.filmId == databaseSaveItem.filmId &&
                    item.translationId == databaseSaveItem.translationId
                ) {
                    return item.lastTime ?: 0L
                }
            }
        }

        return 0L
    }

    private fun getItem(filmId: Int?, transId: String?): DatabaseSaveItem? {
        var item: DatabaseSaveItem? = null
        if (filmId != null) {
            item = DatabaseSaveItem(filmId)
        }

        if (transId != null) {
            item?.translationId = transId
        }

        return item
    }

    private fun getItem(filmId: Int?, transId: String?, season: String?, episode: String?): DatabaseSaveItem? {
        var item: DatabaseSaveItem? = null
        if (filmId != null) {
            item = DatabaseSaveItem(filmId)
        }

        if (transId != null) {
            item?.translationId = transId
        }

        item?.season = season
        item?.episode = episode

        return item
    }
}