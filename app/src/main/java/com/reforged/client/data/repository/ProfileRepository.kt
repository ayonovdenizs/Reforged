package com.reforged.client.data.repository

import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.dto.common.id.UserId
import com.vk.sdk.api.photos.PhotosService
import com.vk.sdk.api.photos.dto.PhotosGetResponseDto
import com.vk.sdk.api.users.UsersService
import com.vk.sdk.api.users.dto.UsersFieldsDto
import com.vk.sdk.api.users.dto.UsersUserFullDto
import com.vk.sdk.api.video.VideoService
import com.vk.sdk.api.video.dto.VideoGetResponseDto
import com.vk.sdk.api.wall.WallService
import com.vk.sdk.api.wall.dto.WallGetResponseDto
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ProfileRepository @Inject constructor() {

    suspend fun getProfile(userId: Long = VK.getUserId().value): Result<UsersUserFullDto> = suspendCancellableCoroutine { continuation ->
        val fields = listOf(
            UsersFieldsDto.PHOTO_200,
            UsersFieldsDto.ABOUT,
            UsersFieldsDto.BDATE,
            UsersFieldsDto.CITY,
            UsersFieldsDto.COUNTRY,
            UsersFieldsDto.FOLLOWERS_COUNT,
            UsersFieldsDto.COUNTERS,
            UsersFieldsDto.STATUS,
            UsersFieldsDto.SCREEN_NAME
        )
        
        VK.execute(UsersService().usersGet(userIds = listOf(UserId(userId)), fields = fields), object : VKApiCallback<List<UsersUserFullDto>> {
            override fun success(result: List<UsersUserFullDto>) {
                val profile = result.firstOrNull()
                if (profile != null) {
                    continuation.resume(Result.success(profile))
                } else {
                    continuation.resume(Result.failure(Exception("Profile not found")))
                }
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getUserWall(userId: Long, offset: Int = 0, count: Int = 20): Result<WallGetResponseDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(WallService().wallGet(ownerId = UserId(userId), offset = offset, count = count, extended = true), object : VKApiCallback<WallGetResponseDto> {
            override fun success(result: WallGetResponseDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getUserPhotos(userId: Long, offset: Int = 0, count: Int = 10): Result<PhotosGetResponseDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(PhotosService().photosGet(ownerId = UserId(userId), albumId = "profile", offset = offset, count = count, extended = true), object : VKApiCallback<PhotosGetResponseDto> {
            override fun success(result: PhotosGetResponseDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getUserVideos(userId: Long, offset: Int = 0, count: Int = 10): Result<VideoGetResponseDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(VideoService().videoGet(ownerId = UserId(userId), offset = offset, count = count, extended = true), object : VKApiCallback<VideoGetResponseDto> {
            override fun success(result: VideoGetResponseDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }
}
