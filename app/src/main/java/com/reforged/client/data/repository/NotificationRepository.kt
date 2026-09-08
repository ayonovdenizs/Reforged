package com.reforged.client.data.repository

import com.reforged.client.data.remote.api.VkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient
) {

    suspend fun getUnreadCount(): Int {
        return try {
            val wrapper = vkHttpClient.getConversations(count = 1)
            wrapper.response?.unreadCount ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
