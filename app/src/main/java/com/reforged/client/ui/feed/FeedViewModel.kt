package com.reforged.client.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.remote.BadgeDto
import com.reforged.client.data.repository.BadgeRepository
import com.reforged.client.data.repository.NewsRepository
import com.vk.sdk.api.groups.dto.GroupsGroupFullDto
import com.vk.sdk.api.newsfeed.dto.NewsfeedNewsfeedItemDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FeedState {
    object Loading : FeedState()
    data class Success(
        val posts: List<NewsfeedNewsfeedItemDto>,
        val profiles: List<UsersUserFullDto>,
        val groups: List<GroupsGroupFullDto>,
        val badges: Map<Long, List<BadgeDto>> = emptyMap()
    ) : FeedState()
    data class Error(val message: String) : FeedState()
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _state.value = FeedState.Loading
            repository.getNewsFeed().onSuccess { response ->
                val posts = response.items
                val profiles = response.profiles
                val groups = response.groups
                
                _state.value = FeedState.Success(posts, profiles, groups)
                
                // Fetch badges asynchronously
                val sourceIds = posts.mapNotNull { getSourceId(it) }.distinct()
                val badgesMap = sourceIds.map { id ->
                    async { id to badgeRepository.getBadges(id) }
                }.awaitAll().toMap()
                
                _state.value = FeedState.Success(posts, profiles, groups, badgesMap)
            }.onFailure { error ->
                _state.value = FeedState.Error(error.message ?: "Unknown error")
            }
        }
    }

    private fun getSourceId(post: NewsfeedNewsfeedItemDto): Long? {
        return when (post) {
            is NewsfeedNewsfeedItemDto.NewsfeedItemWallpostDto -> post.sourceId.value
            is NewsfeedNewsfeedItemDto.NewsfeedItemPhotoDto -> post.sourceId.value
            is NewsfeedNewsfeedItemDto.NewsfeedItemVideoDto -> post.sourceId.value
            else -> null
        }
    }
}
