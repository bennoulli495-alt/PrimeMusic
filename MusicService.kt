package com.primemusic.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.media.app.NotificationCompat.MediaStyle
import com.primemusic.app.MainActivity
import com.primemusic.app.R
import com.primemusic.app.model.Song
import com.primemusic.app.util.MediaScanner

/**
 * Foreground service that owns the MediaPlayer instance so playback survives
 * the app being backgrounded, plus lock-screen / notification controls.
 */
class MusicService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "prime_music_playback"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "com.primemusic.app.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.primemusic.app.ACTION_NEXT"
        const val ACTION_PREV = "com.primemusic.app.ACTION_PREV"
        const val ACTION_STOP = "com.primemusic.app.ACTION_STOP"
    }

    interface PlaybackListener {
        fun onSongChanged(song: Song?)
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var isShuffle = false
    private var repeatMode = 0 // 0 = off, 1 = repeat all, 2 = repeat one

    var listener: PlaybackListener? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "PrimeMusicSession")
        mediaSession.isActive = true
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrevious()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    fun currentSong(): Song? = playlist.getOrNull(currentIndex)
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun getPlaylist(): List<Song> = playlist

    fun setRepeatMode(mode: Int) { repeatMode = mode }
    fun getRepeatMode(): Int = repeatMode
    fun setShuffle(enabled: Boolean) { isShuffle = enabled }
    fun getShuffle(): Boolean = isShuffle

    fun playSongs(songs: List<Song>, startIndex: Int) {
        playlist = songs
        currentIndex = startIndex
        playCurrent()
    }

    private fun playCurrent() {
        val song = playlist.getOrNull(currentIndex) ?: return
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(this@MusicService, MediaScanner.getContentUriForSong(song.id))
                setOnPreparedListener {
                    start()
                    listener?.onPlaybackStateChanged(true)
                    updateNotification()
                }
                setOnCompletionListener {
                    onSongFinished()
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        listener?.onSongChanged(song)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun onSongFinished() {
        when (repeatMode) {
            2 -> playCurrent() // repeat one
            else -> playNext()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            listener?.onPlaybackStateChanged(false)
        } else {
            player.start()
            listener?.onPlaybackStateChanged(true)
        }
        updateNotification()
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        currentIndex = if (isShuffle) {
            playlist.indices.random()
        } else if (currentIndex < playlist.size - 1) {
            currentIndex + 1
        } else if (repeatMode == 1) {
            0
        } else {
            return
        }
        playCurrent()
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        currentIndex = if (isShuffle) {
            playlist.indices.random()
        } else if (currentIndex > 0) {
            currentIndex - 1
        } else if (repeatMode == 1) {
            playlist.size - 1
        } else {
            return
        }
        playCurrent()
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val song = currentSong()
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String, code: Int): PendingIntent {
            val intent = Intent(this, MusicService::class.java).apply { this.action = action }
            return PendingIntent.getService(this, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val playPauseIcon = if (isPlaying()) R.drawable.ic_pause else R.drawable.ic_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "Prime Music")
            .setContentText(song?.artist ?: "")
            .setSmallIcon(R.drawable.ic_song)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying())
            .addAction(R.drawable.ic_prev, "Previous", actionIntent(ACTION_PREV, 1))
            .addAction(playPauseIcon, "Play/Pause", actionIntent(ACTION_PLAY_PAUSE, 2))
            .addAction(R.drawable.ic_next, "Next", actionIntent(ACTION_NEXT, 3))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaSession.release()
        super.onDestroy()
    }
}
