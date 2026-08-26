package com.reforged.client.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vk.sdk.api.messages.dto.MessagesConversationWithMessageDto
import com.vk.sdk.api.messages.dto.MessagesGetConversationsResponseDto
import com.vk.sdk.api.users.dto.UsersUserFullDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(viewModel: MessagesViewModel, onConversationClick: (Long) -> Unit) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Сообщения") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is MessagesState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MessagesState.Error -> Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is MessagesState.Success -> ConversationList(
                    response = currentState.response,
                    onConversationClick = onConversationClick,
                    onLoadMore = { viewModel.loadConversations(isNext = true) }
                )
            }
        }
    }
}

@Composable
fun ConversationList(
    response: MessagesGetConversationsResponseDto,
    onConversationClick: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(response.items) { index, item ->
            if (index >= response.items.size - 5) {
                onLoadMore()
            }
            
            ConversationItem(item, response.profiles ?: emptyList(), response.groups ?: emptyList(), onClick = {
                onConversationClick(item.conversation.peer.id.value)
            })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
fun ConversationItem(
    item: MessagesConversationWithMessageDto,
    profiles: List<UsersUserFullDto>,
    groups: List<com.vk.sdk.api.groups.dto.GroupsGroupFullDto>,
    onClick: () -> Unit
) {
    val peerId = item.conversation.peer.id.value
    val title: String
    val photoUrl: String?

    if (item.conversation.chatSettings != null) {
        title = item.conversation.chatSettings!!.title
        photoUrl = item.conversation.chatSettings!!.photo?.photo100
    } else if (peerId > 0) {
        val user = profiles.find { it.id.value == peerId }
        title = if (user != null) "${user.firstName} ${user.lastName}" else "User $peerId"
        photoUrl = user?.photo100
    } else if (peerId < 0) {
        val group = groups.find { it.id.value == -peerId }
        title = group?.name ?: "Community $peerId"
        photoUrl = group?.photo100
    } else {
        title = "Unknown"
        photoUrl = null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(MaterialTheme.shapes.extraLarge)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.lastMessage?.text ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
