package com.reforged.client.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    object Feed : NavigationItem("feed", "Лента", Icons.Rounded.Newspaper)
    object Messages : NavigationItem("messages", "Сообщения", Icons.Rounded.ChatBubble)
    object Music : NavigationItem("music", "Музыка", Icons.Rounded.MusicNote)
    object Profile : NavigationItem("profile", "Профиль", Icons.Rounded.Person)
    object Settings : NavigationItem("settings", "Настройки", Icons.Rounded.Settings)
}
