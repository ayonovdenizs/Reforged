package com.reforged.client.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.reforged.client.data.remote.BadgeDto
import com.vk.sdk.api.groups.dto.GroupsGroupFullDto
import com.vk.sdk.api.newsfeed.dto.NewsfeedNewsfeedItemDto
import com.vk.sdk.api.users.dto.UsersUserFullDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Лента") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is FeedState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is FeedState.Error -> Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is FeedState.Success -> NewsList(
                    currentState.posts,
                    currentState.profiles,
                    currentState.groups,
                    currentState.badges
                )
            }
        }
    }
}

@Composable
fun NewsList(
    posts: List<NewsfeedNewsfeedItemDto>,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    badges: Map<Long, List<BadgeDto>>
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(posts) { post ->
            PostCard(post, profiles, groups, badges)
        }
    }
}

@Composable
fun PostCard(
    post: NewsfeedNewsfeedItemDto,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    badges: Map<Long, List<BadgeDto>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sourceId = getSourceId(post)
            if (sourceId != null) {
                AuthorInfo(sourceId, profiles, groups, badges[sourceId] ?: emptyList())
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (post) {
                is NewsfeedNewsfeedItemDto.NewsfeedItemWallpostDto -> {
                    if (!post.text.isNullOrEmpty()) {
                        Text(text = post.text!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    post.attachments?.forEach { attachment ->
                        val photo = attachment.photo
                        if (photo != null) {
                            val url = photo.sizes?.lastOrNull()?.url
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                is NewsfeedNewsfeedItemDto.NewsfeedItemVideoDto -> {
                    val video = post.video?.items?.firstOrNull()
                    if (video != null) {
                        Text(text = video.title ?: "Video", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (!video.description.isNullOrEmpty()) {
                            Text(text = video.description!!, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        val imageUrl = video.image?.lastOrNull()?.url
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            )
                            // Play icon overlay
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("▶")
                                }
                            }
                        }
                    }
                }
                is NewsfeedNewsfeedItemDto.NewsfeedItemPhotoDto -> {
                    val photo = post.photos?.items?.firstOrNull()
                    if (photo != null) {
                        if (!post.photos?.items.isNullOrEmpty() && (post.photos?.items?.size ?: 0) > 1) {
                            Text(text = "Photos (${post.photos?.items?.size})", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        post.photos?.items?.forEach { p ->
                            val url = p.sizes?.lastOrNull()?.url
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                else -> {
                    Text(text = "Unsupported item type: ${post.javaClass.simpleName}")
                }
            }
        }
    }
}

@Composable
fun AuthorInfo(
    sourceId: Long,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    badges: List<BadgeDto>
) {
    val name: String
    val photoUrl: String?

    if (sourceId > 0) {
        val user = profiles.find { it.id.value == sourceId }
        name = "${user?.firstName} ${user?.lastName}"
        photoUrl = user?.photo100
    } else {
        val group = groups.find { it.id.value == -sourceId }
        name = group?.name ?: "Unknown Group"
        photoUrl = group?.photo100
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, style = MaterialTheme.typography.titleSmall)
                if (badges.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    BadgesRow(badges)
                }
            }
        }
    }
}

@Composable
fun BadgesRow(badges: List<BadgeDto>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        badges.forEach { badge ->
            val icon = when (badge.slug) {
                "verified_donator" -> "✓"
                "prometheus" -> "🔥"
                "developer" -> "</>"
                else -> ""
            }
            if (icon.isNotEmpty()) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (badge.slug) {
                        "verified_donator" -> MaterialTheme.colorScheme.primary
                        "prometheus" -> MaterialTheme.colorScheme.error
                        "developer" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}

fun getSourceId(post: NewsfeedNewsfeedItemDto): Long? {
    return when (post) {
        is NewsfeedNewsfeedItemDto.NewsfeedItemWallpostDto -> post.sourceId.value
        is NewsfeedNewsfeedItemDto.NewsfeedItemPhotoDto -> post.sourceId.value
        is NewsfeedNewsfeedItemDto.NewsfeedItemVideoDto -> post.sourceId.value
        else -> null
    }
}
