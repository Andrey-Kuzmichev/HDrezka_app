package com.falcofemoralis.hdrezkaapp.broadcasts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.falcofemoralis.hdrezkaapp.R
import com.falcofemoralis.hdrezkaapp.clients.PlayerJsInterface
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment.Companion.presenter
import com.falcofemoralis.hdrezkaapp.views.fragments.FilmFragment.Companion.playerView
import com.squareup.picasso.Picasso

class NotificationListener : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("ID", 0)

        createNotificationChannel(context, id)

        val expandedView = RemoteViews(context.packageName, R.layout.notification_ui_expanded)
        expandedView.setTextViewText(R.id.title, presenter?.film?.title)

        if (PlayerJsInterface.playing) {
            playerView?.evaluateJavascript("mediaElement.pause();", null)
            PlayerJsInterface.playing = false
            expandedView.setImageViewResource(R.id.pausePlay, R.drawable.ic_baseline_pause_24)
        } else {
            playerView?.evaluateJavascript("mediaElement.play();", null)
            PlayerJsInterface.playing = true
            expandedView.setImageViewResource(R.id.pausePlay, R.drawable.ic_baseline_play_arrow_24)
        }

        val intent = Intent(context, NotificationListener::class.java)
        intent.putExtra("ID", id)
        val pendingSwitchIntent = PendingIntent.getBroadcast(context, id, intent, 0)
        expandedView.setOnClickPendingIntent(R.id.pausePlay, pendingSwitchIntent)

        val builder = NotificationCompat.Builder(context, "$id")
            .setSmallIcon(R.drawable.ic_baseline_play_arrow_24)
            .setCustomContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        PlayerJsInterface.notifyanager = notificationManager

        val built = builder.build()
        Picasso.get().load(presenter?.film?.posterPath).into(expandedView, R.id.notif_poster, id, built)

        notificationManager.notify(id, built)

    }

    private fun createNotificationChannel(mContext: Context, id: Number) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel("$id", "HDrezka фильм", NotificationManager.IMPORTANCE_LOW)
            val manager: NotificationManager = mContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}