package com.reforged.client.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AudioApi {
    @GET("method/audio.get")
    suspend fun getAudio(
        @Query("owner_id") ownerId: Long,
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199",
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 100
    ): Response<AudioResponseWrapper>

    @GET("method/audio.getCatalog")
    suspend fun getCatalog(
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199",
        @Query("extended") extended: Int = 1
    ): Response<CatalogResponseWrapper>

    @GET("method/audio.getRecommendations")
    suspend fun getRecommendations(
        @Query("access_token") token: String,
        @Query("v") version: String = "5.199",
        @Query("count") count: Int = 10
    ): Response<AudioResponseWrapper>
}

data class AudioResponseWrapper(
    val response: AudioResponse? = null,
    val error: VkError? = null
)

data class AudioResponse(
    val count: Int,
    val items: List<AudioTrackDto>
)

data class CatalogResponseWrapper(
    val response: CatalogResponse? = null,
    val error: VkError? = null
)

data class CatalogResponse(
    val items: List<CatalogSectionDto>
)

data class CatalogSectionDto(
    val id: String?,
    val title: String?,
    val type: String?, // "list", "blocks", etc.
    val items: List<CatalogItemDto>? = null,
    val playlists: List<PlaylistDto>? = null,
    val audios: List<AudioTrackDto>? = null
)

data class CatalogItemDto(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val type: String?, // "playlist", "audio", etc.
    val playlist: PlaylistDto? = null,
    val audio: AudioTrackDto? = null
)

data class PlaylistDto(
    val id: Long,
    val owner_id: Long,
    val title: String,
    val description: String?,
    val photo: PhotoDto? = null,
    val count: Int
)

data class PhotoDto(
    val photo_300: String?,
    val photo_600: String?,
    val photo_1200: String?
)

data class AudioTrackDto(
    val id: Long,
    val owner_id: Long,
    val artist: String,
    val title: String,
    val duration: Int,
    val url: String?,
    val track_code: String? = null,
    val ads: Any? = null,
    val album: AlbumDto? = null
)

data class AlbumDto(
    val id: Long,
    val thumb: PhotoDto? = null,
    val title: String?
)

data class VkError(
    val error_code: Int,
    val error_msg: String
)
