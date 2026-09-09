package com.reforged.client.data.remote.api

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VkHttpClient @Inject constructor(
    private val tokenStorage: TokenStorage
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private val minRequestInterval = 350L

    /**
     * Executes a safe request to VK API with rate limiting.
     */
    private suspend fun safeRequest(
        baseUrl: String = "https://api.vk.com/",
        path: String,
        isMusic: Boolean = false,
        useAuthAgent: Boolean = false,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        // Rate Limiting Logic
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRequest = currentTime - lastRequestTime
            if (timeSinceLastRequest < minRequestInterval) {
                delay(minRequestInterval - timeSinceLastRequest)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        return client.get(baseUrl + path) {
            if (baseUrl.contains("vk.com") || baseUrl.contains("vk.ru")) {
                parameter("v", if (isMusic) "5.119" else "5.199")
                val token = if (isMusic) tokenStorage.musicAccessToken ?: tokenStorage.accessToken else tokenStorage.accessToken
                if (!parameterExists("access_token")) {
                    parameter("access_token", token)
                }
            }
            if (isMusic) {
                header("User-Agent", "VKMusic/2.1.2 (Android 11; SDK 30; arm64-v8a; Google Pixel 4; ru)")
            } else if (useAuthAgent) {
                header("User-Agent", "VKAndroidApp/8.191-56796 (Android 13; SDK 33; arm64-v8a; Google Pixel 4; ru; 1080x1920)")
            } else {
                header("User-Agent", "VKAndroidApp/8.5-14400 (Android 13; SDK 33; arm64-v8a; Xiaomi; ru; 2340x1080)")
            }
            block()
        }
    }

    private fun HttpRequestBuilder.parameterExists(name: String): Boolean {
        return url.parameters.contains(name)
    }

    // --- Auth API ---
    suspend fun getAnonymToken(params: Map<String, String>): String? {
        val response: String = safeRequest(
            baseUrl = "https://api.vk.ru/oauth/",
            path = "get_anonym_token",
            useAuthAgent = true
        ) {
            params.forEach { (key, value) -> parameter(key, value) }
        }.bodyAsText()
        
        return try {
            JSONObject(response).optJSONObject("response")?.optString("token")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun validateAccount(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.com/",
        path = "method/auth.validateAccount",
        useAuthAgent = true
    ) {
        params.forEach { (key, value) -> parameter(key, value) }
    }.bodyAsText()

    suspend fun directLogin(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/oauth/",
        path = "token",
        useAuthAgent = true
    ) {
        params.forEach { (key, value) -> parameter(key, value) }
    }.bodyAsText()

    suspend fun authByExchangeToken(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/oauth/",
        path = "auth_by_exchange_token",
        useAuthAgent = true
    ) {
        params.forEach { (key, value) -> parameter(key, value) }
    }.bodyAsText()

    // --- Newsfeed API ---
    suspend fun getNewsFeed(
        startFrom: String? = null,
        count: Int = 20,
        filters: String = "post,photo,video"
    ): NewsfeedResponseWrapper = safeRequest(path = "method/newsfeed.get") {
        parameter("start_from", startFrom)
        parameter("count", count)
        parameter("filters", filters)
    }.body()

    // --- Users API ---
    suspend fun getUsers(
        userIds: String,
        fields: String = "photo_200,about,bdate,city,country,followers_count,counters,status,screen_name"
    ): UsersResponseWrapper = safeRequest(path = "method/users.get") {
        parameter("user_ids", userIds)
        parameter("fields", fields)
    }.body()

    // --- Messages API ---
    suspend fun getConversations(
        offset: Int = 0,
        count: Int = 40,
        extended: Int = 1
    ): ConversationsResponseWrapper = safeRequest(path = "method/messages.getConversations") {
        parameter("offset", offset)
        parameter("count", count)
        parameter("extended", extended)
    }.body()

    suspend fun getConversationsById(
        peerIds: String,
        extended: Int = 1
    ): ConversationsByIdResponseWrapper = safeRequest(path = "method/messages.getConversationsById") {
        parameter("peer_ids", peerIds)
        parameter("extended", extended)
    }.body()

    suspend fun getHistory(
        peerId: Long,
        offset: Int = 0,
        count: Int = 30,
        extended: Int = 1
    ): HistoryResponseWrapper = safeRequest(path = "method/messages.getHistory") {
        parameter("peer_id", peerId)
        parameter("offset", offset)
        parameter("count", count)
        parameter("extended", extended)
    }.body()

    suspend fun sendMessage(
        peerId: Long,
        randomId: Int,
        message: String? = null,
        stickerId: Int? = null,
        attachment: String? = null
    ): SendMessageResponseWrapper = safeRequest(path = "method/messages.send") {
        parameter("peer_id", peerId)
        parameter("random_id", randomId)
        parameter("message", message)
        parameter("sticker_id", stickerId)
        parameter("attachment", attachment)
    }.body()

    suspend fun getLongPollServer(
        needPts: Int = 1,
        lpVersion: Int = 3
    ): LongPollServerResponseWrapper = safeRequest(path = "method/messages.getLongPollServer") {
        parameter("need_pts", needPts)
        parameter("lp_version", lpVersion)
    }.body()

    suspend fun getDocsUploadServer(
        type: String? = "doc",
        peerId: Long
    ): UploadServerResponseWrapper = safeRequest(path = "method/docs.getMessagesUploadServer") {
        parameter("type", type)
        parameter("peer_id", peerId)
    }.body()

    suspend fun saveDoc(
        file: String,
        title: String? = null,
        tags: String? = null
    ): SaveDocResponseWrapper = safeRequest(path = "method/docs.save") {
        parameter("file", file)
        parameter("title", title)
        parameter("tags", tags)
    }.body()

    suspend fun markAsRead(peerId: Long): BaseOkResponseWrapper = safeRequest(path = "method/messages.markAsRead") {
        parameter("peer_id", peerId)
    }.body()

    suspend fun getMessagesById(messageIds: String): String = safeRequest(path = "method/messages.getById") {
        parameter("message_ids", messageIds)
        parameter("extended", 1)
    }.bodyAsText()

    // --- Wall API ---
    suspend fun getWall(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 20,
        extended: Int = 1
    ): WallResponseWrapper = safeRequest(path = "method/wall.get") {
        parameter("owner_id", ownerId)
        parameter("offset", offset)
        parameter("count", count)
        parameter("extended", extended)
    }.body()

    // --- Photos API ---
    suspend fun getPhotos(
        ownerId: Long,
        albumId: String = "profile",
        offset: Int = 0,
        count: Int = 10,
        extended: Int = 1
    ): PhotosResponseWrapper = safeRequest(path = "method/photos.get") {
        parameter("owner_id", ownerId)
        parameter("album_id", albumId)
        parameter("offset", offset)
        parameter("count", count)
        parameter("extended", extended)
    }.body()

    // --- Video API ---
    suspend fun getVideos(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 10,
        extended: Int = 1
    ): VideoResponseWrapper = safeRequest(path = "method/video.get") {
        parameter("owner_id", ownerId)
        parameter("offset", offset)
        parameter("count", count)
        parameter("extended", extended)
    }.body()

    // --- Audio API (BOOM) ---
    suspend fun getAudio(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 100
    ): AudioResponseWrapper = safeRequest(path = "method/audio.get", isMusic = true) {
        parameter("owner_id", ownerId)
        parameter("offset", offset)
        parameter("count", count)
    }.body()

    suspend fun getAudioCatalog(
        extended: Int = 1,
        sectionId: String? = null,
        startFrom: String? = null
    ): CatalogResponseWrapper = safeRequest(path = "method/audio.getCatalog", isMusic = true) {
        parameter("extended", extended)
        parameter("section_id", sectionId)
        parameter("start_from", startFrom)
    }.body()

    suspend fun getAudioRecommendations(
        count: Int = 10,
        offset: Int = 0
    ): AudioResponseWrapper = safeRequest(path = "method/audio.getRecommendations", isMusic = true) {
        parameter("count", count)
        parameter("offset", offset)
    }.body()

    suspend fun searchAudio(
        query: String,
        offset: Int = 0,
        count: Int = 50
    ): AudioResponseWrapper = safeRequest(path = "method/audio.search", isMusic = true) {
        parameter("q", query)
        parameter("offset", offset)
        parameter("count", count)
    }.body()

    suspend fun getAudioPlaylists(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 50
    ): PlaylistsResponseWrapper = safeRequest(path = "method/audio.getPlaylists", isMusic = true) {
        parameter("owner_id", ownerId)
        parameter("offset", offset)
        parameter("count", count)
    }.body()

    suspend fun getAudioPlaylistById(
        ownerId: Long,
        playlistId: Long,
        accessKey: String? = null
    ): PlaylistResponseWrapper = safeRequest(path = "method/audio.getPlaylistById", isMusic = true) {
        parameter("owner_id", ownerId)
        parameter("playlist_id", playlistId)
        parameter("access_key", accessKey)
    }.body()

    suspend fun getAudioById(
        audios: String
    ): AudioListResponseWrapper = safeRequest(path = "method/audio.getById", isMusic = true) {
        parameter("audios", audios)
    }.body()

    suspend fun addAudio(
        audioId: Long,
        ownerId: Long
    ): BaseOkResponseWrapper = safeRequest(path = "method/audio.add", isMusic = true) {
        parameter("audio_id", audioId)
        parameter("owner_id", ownerId)
    }.body()

    suspend fun deleteAudio(
        audioId: Long,
        ownerId: Long
    ): BaseOkResponseWrapper = safeRequest(path = "method/audio.delete", isMusic = true) {
        parameter("audio_id", audioId)
        parameter("owner_id", ownerId)
    }.body()

    // --- Badge API ---
    suspend fun getUserBadges(vkId: Long): BadgeResponse = safeRequest(
        baseUrl = "https://pyminelauncher.vercel.app/",
        path = "api_vtlr/users/$vkId/badges/"
    ).body()
}
