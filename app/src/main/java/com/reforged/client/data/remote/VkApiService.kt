package com.reforged.client.data.remote

import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.sdk.api.newsfeed.NewsfeedService
import com.vk.sdk.api.newsfeed.dto.NewsfeedGenericResponseDto
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class VkApiService @Inject constructor() {

    suspend fun getNewsFeed(startFrom: String? = null): NewsfeedGenericResponseDto = suspendCancellableCoroutine { continuation ->
        VK.execute(NewsfeedService().newsfeedGet(startFrom = startFrom), object : VKApiCallback<NewsfeedGenericResponseDto> {
            override fun success(result: NewsfeedGenericResponseDto) {
                continuation.resume(result)
            }

            override fun fail(error: Exception) {
                continuation.resumeWithException(error)
            }
        })
    }
}
