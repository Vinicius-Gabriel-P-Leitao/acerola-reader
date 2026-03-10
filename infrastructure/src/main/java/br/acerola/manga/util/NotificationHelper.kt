package br.acerola.manga.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import br.acerola.manga.infrastructure.R

class NotificationHelper(private val context: Context) {

    companion object {

        const val SYNC_CHANNEL_ID = "sync_channel"
        const val SYNC_NOTIFICATION_ID = 1001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = context.getString(R.string.sync_notification_channel_name)
        val descriptionText = context.getString(R.string.sync_notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(SYNC_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun createBaseNotification(title: String, content: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, 0, true)
    }

    fun updateProgress(builder: NotificationCompat.Builder, progress: Int, max: Int = 100) {
        val isIndeterminate = progress < 0
        builder.setProgress(max, if (isIndeterminate) 0 else progress, isIndeterminate)
        notificationManager.notify(SYNC_NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification() {
        notificationManager.cancel(SYNC_NOTIFICATION_ID)
    }

    fun showFinishedNotification(title: String, content: String) {
        val builder = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOngoing(false)

        notificationManager.notify(SYNC_NOTIFICATION_ID, builder.build())
    }
}
