package com.reforged.client.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    viewModel: FeedViewModel,
    onAuthorClick: (Long) -> Unit = {}
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
                    currentState.badges,
                    onAuthorClick
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
    badges: Map<Long, List<BadgeDto>>,
    onAuthorClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(posts) { post ->
            PostCard(post, profiles, groups, badges, onAuthorClick)
        }
    }
}

@Composable
fun PostCard(
    post: NewsfeedNewsfeedItemDto,
    profiles: List<UsersUserFullDto>,
    groups: List<GroupsGroupFullDto>,
    badges: Map<Long, List<BadgeDto>>,
    onAuthorClick: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sourceId = getSourceId(post)
            if (sourceId != null) {
                AuthorInfo(sourceId, profiles, groups, badges[sourceId] ?: emptyList(), onAuthorClick)
                Spacer(modifier = Modifier.height(8.dp))
            }

            when (post) {
                is NewsfeedNewsfeedItemDto.NewsfeedItemWallpostDto -> {
                    if (!post.text.isNullOrEmpty()) {
                        Text(text = post.text!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val photos = post.attachments?.mapNotNull { it.photo } ?: emptyList()
                    if (photos.isNotEmpty()) {
                        PhotoCarousel(photos)
                        Spacer(modifier = Modifier.height(8.dp))
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
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                }
                            }
                        }
                    }
                }
                is NewsfeedNewsfeedItemDto.NewsfeedItemPhotoDto -> {
                    val photos = post.photos?.items ?: emptyList()
                    if (photos.isNotEmpty()) {
                        PhotoCarousel(photos)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PhotoCarousel(photos: List<com.vk.sdk.api.photos.dto.PhotosPhotoDto>) {
    if (photos.size == 1) {
        val url = photos[0].sizes?.lastOrNull()?.url
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
        )
    } else {
        val pagerState = rememberPagerState(pageCount = { photos.size })
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val url = photos[page].sizes?.lastOrNull()?.url
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
                
                // Page indicator overlay
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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
    badges: List<BadgeDto>,
    onAuthorClick: (Long) -> Unit
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onAuthorClick(sourceId) }
    ) {
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
                "verified_donator" -> Icons.Rounded.Verified
                "prometheus" -> Icons.Rounded.Whatshot
                "developer" -> Icons.Rounded.Code
                else -> null
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = badge.label,
                    tint = when (badge.slug) {
                        "verified_donator" -> MaterialTheme.colorScheme.primary
                        "prometheus" -> MaterialTheme.colorScheme.error
                        "developer" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(16.dp).padding(horizontal = 1.dp)
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
