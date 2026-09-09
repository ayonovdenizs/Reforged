package com.reforged.client.ui.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.reforged.client.data.manager.LongPollEvent
import com.reforged.client.data.manager.LongPollManager
import com.reforged.client.data.repository.MessagesRepository
import com.reforged.client.data.remote.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

sealed class ChatState {
    object Loading : ChatState()
    data class Success(
        val history: HistoryResponse,
        val title: String = "Chat",
        val photoUrl: String? = null,
        val profiles: List<UserDto> = emptyList(),
        val groups: List<GroupDto> = emptyList(),
        val playingAudioUrl: String? = null,
        val typingUsers: List<Long> = emptyList()
    ) : ChatState()
    data class Error(val message: String) : ChatState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: MessagesRepository,
    private val player: ExoPlayer,
    private val longPollManager: LongPollManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val peerId: Long = checkNotNull(savedStateHandle["peerId"])

    private val _state = MutableStateFlow<ChatState>(ChatState.Loading)
    val state: StateFlow<ChatState> = _state

    init {
        loadHistory()
        observeLongPoll()
    }

    private fun observeLongPoll() {
        viewModelScope.launch {
            longPollManager.events.collectLatest { event ->
                when (event) {
                    is LongPollEvent.NewMessage -> {
                        if (event.peerId == peerId) {
                            handleNewMessageEvent(event)
                        }
                    }
                    is LongPollEvent.Typing -> {
                        if (event.peerId == peerId) {
                            addTypingUser(event.userId)
                        }
                    }
                    is LongPollEvent.Read -> {
                        if (event.peerId == peerId) {
                            loadHistory(isSilent = true)
                        }
                    }
                    is LongPollEvent.MessageFlags -> {
                        if (event.peerId == peerId) {
                            loadHistory(isSilent = true)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleNewMessageEvent(event: LongPollEvent.NewMessage) {
        viewModelScope.launch {
            val currentState = (state.value as? ChatState.Success) ?: return@launch
            
            // 1. Check if it's our own message echo by randomId
            if (event.isOut && event.randomId != 0L) {
                val existing = currentState.history.items.find { it.randomId.toLong() == event.randomId }
                if (existing != null && existing.id == 0) {
                    // It's an echo of our pending message, we can just reload history to get the real ID
                    loadHistory(isSilent = true)
                    return@launch
                }
            }

            // 2. If it's a full message (no attachments/fwd/reply), we can theoretically insert it.
            // But for simplicity and correctness (profiles/groups), we reload history or fetch by ID.
            if (event.isFull) {
                loadHistory(isSilent = true)
            } else {
                // Non-full message, need enrichment
                repository.getMessageById(event.messageId).onSuccess { fullMessage ->
                    val latest = (state.value as? ChatState.Success) ?: return@onSuccess
                    if (latest.history.items.any { it.id == fullMessage.id }) return@onSuccess
                    
                    val newItems = listOf(fullMessage) + latest.history.items
                    _state.value = latest.copy(history = latest.history.copy(items = newItems))
                }.onFailure {
                    loadHistory(isSilent = true)
                }
            }
        }
    }

    private fun addTypingUser(userId: Long) {
        val current = (state.value as? ChatState.Success) ?: return
        if (userId in current.typingUsers) return
        
        _state.value = current.copy(typingUsers = current.typingUsers + userId)
        
        viewModelScope.launch {
            delay(5000)
            val latest = (state.value as? ChatState.Success) ?: return@launch
            _state.value = latest.copy(typingUsers = latest.typingUsers - userId)
        }
    }

    fun loadHistory(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) _state.value = ChatState.Loading
            
            val historyDeferred: Deferred<Result<HistoryResponse>> = async { repository.getHistory(peerId) }
            val convDeferred: Deferred<Result<List<ConversationDto>>> = async { repository.getConversation(peerId) }
            
            val historyResult = historyDeferred.await()
            val convResult = convDeferred.await()
            
            historyResult.onSuccess { history ->
                val conv = convResult.getOrNull()
                val item = conv?.firstOrNull()
                
                var title = "Chat"
                var photoUrl: String? = null
                
                if (item?.chatSettings != null) {
                    title = item.chatSettings.title
                    photoUrl = item.chatSettings.photo?.photo100
                } else if (peerId > 0) {
                    val user = history.profiles?.find { it.id == peerId }
                    if (user != null) {
                        title = "${user.firstName} ${user.lastName}"
                        photoUrl = user.photo200
                    }
                } else {
                    val group = history.groups?.find { it.id == -peerId }
                    if (group != null) {
                        title = group.name
                        photoUrl = group.photo200
                    }
                }
                
                val current = (state.value as? ChatState.Success)

                _state.value = ChatState.Success(
                    history = history,
                    title = title,
                    photoUrl = photoUrl,
                    profiles = history.profiles ?: emptyList(),
                    groups = history.groups ?: emptyList(),
                    playingAudioUrl = current?.playingAudioUrl,
                    typingUsers = current?.typingUsers ?: emptyList()
                )
                
                // Mark messages as read
                repository.markAsRead(peerId)
            }.onFailure { error ->
                if (!isSilent) _state.value = ChatState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val randomId = Random.nextInt()
        
        // Optimistic UI insert (optional, but good for UX)
        viewModelScope.launch {
            val currentState = (state.value as? ChatState.Success) ?: return@launch
            val pendingMessage = MessageDto(
                id = 0, // indicates pending
                date = System.currentTimeMillis() / 1000,
                peerId = peerId,
                fromId = 0, // current user
                text = text,
                randomId = randomId,
                out = 1
            )
            _state.value = currentState.copy(
                history = currentState.history.copy(items = listOf(pendingMessage) + currentState.history.items)
            )
            
            repository.sendMessage(peerId, text, randomId = randomId).onFailure {
                // Remove pending message on failure
                val latest = (state.value as? ChatState.Success) ?: return@onFailure
                _state.value = latest.copy(
                    history = latest.history.copy(items = latest.history.items.filter { it.randomId != randomId })
                )
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
}
