package com.reforged.client.data.repository

import com.google.gson.Gson
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import com.reforged.client.data.remote.api.VKService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val vkService: VKService,
    private val tokenStorage: TokenStorage
) {
    private val gson = Gson()

    suspend fun getProfile(userId: Long = tokenStorage.userId): Result<UserDto> {
        return try {
            val response = vkService.getUsers(userIds = userId.toString())
            if (response.isSuccessful) {
                val body = response.body()
                val profileJson = body?.response?.firstOrNull()
                if (profileJson != null) {
                    val profile = gson.fromJson(profileJson, UserDto::class.java)
                    Result.success(profile)
                } else {
                    Result.failure(Exception("Profile not found: ${body?.error?.error_msg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserWall(userId: Long, offset: Int = 0, count: Int = 20): Result<WallResponse> {
        return try {
            val response = vkService.getWall(ownerId = userId, offset = offset, count = count)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.error_msg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPhotos(userId: Long, offset: Int = 0, count: Int = 10): Result<PhotosResponse> {
        return try {
            val response = vkService.getPhotos(ownerId = userId, offset = offset, count = count)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.error_msg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserVideos(userId: Long, offset: Int = 0, count: Int = 10): Result<VideoResponse> {
        return try {
            val response = vkService.getVideos(ownerId = userId, offset = offset, count = count)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.error_msg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
