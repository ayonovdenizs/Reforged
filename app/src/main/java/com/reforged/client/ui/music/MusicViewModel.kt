package com.reforged.client.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.data.repository.MusicRepository
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val player: ExoPlayer
) : ViewModel() {

    private val _state = MutableStateFlow<MusicState>(MusicState.Loading)
    val state: StateFlow<MusicState> = _state

    private val _currentTrack = MutableStateFlow<AudioTrackDto?>(null)
    val currentTrack: StateFlow<AudioTrackDto?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _selectedTab = MutableStateFlow(0) // 0: Main, 1: My Music, 2: Browse
    val selectedTab: StateFlow<Int> = _selectedTab

    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        loadAll()
        observePlayer()
    }

    private fun observePlayer() {
        viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    val current = player.currentPosition.toFloat()
                    val total = player.duration.toFloat()
                    if (total > 0) {
                        _progress.value = current / total
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
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
                    .build()
            )
            .build()
            
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
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
