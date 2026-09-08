package com.reforged.client.data.repository

import com.reforged.client.data.remote.NewsfeedResponse
import com.reforged.client.data.remote.api.VkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient
) {
    suspend fun getNewsFeed(startFrom: String? = null): Result<NewsfeedResponse> {
        return try {
            val wrapper = vkHttpClient.getNewsFeed(startFrom)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Empty response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
