package com.reforged.client.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reforged.client.data.remote.BadgeDto
import com.vk.sdk.api.photos.dto.PhotosPhotoDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import com.vk.sdk.api.video.dto.VideoVideoFullDto
import com.vk.sdk.api.wall.dto.WallWallItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Выход")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is ProfileState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ProfileState.Error -> Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is ProfileState.Success -> ProfileContent(
                    profile = currentState.profile,
                    wallPosts = currentState.wallPosts,
                    photos = currentState.photos,
                    videos = currentState.videos,
                    badges = currentState.badges,
                    onSettingsClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    profile: UsersUserFullDto,
    wallPosts: List<WallWallItemDto>,
    photos: List<PhotosPhotoDto>,
    videos: List<VideoVideoFullDto>,
    badges: List<BadgeDto>,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Background & Avatar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            
            AsyncImage(
                model = profile.photo200,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${profile.firstName} ${profile.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Badges
                badges.forEach { badge ->
                    BadgeView(badge)
                }
            }
            
            if (!profile.status.isNullOrEmpty()) {
                Text(
                    text = profile.status!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Edit */ },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Редактировать")
                }
                
                Button(
                    onClick = onSettingsClick,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Настройки")
                }
            }

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem("Друзья", profile.counters?.friends ?: 0)
                ProfileStatItem("Подписчики", profile.followersCount ?: 0)
                ProfileStatItem("Фото", profile.counters?.photos ?: 0)
                ProfileStatItem("Видео", profile.counters?.videos ?: 0)
            }
        }

        if (photos.isNotEmpty()) {
            ProfileSectionHeader("ФОТОГРАФИИ", profile.counters?.photos ?: photos.size)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                photos.forEach { photo ->
                    AsyncImage(
                        model = photo.sizes?.lastOrNull()?.url,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        if (videos.isNotEmpty()) {
            ProfileSectionHeader("ВИДЕО", profile.counters?.videos ?: videos.size)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                videos.forEach { video ->
                    Box(modifier = Modifier.size(160.dp, 90.dp)) {
                        AsyncImage(
                            model = video.image?.lastOrNull()?.url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ProfileInfoItem("🎂", "День рождения", profile.bdate ?: "Не указан")
        ProfileInfoItem("📍", "Город", profile.city?.title ?: "Не указан")
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Wall
        Text(
            text = "ЗАПИСИ НА СТЕНЕ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        
        wallPosts.forEach { item ->
            if (item is WallWallItemDto.WallWallpostFullDto) {
                WallPostItem(item)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BadgeView(badge: BadgeDto) {
    val emoji = when (badge.slug) {
        "verified_donator" -> "✅"
        "prometheus" -> "🔥"
        "developer" -> "👨‍💻"
        else -> "✨"
    }
    Text(text = emoji, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
fun WallPostItem(post: WallWallItemDto.WallWallpostFullDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (!post.text.isNullOrEmpty()) {
            Text(text = post.text!!, style = MaterialTheme.typography.bodyMedium)
        }
        post.attachments?.forEach { attachment ->
            attachment.photo?.let { photo ->
                AsyncImage(
                    model = photo.sizes?.lastOrNull()?.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(top = 8.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Text(text = " ${post.likes?.count ?: 0}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Text(text = " ${post.comments?.count ?: 0}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title $count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "ВСЕ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileStatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileInfoItem(iconEmoji: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = iconEmoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
