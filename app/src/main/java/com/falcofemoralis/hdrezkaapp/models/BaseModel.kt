package com.falcofemoralis.hdrezkaapp.models

import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.objects.SettingsData
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment
import com.squareup.picasso.Picasso
import org.jsoup.Connection
import org.jsoup.Jsoup

object BaseModel {
    fun getJsoup(link: String?): Connection {
        val connection = Jsoup.connect(link?.replace(" ", "")?.replace("\n", ""))
            .userAgent(SettingsData.useragent)
            .ignoreContentType(true)
            //.ignoreHttpErrors(true)
            //.followRedirects(false)
            .timeout(30000)

        if (SettingsData.isAltLoading == true) {
            connection.header("Host", SettingsData.provider!!.replace("http://", "").replace("https://", "").replace(" ", "").replace("\n", ""))
                .header("Connection", "keep-alive")
                //.header("Content-Length", ""+c.request().requestBody().length())
                .header("Cache-Control", "no-cache")
                .header("Origin", SettingsData.provider)
                .header("Upgrade-Insecure-Requests", "1")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "ru-UA,ru;q=0.9,en-US;q=0.8,en;q=0.7,uk-UA;q=0.6,uk;q=0.5,ml-IN;q=0.4,ml;q=0.3,ru-RU;q=0.2")
        }
        return connection
    }

 /*   fun getPicasso(url: String,){
        Picasso
            .get()
            .load(FilmFragment.presenter?.film?.posterPath)

    }*/
}