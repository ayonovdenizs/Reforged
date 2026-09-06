package com.reforged.client.data.repository

import com.reforged.client.data.remote.api.VKService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val vkService: VKService
) {

    suspend fun getUnreadCount(): Int {
        return try {
            val response = vkService.getConversations(count = 1)
            if (response.isSuccessful) {
                response.body()?.response?.unreadCount ?: 0
            } else {
                0
            }
        } catch (_: Exception) {
            0
        }
    }
}
