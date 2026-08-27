package com.reforged.client

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.reforged.client.service.LongPollService
import com.reforged.client.service.NotificationWorker
import java.util.concurrent.TimeUnit
import com.reforged.client.ui.auth.AuthViewModel
import com.reforged.client.ui.auth.LoginScreen
import com.reforged.client.ui.feed.FeedScreen
import com.reforged.client.ui.feed.FeedViewModel
import com.reforged.client.ui.messages.ChatScreen
import com.reforged.client.ui.messages.ChatViewModel
import com.reforged.client.ui.messages.MessagesScreen
import com.reforged.client.ui.messages.MessagesViewModel
import com.reforged.client.ui.music.MusicScreen
import com.reforged.client.ui.music.MusicViewModel
import com.reforged.client.ui.navigation.NavigationItem
import com.reforged.client.ui.profile.ProfileScreen
import com.reforged.client.ui.profile.ProfileViewModel
import com.reforged.client.ui.settings.SettingsScreen
import com.reforged.client.ui.settings.SettingsViewModel
import com.reforged.client.ui.theme.ReforgedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleNotifications()
        
        setContent {
            ReforgedTheme {
                val isAuthorized by authViewModel.isAuthorized.collectAsState()

                if (isAuthorized) {
                    NotificationPermissionRequest()
                    LaunchedEffect(Unit) {
                        val intent = Intent(this@MainActivity, LongPollService::class.java)
                        startService(intent)
                    }
                    MainScreen(onLogout = { 
                        authViewModel.logout()
                        stopService(Intent(this@MainActivity, LongPollService::class.java))
                    })
                } else {
                    LoginScreen(viewModel = authViewModel)
                }
            }
        }
    }

    private fun scheduleNotifications() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_check",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

@Composable
fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ -> }
        
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        NavigationItem.Feed,
        NavigationItem.Messages,
        NavigationItem.Music,
        NavigationItem.Profile
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Hide bottom bar in chat
            if (currentRoute != "chat/{peerId}") {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    navController.graph.startDestinationRoute?.let { route ->
                                        popUpTo(route) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationItem.Feed.route) {
                val feedViewModel: FeedViewModel = hiltViewModel()
                FeedScreen(
                    viewModel = feedViewModel,
                    onAuthorClick = { userId ->
                        navController.navigate("profile/$userId")
                    }
                )
            }
            composable(NavigationItem.Messages.route) {
                val messagesViewModel: MessagesViewModel = hiltViewModel()
                MessagesScreen(
                    viewModel = messagesViewModel,
                    onConversationClick = { peerId ->
                        navController.navigate("chat/$peerId")
                    }
                )
            }
            composable(
                route = "chat/{peerId}",
                arguments = listOf(navArgument("peerId") { type = NavType.LongType })
            ) {
                val chatViewModel: ChatViewModel = hiltViewModel()
                ChatScreen(
                    viewModel = chatViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavigationItem.Music.route) {
                val musicViewModel: MusicViewModel = hiltViewModel()
                MusicScreen(viewModel = musicViewModel)
            }
            composable(NavigationItem.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onSettingsClick = { navController.navigate(NavigationItem.Settings.route) },
                    onLogoutClick = onLogout
                )
            }
            composable(
                route = "profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onSettingsClick = {},
                    onLogoutClick = {},
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavigationItem.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Экран: $name (В разработке)", style = MaterialTheme.typography.headlineMedium)
    }
}
