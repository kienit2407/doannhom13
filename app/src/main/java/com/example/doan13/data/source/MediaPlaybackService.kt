package com.example.doan13.data.source

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.doan13.R
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.MainActivity

class MediaPlaybackService : MediaSessionService () {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {
                @OptIn(UnstableApi::class)
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .build()
                }
            })
            .build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnd()
                }
            }
        })
        startForeground(1, createNotification())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        player = null
        super.onDestroy()
    }

    fun playSong(song: SongModels) {
        val mediaItem = MediaItem.fromUri(song.mp3Url ?: "")
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    fun playPlaylist(songs: List<SongModels>, shuffle: Boolean = false) {
        val mediaItems = songs.mapNotNull { song ->
            song.mp3Url?.let { MediaItem.fromUri(it) }
        }
        player?.setMediaItems(mediaItems)
        player?.shuffleModeEnabled = shuffle
        player?.prepare()
        player?.play()
    }

    private fun handlePlaybackEnd() {
        val currentIndex = player?.currentMediaItemIndex ?: -1
        val playlistSize = player?.mediaItemCount ?: 0
        if (currentIndex != -1 && playlistSize > 0) {
            when (mediaSession?.player?.repeatMode) {
                Player.REPEAT_MODE_ONE -> player?.seekTo(currentIndex, 0)
                Player.REPEAT_MODE_ALL -> {
                    val nextIndex = if (currentIndex < playlistSize - 1) currentIndex + 1 else 0
                    player?.seekTo(nextIndex, 0)
                }
                else -> {
                    if (currentIndex < playlistSize - 1) player?.seekTo(currentIndex + 1, 0)
                    else player?.pause()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "media_channel")
            .setContentTitle("Playing")
            .setContentText("Music Player")
            .setSmallIcon(R.drawable.close)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}