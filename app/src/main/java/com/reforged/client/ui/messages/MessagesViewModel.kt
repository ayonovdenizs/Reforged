package com.reforged.client.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.manager.LongPollEvent
import com.reforged.client.data.manager.LongPollManager
import com.reforged.client.data.repository.MessagesRepository
import com.vk.sdk.api.messages.dto.MessagesGetConversationsResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MessagesState {
    object Loading : MessagesState()
    data class Success(val response: MessagesGetConversationsResponseDto) : MessagesState()
    data class Error(val message: String) : MessagesState()
}

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val repository: MessagesRepository,
    private val longPollManager: LongPollManager
) : ViewModel() {

    private val _state = MutableStateFlow<MessagesState>(MessagesState.Loading)
    val state: StateFlow<MessagesState> = _state

    init {
        loadConversations()
        observeLongPoll()
    }

    private fun observeLongPoll() {
        viewModelScope.launch {
            longPollManager.events.collectLatest { event ->
                if (event is LongPollEvent.RefreshConversations) {
                    loadConversations(isSilent = true)
                }
            }
        }
    }

    fun loadConversations(isSilent: Boolean = false, isNext: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && !isNext) _state.value = MessagesState.Loading
            
            val currentOffset = if (isNext) {
                (state.value as? MessagesState.Success)?.response?.items?.size ?: 0
            } else 0

            repository.getConversations(offset = currentOffset).onSuccess { response ->
                if (isNext) {
                    val current = (state.value as? MessagesState.Success)?.response
                    if (current != null) {
                        val merged = response.copy(items = current.items + response.items)
                        _state.value = MessagesState.Success(merged)
                    }
                } else {
                    _state.value = MessagesState.Success(response)
                }
            }.onFailure { error ->
                if (!isSilent) _state.value = MessagesState.Error(error.message ?: "Unknown error")
            }
        }
    }
}
