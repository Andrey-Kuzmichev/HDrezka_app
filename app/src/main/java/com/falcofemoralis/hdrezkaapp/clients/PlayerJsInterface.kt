package com.falcofemoralis.hdrezkaapp.clients

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.webkit.JavascriptInterface
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.broadcasts.NotificationListener
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment.Companion.presenter
import com.squareup.picasso.Picasso

class PlayerJsInterface(val mContext: Context) {
    val id: Int = 0

    @JavascriptInterface
    fun mediaAction(type: String?) {
        playing = type.toBoolean()

        createNotificationChannel(id)

        val expandedView = RemoteViews(mContext.packageName, R.layout.notification_ui_expanded)
        expandedView.setTextViewText(R.id.title, presenter?.film?.title)

        if (playing) {
            expandedView.setImageViewResource(R.id.pausePlay, R.drawable.ic_baseline_pause_24)
        } else {
            expandedView.setImageViewResource(R.id.pausePlay, R.drawable.ic_baseline_play_arrow_24)
        }

        val intent = Intent(mContext, NotificationListener::class.java)
        intent.putExtra("ID", id)
        val pendingSwitchIntent = PendingIntent.getBroadcast(mContext, id, intent, 0)
        expandedView.setOnClickPendingIntent(R.id.pausePlay, pendingSwitchIntent)

        val builder = NotificationCompat.Builder(mContext, "$id")
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
            .setCustomContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifyanager = notificationManager

        val built = builder.build()
        Picasso.get().load(presenter?.film?.posterPath).into(expandedView, R.id.notif_poster, id, built)
        notificationManager.notify(id, built)
    }

    companion object {
        fun stop() {
            FilmFragment.playerView?.evaluateJavascript("mediaElement.pause();", null)
            playing = false
        }

        var playing = false
        var notifyanager: NotificationManager? = null
    }

    private fun createNotificationChannel(id: Number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("$id", "HDrezka фильм", NotificationManager.IMPORTANCE_LOW)
            val manager: NotificationManager = mContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}