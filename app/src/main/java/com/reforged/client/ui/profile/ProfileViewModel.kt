package com.reforged.client.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.remote.BadgeDto
import com.reforged.client.data.repository.BadgeRepository
import com.reforged.client.data.repository.ProfileRepository
import com.vk.sdk.api.photos.dto.PhotosPhotoDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import com.vk.sdk.api.video.dto.VideoVideoFullDto
import com.vk.sdk.api.wall.dto.WallWallItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val profile: UsersUserFullDto,
        val wallPosts: List<WallWallItemDto> = emptyList(),
        val photos: List<PhotosPhotoDto> = emptyList(),
        val videos: List<VideoVideoFullDto> = emptyList(),
        val badges: List<BadgeDto> = emptyList()
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    init {
        loadProfile()
    }

    fun loadProfile(userId: Long? = null) {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            repository.getProfile(userId ?: com.vk.api.sdk.VK.getUserId().value).onSuccess { profile ->
                val uId = profile.id.value
                
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
