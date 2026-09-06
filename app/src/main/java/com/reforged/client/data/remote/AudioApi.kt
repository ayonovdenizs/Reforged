package com.reforged.client.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AudioApi {
    @GET("method/audio.get")
    suspend fun getAudio(
        @Query("owner_id") ownerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 100
    ): Response<AudioResponseWrapper>

    @GET("method/audio.getCatalog")
    suspend fun getCatalog(
        @Query("extended") extended: Int = 1,
        @Query("section_id") sectionId: String? = null,
        @Query("start_from") startFrom: String? = null
    ): Response<CatalogResponseWrapper>

    @GET("method/audio.getRecommendations")
    suspend fun getRecommendations(
        @Query("count") count: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<AudioResponseWrapper>

    @GET("method/audio.search")
    suspend fun search(
        @Query("q") query: String,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 50
    ): Response<AudioResponseWrapper>

    @GET("method/audio.getPlaylists")
    suspend fun getPlaylists(
        @Query("owner_id") ownerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 50
    ): Response<PlaylistsResponseWrapper>

    @GET("method/audio.getPlaylistById")
    suspend fun getPlaylistById(
        @Query("owner_id") ownerId: Long,
        @Query("playlist_id") playlistId: Long,
        @Query("access_key") accessKey: String? = null
    ): Response<PlaylistResponseWrapper>

    @GET("method/audio.getById")
    suspend fun getById(
        @Query("audios") audios: String // ownerId_audioId
    ): Response<AudioListResponseWrapper>

    @GET("method/audio.add")
    suspend fun add(
        @Query("audio_id") audioId: Long,
        @Query("owner_id") ownerId: Long
    ): Response<BaseOkResponseWrapper>

    @GET("method/audio.delete")
    suspend fun delete(
        @Query("audio_id") audioId: Long,
        @Query("owner_id") ownerId: Long
    ): Response<BaseOkResponseWrapper>
}

data class AudioResponseWrapper(
    val response: AudioResponse? = null,
    val error: VkError? = null
)

data class AudioResponse(
    val count: Int,
    val items: List<AudioTrackDto>
)

data class AudioListResponseWrapper(
    val response: List<AudioTrackDto>? = null,
    val error: VkError? = null
)

data class PlaylistsResponseWrapper(
    val response: PlaylistsResponse? = null,
    val error: VkError? = null
)

data class PlaylistsResponse(
    val count: Int,
    val items: List<PlaylistDto>
)

data class PlaylistResponseWrapper(
    val response: PlaylistDto? = null,
    val error: VkError? = null
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
    val photo: AudioPhotoDto? = null,
    val count: Int
)

data class AudioPhotoDto(
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
    val thumb: AudioPhotoDto? = null,
    val title: String?
)


