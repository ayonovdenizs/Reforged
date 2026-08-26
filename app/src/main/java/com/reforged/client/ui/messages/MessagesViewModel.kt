package com.reforged.client.ui.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.repository.MessagesRepository
import com.vk.sdk.api.messages.dto.MessagesGetConversationsResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<MessagesState>(MessagesState.Loading)
    val state: StateFlow<MessagesState> = _state

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadConversations(isSilent = true)
        }
    }

    init {
        loadConversations()
        
        val filter = IntentFilter("com.reforged.client.NEW_MESSAGE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(messageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(messageReceiver, filter)
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

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(messageReceiver)
    }
}
