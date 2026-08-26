package com.reforged.client.ui.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.reforged.client.data.repository.MessagesRepository
import com.vk.sdk.api.groups.dto.GroupsGroupFullDto
import com.vk.sdk.api.messages.dto.MessagesGetConversationByIdExtendedDto
import com.vk.sdk.api.messages.dto.MessagesGetHistoryResponseDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChatState {
    object Loading : ChatState()
    data class Success(
        val history: MessagesGetHistoryResponseDto,
        val title: String = "Chat",
        val photoUrl: String? = null,
        val profiles: List<UsersUserFullDto> = emptyList(),
        val groups: List<GroupsGroupFullDto> = emptyList(),
        val playingAudioUrl: String? = null
    ) : ChatState()
    data class Error(val message: String) : ChatState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MessagesRepository,
    private val player: ExoPlayer,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val peerId: Long = checkNotNull(savedStateHandle["peerId"])

    private val _state = MutableStateFlow<ChatState>(ChatState.Loading)
    val state: StateFlow<ChatState> = _state

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadHistory(isSilent = true)
        }
    }

    init {
        loadHistory()
        val filter = IntentFilter("com.reforged.client.NEW_MESSAGE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(messageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(messageReceiver, filter)
        }
    }

    fun loadHistory(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) _state.value = ChatState.Loading
            
            val historyDeferred: Deferred<Result<MessagesGetHistoryResponseDto>> = async { repository.getHistory(peerId) }
            val convDeferred: Deferred<Result<MessagesGetConversationByIdExtendedDto>> = async { repository.getConversation(peerId) }
            
            val historyResult = historyDeferred.await()
            val convResult = convDeferred.await()
            
            historyResult.onSuccess { history ->
                val conv = convResult.getOrNull()
                val item = conv?.items?.firstOrNull()
                
                var title = "Chat"
                var photoUrl: String? = null
                
                if (item?.chatSettings != null) {
                    title = item.chatSettings!!.title
                    photoUrl = item.chatSettings!!.photo?.photo100
                } else if (peerId > 0) {
                    val user = conv?.profiles?.find { it.id.value == peerId }
                    if (user != null) {
                        title = "${user.firstName} ${user.lastName}"
                        photoUrl = user.photo100
                    }
                } else {
                    val group = conv?.groups?.find { it.id.value == -peerId }
                    if (group != null) {
                        title = group.name ?: "Group"
                        photoUrl = group.photo100
                    }
                }
                
                val currentPlaying = (state.value as? ChatState.Success)?.playingAudioUrl

                _state.value = ChatState.Success(
                    history = history,
                    title = title,
                    photoUrl = photoUrl,
                    profiles = conv?.profiles ?: emptyList(),
                    groups = conv?.groups ?: emptyList(),
                    playingAudioUrl = currentPlaying
                )
            }.onFailure { error ->
                if (!isSilent) _state.value = ChatState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(peerId, text).onSuccess {
                loadHistory(isSilent = true)
            }
        }
    }

    fun sendSticker(stickerId: Int) {
        viewModelScope.launch {
            repository.sendMessage(peerId, "", stickerId = stickerId).onSuccess {
                loadHistory(isSilent = true)
            }
        }
    }

    fun playAudio(url: String) {
        val currentState = state.value as? ChatState.Success ?: return
        
        if (currentState.playingAudioUrl == url && player.isPlaying) {
            player.pause()
            _state.value = currentState.copy(playingAudioUrl = null)
        } else {
            player.stop()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.play()
            _state.value = currentState.copy(playingAudioUrl = url)
        }
    }

    fun uploadAndSendFile(fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            repository.getDocsUploadServer(peerId).onSuccess { uploadUrl ->
                repository.uploadDocument(uploadUrl, fileBytes, fileName).onSuccess { attachment ->
                    repository.sendMessage(peerId, "", attachments = listOf(attachment)).onSuccess {
                        loadHistory(isSilent = true)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(messageReceiver)
        } catch (e: Exception) { }
    }
}
