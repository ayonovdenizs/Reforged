package com.reforged.client.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reforged.client.data.remote.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    if (viewModel.userId != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (viewModel.userId == null) {
                        IconButton(onClick = onLogoutClick) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Выход")
                        }
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
                    isOwnProfile = viewModel.userId == null,
                    onSettingsClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    profile: UserDto,
    wallPosts: List<WallPostDto>,
    photos: List<VkPhotoDto>,
    videos: List<VideoDto>,
    badges: List<BadgeDto>,
    isOwnProfile: Boolean,
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
                    text = profile.status,
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
                if (isOwnProfile) {
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
                } else {
                    Button(
                        onClick = { /* Send Message */ },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Сообщение")
                    }
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
            WallPostItem(item)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BadgeView(badge: BadgeDto) {
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
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun WallPostItem(post: WallPostDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (post.text.isNotEmpty()) {
            Text(text = post.text, style = MaterialTheme.typography.bodyMedium)
        }
        
        val photos = post.attachments?.mapNotNull { it.photo } ?: emptyList()
        if (photos.isNotEmpty()) {
            PhotoCarousel(photos)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Text(text = " ${post.likes?.count ?: 0}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            // Comment count is not directly in WallPostDto, maybe in future
        }
    }
}

@Composable
fun PhotoCarousel(photos: List<VkPhotoDto>) {
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
