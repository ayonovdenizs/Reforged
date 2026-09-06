package com.reforged.client.data.repository

import com.reforged.client.data.remote.NewsfeedResponse
import com.reforged.client.data.remote.api.VKService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val vkService: VKService
) {

    suspend fun getNewsFeed(startFrom: String? = null): Result<NewsfeedResponse> {
        return try {
            val response = vkService.getNewsFeed(startFrom)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
