package com.reforged.client.data.repository

import com.reforged.client.data.remote.VkApiService
import com.vk.sdk.api.newsfeed.dto.NewsfeedGenericResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val vkApiService: VkApiService
) {
    suspend fun getNewsFeed(startFrom: String? = null): Result<NewsfeedGenericResponseDto> {
        return try {
            val response = vkApiService.getNewsFeed(startFrom)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
