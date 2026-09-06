package com.reforged.client.ui.music

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.data.repository.MusicRepository
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import com.reforged.client.service.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MusicState {
    object Loading : MusicState()
    data class Success(
        val myTracks: List<AudioTrackDto> = emptyList(),
        val catalog: List<CatalogSectionDto> = emptyList()
    ) : MusicState()
    data class Error(val message: String) : MusicState()
}

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val player: ExoPlayer,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow<MusicState>(MusicState.Loading)
    val state: StateFlow<MusicState> = _state

    private val _currentTrack = MutableStateFlow<AudioTrackDto?>(null)
    val currentTrack: StateFlow<AudioTrackDto?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle

    private val _repeatMode = MutableStateFlow(0) // 0: Off, 1: One, 2: All
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _selectedTab = MutableStateFlow(0) // 0: Main, 1: My Music, 2: Browse
    val selectedTab: StateFlow<Int> = _selectedTab

    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        loadAll()
        observePlayer()
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    when (_repeatMode.value) {
                        1 -> {
                            player.seekTo(0)
                            player.play()
                        }
                        else -> skipNext()
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicViewModel", "Player error: ${error.errorCodeName} (${error.errorCode})", error)
                _state.value = MusicState.Error("Ошибка воспроизведения: ${error.message}")
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { id ->
                    val currentState = state.value as? MusicState.Success ?: return@let
                    val track = currentState.myTracks.find { it.id.toString() == id }
                        ?: currentState.catalog.flatMap { it.audios ?: emptyList() }.find { it.id.toString() == id }
                    
                    if (track != null) {
                        _currentTrack.value = track
                    }
                }
            }
        })
    }

    private fun observePlayer() {
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val current = player.currentPosition
                    val total = player.duration
                    _currentTime.value = current
                    _duration.value = total
                    if (total > 0) {
                        _progress.value = current.toFloat() / total.toFloat()
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    fun seekTo(position: Float) {
        val total = player.duration
        if (total > 0) {
            player.seekTo((position * total).toLong())
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun loadAll(isNext: Boolean = false) {
        viewModelScope.launch {
            if (!isNext) _state.value = MusicState.Loading
            
            val currentOffset = if (isNext) (state.value as? MusicState.Success)?.myTracks?.size ?: 0 else 0
            
            val myMusicResult = repository.getMyMusic(offset = currentOffset)
            val catalogResult = if (!isNext) repository.getCatalog() else Result.success(emptyList())

            if (myMusicResult.isSuccess || catalogResult.isSuccess) {
                val current = (state.value as? MusicState.Success)
                _state.value = MusicState.Success(
                    myTracks = if (isNext && current != null) current.myTracks + myMusicResult.getOrDefault(emptyList()) else myMusicResult.getOrDefault(emptyList()),
                    catalog = if (isNext && current != null) current.catalog else catalogResult.getOrDefault(emptyList())
                )
            } else {
                if (!isNext) _state.value = MusicState.Error(
                    myMusicResult.exceptionOrNull()?.message ?: catalogResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    fun playTrack(track: AudioTrackDto) {
        if (track.url == null) return
        
        _currentTrack.value = track
        _isPlaying.value = true
        
        val mediaItem = MediaItem.Builder()
            .setUri(track.url)
            .setMediaId(track.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist(track.artist)
                    .setTitle(track.title)
                    .setArtworkUri(track.album?.thumb?.photo_300?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
            
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Start playback service to show notification
        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
            
            val intent = Intent(context, PlaybackService::class.java)
            context.startService(intent)
        }
    }

    fun skipNext() {
        val currentState = state.value as? MusicState.Success ?: return
        val current = _currentTrack.value ?: return
        
        // Find in myTracks or catalog (simplified: just myTracks for now)
        val index = currentState.myTracks.indexOfFirst { it.id == current.id }
        if (index != -1 && index < currentState.myTracks.size - 1) {
            playTrack(currentState.myTracks[index + 1])
        }
    }

    fun skipPrevious() {
        val currentState = state.value as? MusicState.Success ?: return
        val current = _currentTrack.value ?: return
        
        val index = currentState.myTracks.indexOfFirst { it.id == current.id }
        if (index > 0) {
            playTrack(currentState.myTracks[index - 1])
        }
    }
}
