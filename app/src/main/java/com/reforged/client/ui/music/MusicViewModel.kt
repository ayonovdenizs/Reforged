package com.reforged.client.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.manager.MusicPlayerManager
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _state = MutableStateFlow<MusicState>(MusicState.Loading)
    val state: StateFlow<MusicState> = _state

    val currentTrack: StateFlow<AudioTrackDto?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val progress: StateFlow<Float> = playerManager.progress
    val currentTime: StateFlow<Long> = playerManager.currentTime
    val duration: StateFlow<Long> = playerManager.duration
    val isShuffle: StateFlow<Boolean> = playerManager.shuffleMode
    val repeatMode: StateFlow<Int> = playerManager.repeatMode

    private val _selectedTab = MutableStateFlow(0) // 0: Main, 1: My Music, 2: Browse
    val selectedTab: StateFlow<Int> = _selectedTab

    init {
        loadAll()
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

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun playTrack(track: AudioTrackDto) {
        val currentState = state.value as? MusicState.Success
        if (currentState != null && currentState.myTracks.contains(track)) {
            playerManager.playPlaylist(currentState.myTracks, currentState.myTracks.indexOf(track))
        } else {
            playerManager.playTrack(track)
        }
    }

    fun togglePlayback() {
        playerManager.togglePlayback()
    }

    fun skipNext() {
        playerManager.next()
    }

    fun skipPrevious() {
        playerManager.previous()
    }

    fun seekTo(position: Float) {
        playerManager.seekTo(position)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }
}
