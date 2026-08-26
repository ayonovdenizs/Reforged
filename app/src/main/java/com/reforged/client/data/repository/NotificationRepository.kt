package com.reforged.client.data.repository

import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.sdk.api.messages.MessagesService
import com.vk.sdk.api.messages.dto.MessagesGetConversationsResponseDto
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class NotificationRepository @Inject constructor() {

    suspend fun getUnreadCount(): Int = suspendCancellableCoroutine { continuation ->
        VK.execute(MessagesService().messagesGetConversations(filter = null, count = 1), object : VKApiCallback<MessagesGetConversationsResponseDto> {
            override fun success(result: MessagesGetConversationsResponseDto) {
                continuation.resume(result.unreadCount ?: 0)
            }

            override fun fail(error: Exception) {
                continuation.resume(0)
            }
        })
    }
}
