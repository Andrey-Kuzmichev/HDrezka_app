package com.falcofemoralis.hdrezkaapp.models

import android.util.ArrayMap
import com.falcofemoralis.hdrezkaapp.objects.Actor
import com.falcofemoralis.hdrezkaapp.objects.Film
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import org.json.JSONObject
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object ActorModel {
    private const val POST_ACTOR = "/ajax/person_info/"
    const val NO_PHOTO = "nopersonphoto"
    const val STATIC_PROTOCOL = "static"

    fun getActorMainInfo(actor: Actor): Actor {
        val data: ArrayMap<String, String> = ArrayMap()
        data["id"] = actor.id.toString()
        data["pid"] = actor.pid.toString()

        val result: Document? = BaseModel.getJsoup(SettingsData.provider + POST_ACTOR).data(data).post()

        if (result != null) {
            val bodyString: String = result.select("body").text()
            if (bodyString != "503") {
                val jsonObject = JSONObject(bodyString)

                val isSuccess: Boolean = jsonObject.getBoolean("success")

                if (isSuccess && jsonObject.has("person")) {
                    val person: JSONObject = jsonObject.getJSONObject("person")
                    actor.careers = getJsonString(person, "careers")
                    actor.link = getJsonString(person, "link")
                    actor.name = getJsonString(person, "name")
                    actor.nameOrig = getJsonString(person, "name_alt")
                    actor.photo = getJsonString(person, "photo")
                    actor.diedOnAge = getJsonString(person, "agefull")
                    actor.age = getJsonString(person, "age")
                    actor.birthday = getJsonString(person, "birthday")
                    actor.birthplace = getJsonString(person, "birthplace")
                    actor.deathday = getJsonString(person, "deathday")
                    actor.deathplace = getJsonString(person, "deathplace")
                    return actor
                } else {
                    throw HttpStatusException("failed to get actor data", 503, SettingsData.provider)
                }
            }
        } else {
            throw HttpStatusException("failed to get actor data", 400, SettingsData.provider)
        }

        actor.hasMainData = true
        return actor
    }

    private fun getJsonString(person: JSONObject, key: String): String? {
        return try {
            val obj = person.getString(key)
            if (obj.isEmpty() || obj == "null") {
                null
            } else {
                obj
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getActorFilms(actor: Actor) {
        // actor.link already with provider host!
        val document: Document = BaseModel.getJsoup(actor.link).get()

        val careerEls = document.select("div.b-person__career")
        val careers: ArrayList<Pair<String, ArrayList<Film>>> = ArrayList()
        if (careerEls != null) {
            for (el in careerEls) {
                val header = el.select("h2").text()
                // val info = el.select("span.b-person__career_stats").text()
                val films: ArrayList<Film> = FilmsListModel.getFilmsFromPage(Jsoup.parse(el.toString()))

                /*     val type: CareerType = when (name) {
                         "Актер" -> CareerType.ACTOR
                         "Режиссер" -> CareerType.DIRECTOR
                         "Сценарист" -> CareerType.SCRIPTWRITER
                         "Продюсер" -> CareerType.PRODUCER
                         "Оператор" -> CareerType.OPERATOR
                         "Монтажер" -> CareerType.EDITOR
                         else -> CareerType.ACTOR
                     }*/
                careers.add(Pair(header, films))
            }
        }

        actor.personCareerFilms = careers
    }
}