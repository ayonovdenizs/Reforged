package com.reforged.client.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBackClick: () -> Unit) {
    val blockAds by viewModel.blockAds.collectAsState()
    val blockRecommended by viewModel.blockRecommended.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Контент", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = blockAds, onCheckedChange = { viewModel.setBlockAds(it) })
                Text("Блокировать рекламу")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = blockRecommended, onCheckedChange = { viewModel.setBlockRecommended(it) })
                Text("Блокировать рекомендации")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "О приложении", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "VK Reforged v1.0", style = MaterialTheme.typography.bodyMedium)
            Text(text = "DEV BUILD", style = MaterialTheme.typography.bodySmall)
        }
    }
}
