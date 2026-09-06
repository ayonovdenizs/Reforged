package com.reforged.client.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.remote.*
import com.reforged.client.data.repository.BadgeRepository
import com.reforged.client.data.repository.NewsRepository
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
        val posts: List<NewsItemDto>,
        val profiles: List<UserDto>,
        val groups: List<GroupDto>,
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
                
                _state.value = FeedState.Success(posts, profiles ?: emptyList(), groups ?: emptyList())
                
                // Fetch badges asynchronously
                val sourceIds = posts.map { it.sourceId }.distinct()
                val badgesMap = sourceIds.map { id ->
                    async { id to badgeRepository.getBadges(id) }
                }.awaitAll().toMap()
                
                _state.value = FeedState.Success(posts, profiles ?: emptyList(), groups ?: emptyList(), badgesMap)
            }.onFailure { error ->
                _state.value = FeedState.Error(error.message ?: "Unknown error")
            }
        }
    }
}
