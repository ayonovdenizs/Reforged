package com.reforged.client.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import com.reforged.client.data.repository.BadgeRepository
import com.reforged.client.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val profile: UserDto,
        val wallPosts: List<WallPostDto> = emptyList(),
        val photos: List<VkPhotoDto> = emptyList(),
        val videos: List<VideoDto> = emptyList(),
        val badges: List<BadgeDto> = emptyList()
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val badgeRepository: BadgeRepository,
    private val tokenStorage: TokenStorage,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userId: Long? = savedStateHandle.get<Long>("userId")

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    init {
        loadProfile(userId)
    }

    fun loadProfile(targetUserId: Long? = null) {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            repository.getProfile(targetUserId ?: tokenStorage.userId).onSuccess { profile ->
                val uId = profile.id
                
                val wallDeferred = async { repository.getUserWall(uId) }
                val photosDeferred = async { repository.getUserPhotos(uId) }
                val videosDeferred = async { repository.getUserVideos(uId) }
                val badgesDeferred = async { badgeRepository.getBadges(uId) }
                
                _state.value = ProfileState.Success(
                    profile = profile,
                    wallPosts = wallDeferred.await().getOrNull()?.items ?: emptyList(),
                    photos = photosDeferred.await().getOrNull()?.items ?: emptyList(),
                    videos = videosDeferred.await().getOrNull()?.items ?: emptyList(),
                    badges = badgesDeferred.await()
                )
            }.onFailure { error ->
                _state.value = ProfileState.Error(error.message ?: "Unknown error")
            }
        }
    }
}
