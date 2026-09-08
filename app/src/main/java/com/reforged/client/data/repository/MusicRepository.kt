package com.reforged.client.data.repository

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import com.reforged.client.data.remote.api.VkHttpClient
import com.reforged.client.util.AudioDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun getMyMusic(offset: Int = 0, count: Int = 50): Result<List<AudioTrackDto>> {
        val userId = tokenStorage.userId

        return try {
            val wrapper = vkHttpClient.getAudio(userId, offset = offset, count = count)
            if (wrapper.response != null) {
                val tracks = wrapper.response.items.map { track ->
                    track.copy(url = track.url?.let { AudioDecoder.decode(it, userId) })
                }
                Result.success(tracks)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCatalog(sectionId: String? = null, startFrom: String? = null): Result<List<CatalogSectionDto>> {
        val userId = tokenStorage.userId

        return try {
            val wrapper = vkHttpClient.getAudioCatalog(sectionId = sectionId, startFrom = startFrom)
            if (wrapper.response != null) {
                val sections = wrapper.response.items.map { section ->
                    section.copy(
                        audios = section.audios?.map { it.copy(url = it.url?.let { url -> AudioDecoder.decode(url, userId) }) }
                    )
                }
                Result.success(sections)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchAudio(query: String, offset: Int = 0, count: Int = 50): Result<List<AudioTrackDto>> {
        val userId = tokenStorage.userId
        return try {
            val wrapper = vkHttpClient.searchAudio(query, offset, count)
            if (wrapper.response != null) {
                val tracks = wrapper.response.items.map { track ->
                    track.copy(url = track.url?.let { AudioDecoder.decode(it, userId) })
                }
                Result.success(tracks)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecommendations(count: Int = 10, offset: Int = 0): Result<List<AudioTrackDto>> {
        val userId = tokenStorage.userId
        return try {
            val wrapper = vkHttpClient.getAudioRecommendations(count, offset)
            if (wrapper.response != null) {
                val tracks = wrapper.response.items.map { track ->
                    track.copy(url = track.url?.let { AudioDecoder.decode(it, userId) })
                }
                Result.success(tracks)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addAudio(audioId: Long, ownerId: Long): Result<Int> {
        return try {
            val wrapper = vkHttpClient.addAudio(audioId, ownerId)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAudio(audioId: Long, ownerId: Long): Result<Int> {
        return try {
            val wrapper = vkHttpClient.deleteAudio(audioId, ownerId)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Unknown response structure"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
