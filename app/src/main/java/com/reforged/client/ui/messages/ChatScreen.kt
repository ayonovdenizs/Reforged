package com.reforged.client.ui.messages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vk.sdk.api.messages.dto.MessagesGetHistoryResponseDto
import com.vk.sdk.api.messages.dto.MessagesMessageDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import com.vk.sdk.api.groups.dto.GroupsGroupFullDto
import com.vk.sdk.api.messages.dto.MessagesAudioMessageDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onBackClick: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    val title = (state as? ChatState.Success)?.title ?: "Chat"
    val photoUrl = (state as? ChatState.Success)?.photoUrl
    val playingAudioUrl = (state as? ChatState.Success)?.playingAudioUrl

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val name = "file_${System.currentTimeMillis()}"
                viewModel.uploadAndSendFile(bytes, name)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                text = text,
                onTextChange = { text = it },
                onSendClick = {
                    viewModel.sendMessage(text)
                    text = ""
                },
                onStickerClick = {
                    viewModel.sendSticker(it)
                },
                onAttachClick = {
                    filePickerLauncher.launch("*/*")
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is ChatState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ChatState.Error -> Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is ChatState.Success -> MessageList(
                    response = currentState.history,
                    profiles = currentState.profiles,
                    groups = currentState.groups,
                    playingAudioUrl = playingAudioUrl,
                    onAudioClick = { viewModel.playAudio(it) }
                )
            }
        }
    }
}

@Composable
fun MessageList(
    response: MessagesGetHistoryResponseDto,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    playingAudioUrl: String?,
    onAudioClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val messages = response.items.reversed()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageItem(message, profiles, groups, playingAudioUrl, onAudioClick)
        }
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}

@Composable
fun MessageItem(
    message: MessagesMessageDto,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    playingAudioUrl: String?,
    onAudioClick: (String) -> Unit
) {
    val isOut = message.out?.value == 1
    val senderId = message.fromId.value
    
    val senderName: String?
    val senderPhoto: String?
    
    if (senderId > 0) {
        val user = profiles.find { it.id.value == senderId }
        senderName = user?.let { "${it.firstName} ${it.lastName}" }
        senderPhoto = user?.photo100
    } else {
        val group = groups.find { it.id.value == -senderId }
        senderName = group?.name
        senderPhoto = group?.photo100
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        if (!isOut) {
            AsyncImage(
                model = senderPhoto,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isOut) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (!isOut && senderName != null) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            
            Surface(
                color = if (isOut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (!message.text.isNullOrEmpty()) {
                        Text(text = message.text!!)
                    }
                    
                    message.attachments?.forEach { attachment ->
                        attachment.photo?.let { photo ->
                            val url = photo.sizes?.lastOrNull()?.url
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.sizeIn(maxWidth = 200.dp).padding(top = 4.dp).clip(MaterialTheme.shapes.small)
                            )
                        }
                        attachment.sticker?.let { sticker ->
                            val url = sticker.images?.lastOrNull()?.url
                            AsyncImage(
                                model = url,
                                contentDescription = "Sticker",
                                modifier = Modifier.size(128.dp).padding(top = 4.dp)
                            )
                        }
                        attachment.audioMessage?.let { audio ->
                            AudioMessageItem(
                                audio = audio,
                                isPlaying = playingAudioUrl == audio.linkMp3,
                                onPlayClick = { audio.linkMp3?.let { onAudioClick(it) } }
                            )
                        }
                        attachment.graffiti?.let { graffiti ->
                            AsyncImage(
                                model = graffiti.url,
                                contentDescription = "Graffiti",
                                modifier = Modifier.sizeIn(maxWidth = 200.dp).padding(top = 4.dp)
                            )
                        }
                        attachment.doc?.let { doc ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = doc.title ?: "Файл", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioMessageItem(
    audio: MessagesAudioMessageDto,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPlayClick) {
            Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = "Голосовое сообщение",
                style = MaterialTheme.typography.labelSmall
            )
            LinearProgressIndicator(
                progress = { 0f },
                modifier = Modifier.width(100.dp).height(2.dp)
            )
            Text(
                text = "${audio.duration}с",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onStickerClick: (Int) -> Unit,
    onAttachClick: () -> Unit
) {
    var showStickers by remember { mutableStateOf(false) }

    Column {
        if (showStickers) {
            StickerPicker(onStickerSelect = {
                onStickerClick(it)
                showStickers = false
            })
        }
        
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showStickers = !showStickers }) {
                    Text("😊")
                }
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSendClick, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Отправить")
                }
            }
        }
    }
}

@Composable
fun StickerPicker(onStickerSelect: (Int) -> Unit) {
    val stickerIds = listOf(62369, 62370, 62371, 62372, 62373, 62374)
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(stickerIds) { id ->
                val url = "https://vk.ru/sticker/1-$id-128"
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(80.dp)
                        .clickable { onStickerSelect(id) }
                )
            }
        }
    }
}
