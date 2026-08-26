package com.reforged.client.data.repository

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.AudioApi
import com.reforged.client.data.remote.AudioTrackDto
import com.reforged.client.data.remote.CatalogSectionDto
import com.reforged.client.util.AudioDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val audioApi: AudioApi,
    private val tokenStorage: TokenStorage
) {
    suspend fun getMyMusic(offset: Int = 0, count: Int = 50): Result<List<AudioTrackDto>> {
        val token = tokenStorage.accessToken ?: return Result.failure(Exception("Not authorized"))
        val userId = tokenStorage.userId

        return try {
            val response = audioApi.getAudio(userId, token, offset = offset, count = count)
            if (response.isSuccessful) {
                val wrapper = response.body()
                if (wrapper?.response != null) {
                    val tracks = wrapper.response.items.map { track ->
                        track.copy(url = track.url?.let { AudioDecoder.decode(it, userId) })
                    }
                    Result.success(tracks)
                } else if (wrapper?.error != null) {
                    Result.failure(Exception("VK Error ${wrapper.error.error_code}: ${wrapper.error.error_msg}"))
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
        val token = tokenStorage.accessToken ?: return Result.failure(Exception("Not authorized"))
        val userId = tokenStorage.userId

        return try {
            val response = audioApi.getCatalog(token)
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
                    Result.failure(Exception("VK Error ${wrapper.error.error_code}: ${wrapper.error.error_msg}"))
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
