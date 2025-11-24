package com.cherrystudios.bamboo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.image.BitmapFactoryImageDecoder
import androidx.media3.session.MediaSession
import coil.decode.BitmapFactoryDecoder
import com.cherrystudios.bamboo.constant.ACTION_MUSIC_PROGRESS
import com.cherrystudios.bamboo.constant.EXTRA_DURATION
import com.cherrystudios.bamboo.constant.EXTRA_POSITION
import com.cherrystudios.bamboo.helper.MusicNotificationHelper
import com.cherrystudios.bamboo.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * AudioPlayService
 *
 * @author john
 * @since 2025-11-12
 */
class MusicPlayService : Service() {
    private val binder = MusicBinder()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private var progressJob: Job? = null
    private lateinit var notificationHelper: MusicNotificationHelper

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayService = this@MusicPlayService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("Music play service #onCreate")
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
        notificationHelper = MusicNotificationHelper(this)
        startProgressUpdate()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (coroutineContext.isActive) {
                val position = player.currentPosition
                val duration = player.duration
                if (duration > 0 && position > 0) {
                    // 更新通知或广播给 Activity
                    updateNotification(position, duration)
                    broadcastProgress(position, duration)
                }
                delay(1000L) // 每秒更新一次
            }
        }
    }

    private fun updateNotification(position: Long = 0, duration: Long = 0) {
        notificationHelper.showNotification(
            player.mediaMetadata.title?.toString() ?: "Title",
            player.mediaMetadata.artist?.toString() ?: "Artist",
            player.mediaMetadata.artworkData?.run {
                return@run BitmapFactory.decodeByteArray(this, 0, size)
            },
            true,
            mediaSession,
            position,
            duration
        )
    }


    fun play(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        updateNotification()
    }

    fun playSong(song: Song) {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setArtworkUri(song.coverUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pauseMusic() {
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun playPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stopMusic() {
        player.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        notificationHelper.cancelNotification()
        super.onDestroy()
    }


    private fun broadcastProgress(position: Long, duration: Long) {
        val intent = Intent(ACTION_MUSIC_PROGRESS)
        intent.putExtra(EXTRA_POSITION, position)
        intent.putExtra(EXTRA_DURATION, duration)
        sendBroadcast(intent)
    }
}