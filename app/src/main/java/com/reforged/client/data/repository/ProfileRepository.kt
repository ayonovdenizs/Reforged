package com.reforged.client.data.repository

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import com.reforged.client.data.remote.api.VkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun getProfile(userId: Long = tokenStorage.userId): Result<UserDto> {
        return try {
            val wrapper = vkHttpClient.getUsers(userIds = userId.toString())
            val profile = wrapper.response?.firstOrNull()
            if (profile != null) {
                Result.success(profile)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserWall(userId: Long, offset: Int = 0, count: Int = 20): Result<WallResponse> {
        return try {
            val wrapper = vkHttpClient.getWall(ownerId = userId, offset = offset, count = count)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Wall items not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPhotos(userId: Long, offset: Int = 0, count: Int = 10): Result<PhotosResponse> {
        return try {
            val wrapper = vkHttpClient.getPhotos(ownerId = userId, offset = offset, count = count)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Photos not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserVideos(userId: Long, offset: Int = 0, count: Int = 10): Result<VideoResponse> {
        return try {
            val wrapper = vkHttpClient.getVideos(ownerId = userId, offset = offset, count = count)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Videos not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
