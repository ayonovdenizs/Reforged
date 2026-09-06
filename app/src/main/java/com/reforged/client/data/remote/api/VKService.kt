package com.reforged.client.data.remote.api

import com.reforged.client.data.remote.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface VKService {

    @GET("method/audio.get")
    suspend fun getAudio(
        @Query("owner_id") ownerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 100
    ): Response<AudioResponseWrapper>

    @GET("method/audio.getCatalog")
    suspend fun getCatalog(
        @Query("extended") extended: Int = 1
    ): Response<CatalogResponseWrapper>

    @GET("method/audio.getRecommendations")
    suspend fun getRecommendations(
        @Query("count") count: Int = 10
    ): Response<AudioResponseWrapper>

    @GET("method/newsfeed.get")
    suspend fun getNewsFeed(
        @Query("start_from") startFrom: String? = null,
        @Query("count") count: Int = 20,
        @Query("filters") filters: String = "post,photo,video"
    ): Response<NewsfeedResponseWrapper>

    @GET("method/users.get")
    suspend fun getUsers(
        @Query("user_ids") userIds: String,
        @Query("fields") fields: String = "photo_200,about,bdate,city,country,followers_count,counters,status,screen_name"
    ): Response<UsersResponseWrapper>

    @GET("method/messages.getConversations")
    suspend fun getConversations(
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 40,
        @Query("extended") extended: Int = 1
    ): Response<ConversationsResponseWrapper>

    @GET("method/messages.getConversationsById")
    suspend fun getConversationsById(
        @Query("peer_ids") peerIds: String,
        @Query("extended") extended: Int = 1
    ): Response<ConversationsByIdResponseWrapper>

    @GET("method/messages.getHistory")
    suspend fun getHistory(
        @Query("peer_id") peerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 30,
        @Query("extended") extended: Int = 1
    ): Response<HistoryResponseWrapper>

    @GET("method/messages.send")
    suspend fun sendMessage(
        @Query("peer_id") peerId: Long,
        @Query("random_id") randomId: Int,
        @Query("message") message: String? = null,
        @Query("sticker_id") stickerId: Int? = null,
        @Query("attachment") attachment: String? = null
    ): Response<SendMessageResponseWrapper>

    @GET("method/messages.getLongPollServer")
    suspend fun getLongPollServer(
        @Query("need_pts") needPts: Int = 1,
        @Query("lp_version") lpVersion: Int = 3
    ): Response<LongPollServerResponseWrapper>

    @GET("method/docs.getMessagesUploadServer")
    suspend fun getDocsUploadServer(
        @Query("type") type: String? = "doc",
        @Query("peer_id") peerId: Long
    ): Response<UploadServerResponseWrapper>

    @GET("method/docs.save")
    suspend fun saveDoc(
        @Query("file") file: String,
        @Query("title") title: String? = null,
        @Query("tags") tags: String? = null
    ): Response<SaveDocResponseWrapper>

    @GET("method/messages.markAsRead")
    suspend fun markAsRead(
        @Query("peer_id") peerId: Long
    ): Response<BaseOkResponseWrapper>

    @GET("method/wall.get")
    suspend fun getWall(
        @Query("owner_id") ownerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 20,
        @Query("extended") extended: Int = 1
    ): Response<WallResponseWrapper>

    @GET("method/photos.get")
    suspend fun getPhotos(
        @Query("owner_id") ownerId: Long,
        @Query("album_id") albumId: String = "profile",
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 10,
        @Query("extended") extended: Int = 1
    ): Response<PhotosResponseWrapper>

    @GET("method/video.get")
    suspend fun getVideos(
        @Query("owner_id") ownerId: Long,
        @Query("offset") offset: Int = 0,
        @Query("count") count: Int = 10,
        @Query("extended") extended: Int = 1
    ): Response<VideoResponseWrapper>
}


data class UsersResponseWrapper(
    val response: List<UserDto>? = null,
    val error: VkError? = null
)
