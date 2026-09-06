package com.reforged.client.data.repository

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.data.remote.api.VKService
import com.reforged.client.util.AudioDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val vkService: VKService,
    private val tokenStorage: TokenStorage
) {
    suspend fun getMyMusic(offset: Int = 0, count: Int = 50): Result<List<AudioTrackDto>> {
        val userId = tokenStorage.userId

        return try {
            val response = vkService.getAudio(userId, offset = offset, count = count)
            if (response.isSuccessful) {
                val wrapper = response.body()
                if (wrapper?.response != null) {
                    val tracks = wrapper.response.items.map { track ->
                        track.copy(url = track.url?.let { AudioDecoder.decode(it, userId) })
                    }
                    Result.success(tracks)
                } else if (wrapper?.error != null) {
                    Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
                } else {
                    Result.failure(Exception("Unknown response structure"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCatalog(): Result<List<CatalogSectionDto>> {
        val userId = tokenStorage.userId

        return try {
            val response = vkService.getCatalog()
            if (response.isSuccessful) {
                val wrapper = response.body()
                if (wrapper?.response != null) {
                    val sections = wrapper.response.items.map { section ->
                        section.copy(
                            audios = section.audios?.map { it.copy(url = it.url?.let { url -> AudioDecoder.decode(url, userId) }) }
                        )
                    }
                    Result.success(sections)
                } else if (wrapper?.error != null) {
                    Result.failure(Exception("VK Error ${wrapper.error.errorCode}: ${wrapper.error.errorMsg}"))
                } else {
                    Result.failure(Exception("Unknown response structure"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
