package com.reforged.client.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.data.remote.PlaylistDto
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(viewModel: MusicViewModel) {
    val state by viewModel.state.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlayerSheet by remember { mutableStateOf(false) }

    val tabs = listOf("Главная", "Моя музыка", "Обзор")

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Back */ }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                        Text(
                            text = "Музыка",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )
                        IconButton(onClick = { /* Playlist manager */ }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null)
                        }
                    }
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { viewModel.selectTab(index) },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (currentTrack != null) {
                MiniPlayer(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    progress = progress,
                    onTogglePlayback = { viewModel.togglePlayback() },
                    onNext = { viewModel.skipNext() },
                    onPrevious = { viewModel.skipPrevious() },
                    onClick = { showPlayerSheet = true }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is MusicState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MusicState.Error -> Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                is MusicState.Success -> {
                    when (selectedTab) {
                        0 -> MainCatalogContent(currentState.catalog, viewModel)
                        1 -> MyMusicContent(currentState.myTracks, viewModel)
                        2 -> Text("Обзор в разработке", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }

    if (showPlayerSheet && currentTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlayerSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerContent(
                track = currentTrack!!,
                isPlaying = isPlaying,
                progress = progress,
                currentTime = currentTime,
                duration = duration,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                onTogglePlayback = { viewModel.togglePlayback() },
                onNext = { viewModel.skipNext() },
                onPrevious = { viewModel.skipPrevious() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onSeek = { viewModel.seekTo(it) },
                onClose = { showPlayerSheet = false }
            )
        }
    }
}

@Composable
fun FullPlayerContent(
    track: AudioTrackDto,
    isPlaying: Boolean,
    progress: Float,
    currentTime: Long,
    duration: Long,
    isShuffle: Boolean,
    repeatMode: Int,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Text("Сейчас играет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = track.album?.thumb?.photo_600 ?: track.album?.thumb?.photo_300,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        var sliderPosition by remember { mutableFloatStateOf(progress) }
        var isDragging by remember { mutableStateOf(false) }

        LaunchedEffect(progress) {
            if (!isDragging) {
                sliderPosition = progress
            }
        }

        Column {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isDragging = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeek(sliderPosition)
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentMs = if (isDragging) (sliderPosition * duration).toLong() else currentTime
                Text(formatTime(currentMs), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(duration), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
            }
            FloatingActionButton(
                onClick = onTogglePlayback,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onToggleRepeat) {
                Icon(
                    when (repeatMode) {
                        1 -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = null,
                    tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MainCatalogContent(catalog: List<CatalogSectionDto>, viewModel: MusicViewModel) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item {
            SearchBarPlaceholder()
        }
        item {
            SubscriptionBanner()
        }
        
        items(catalog) { section ->
            CatalogSection(section, viewModel)
        }
    }
}

@Composable
fun SearchBarPlaceholder() {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Поиск", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SubscriptionBanner() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Целый месяц — 0 ₽", color = Color.White, fontWeight = FontWeight.Bold)
                Text("с подпиской", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text("Подключить")
                }
            }
            Text("🎵", fontSize = 48.sp)
        }
    }
}

@Composable
fun CatalogSection(section: CatalogSectionDto, viewModel: MusicViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Показать все",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (section.audios != null) {
            section.audios.take(3).forEach { track ->
                MusicItem(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    onClick = { viewModel.playTrack(track) }
                )
            }
        } else if (section.playlists != null) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(section.playlists) { playlist ->
                    PlaylistCard(playlist)
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(playlist: PlaylistDto) {
    Column(modifier = Modifier.width(160.dp)) {
        AsyncImage(
            model = playlist.photo?.photo_300,
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = playlist.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
        Text(text = "VK Музыка", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun MyMusicContent(tracks: List<AudioTrackDto>, viewModel: MusicViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        itemsIndexed(tracks) { index, track ->
            if (index >= tracks.size - 10) {
                viewModel.loadAll(isNext = true)
            }
            MusicItem(
                track = track,
                isCurrent = currentTrack?.id == track.id,
                onClick = { viewModel.playTrack(track) }
            )
        }
    }
}

@Composable
fun MusicItem(track: AudioTrackDto, isCurrent: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.album?.thumb?.photo_300,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun MiniPlayer(
    track: AudioTrackDto,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        tonalElevation = 4.dp
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.album?.thumb?.photo_300,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    Text(text = track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onTogglePlayback, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
