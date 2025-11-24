package com.cherrystudios.bamboo.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.cherrystudios.bamboo.service.MusicPlayService
import com.cherrystudios.bamboo.ui.main.MainActivity
import kotlin.jvm.java

/**
 * MusicNotificationHelper
 *
 * @author john
 * @since 2025-11-24
 */
class MusicNotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_NEXT = "ACTION_NEXT"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        mediaSession: MediaSession,
        currentPosition: Long = 0,
        duration: Long = 0
    ): Notification {

        // Create pending intents for media controls
        val playPauseIntent = createPendingIntent(
            if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        )
        val previousIntent = createPendingIntent(ACTION_PREVIOUS)
        val nextIntent = createPendingIntent(ACTION_NEXT)

        // Create intent for clicking the notification
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(albumArt)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(duration.toInt(), currentPosition.toInt(), duration == 0L)
            .addAction(android.R.drawable.ic_media_previous, "Previous", previousIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setStyle(MediaStyleNotificationHelper.MediaStyle(mediaSession))
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, MusicPlayService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showNotification(
        title: String,
        artist: String,
        albumArt: Bitmap?,
        isPlaying: Boolean,
        mediaSession: MediaSession,
        currentPosition: Long = 0,
        duration: Long = 0
    ) {
        val notification = buildNotification(title, artist, albumArt, isPlaying, mediaSession, currentPosition, duration)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}