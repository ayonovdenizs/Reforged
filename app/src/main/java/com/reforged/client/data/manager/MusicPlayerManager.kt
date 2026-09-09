package com.reforged.client.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.service.PlaybackService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    private val player: ExoPlayer,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentTrack = MutableStateFlow<AudioTrackDto?>(null)
    val currentTrack: StateFlow<AudioTrackDto?> = _currentTrack

    private val _playlist = MutableStateFlow<List<AudioTrackDto>>(emptyList())
    val playlist: StateFlow<List<AudioTrackDto>> = _playlist

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode

    init {
        setupPlayerListener()
        startProgressUpdate()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // Handled by exoPlayer internally if repeat mode is set, 
                    // but we can add custom logic here if needed.
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { id ->
                    val track = _playlist.value.find { it.id.toString() == id }
                    if (track != null) {
                        _currentTrack.value = track
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun startProgressUpdate() {
        scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    val current = player.currentPosition
                    val total = player.duration
                    _currentTime.value = current
                    _duration.value = if (total > 0) total else 0L
                    if (total > 0) {
                        _progress.value = current.toFloat() / total.toFloat()
                    }
                }
                delay(500)
            }
        }
    }

    fun playPlaylist(tracks: List<AudioTrackDto>, startIndex: Int = 0) {
        _playlist.value = tracks
        player.stop()
        player.clearMediaItems()
        
        val mediaItems = tracks.map { track ->
            val uri = Uri.parse(track.url ?: "")
            val mimeType = if (track.url?.contains(".m3u8") == true) {
                androidx.media3.common.MimeTypes.APPLICATION_M3U8
            } else {
                androidx.media3.common.MimeTypes.AUDIO_MPEG
            }

            MediaItem.Builder()
                .setUri(uri)
                .setMimeType(mimeType)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtist(track.artist)
                        .setTitle(track.title)
                        .setArtworkUri(track.album?.thumb?.photo_300?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
        
        startPlaybackService()
    }

    fun playTrack(track: AudioTrackDto) {
        val index = _playlist.value.indexOfFirst { it.id == track.id }
        if (index != -1) {
            player.seekTo(index, 0L)
            player.play()
        } else {
            // If track not in current playlist, reset playlist to just this track
            playPlaylist(listOf(track))
        }
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                player.prepare()
            }
            player.play()
            startPlaybackService()
        }
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun seekTo(position: Float) {
        val total = player.duration
        if (total > 0) {
            player.seekTo((position * total).toLong())
        }
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun toggleRepeat() {
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
    }

    private fun startPlaybackService() {
        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }
}
