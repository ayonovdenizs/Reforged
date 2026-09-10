package com.reforged.client.data.remote.api

import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
        skipAutoToken: Boolean = false,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, String>? = null,
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

        val response = client.request(baseUrl + path) {
            this.method = method
            
            val allParams = params?.toMutableMap() ?: mutableMapOf()
            
            if (baseUrl.contains("vk.com") || baseUrl.contains("vk.ru")) {
                val version = if (isMusic) "5.119" else "5.199"
                if (!allParams.containsKey("v")) {
                    allParams["v"] = version
                }

                // ⚠️ Auth-эндпоинты (oauth/*, auth.*, ecosystem.*) НЕ должны получать
                // user access_token автоматически — у них свой access_token/anonymous_token.
                // Иначе сервер видит чужой/протухший токен и капча/валидация ломается.
                val isAuthCall = path.startsWith("get_anonym_token") ||
                        path.startsWith("token") ||
                        path.startsWith("auth_by_exchange_token") ||
                        path.startsWith("auth.") ||
                        path.startsWith("ecosystem.")
                if (!skipAutoToken && !isAuthCall) {
                    val token = if (isMusic) tokenStorage.musicAccessToken ?: tokenStorage.accessToken else tokenStorage.accessToken
                    if (!allParams.containsKey("access_token") && token != null) {
                        allParams["access_token"] = token
                    }
                }

                if (!allParams.containsKey("https")) {
                    allParams["https"] = "1"
                }
                // device_id + lang обязательны для auth-флоу
                if (isAuthCall) {
                    if (!allParams.containsKey("lang")) {
                        allParams["lang"] = "ru"
                    }
                }
            }
            
            if (method == HttpMethod.Post) {
                setBody(FormDataContent(Parameters.build {
                    allParams.forEach { (k, v) -> append(k, v) }
                }))
            } else {
                allParams.forEach { (k, v) -> parameter(k, v) }
            }
            
            if (isMusic) {
                header("User-Agent", "VKAndroidApp/8.191-56796 (Android 13; SDK 33; arm64-v8a; Google Pixel 4; ru; 1080x1920)")
            } else if (useAuthAgent) {
                header("User-Agent", "VKAndroidApp/8.191-56796 (Android 13; SDK 33; arm64-v8a; Google Pixel 4; ru; 1080x1920)")
            } else {
                header("User-Agent", "VKAndroidApp/8.191-56796 (Android 13; SDK 33; arm64-v8a; Google Pixel 4; ru; 1080x1920)")
            }
            block()
        }
        android.util.Log.d("VkHttpClient", "Request: ${response.request.method.value} ${response.request.url}, Status: ${response.status}")
        return response
    }

    private fun HttpRequestBuilder.parameterExists(name: String): Boolean {
        return url.parameters.contains(name) || (this.body as? FormDataContent)?.formData?.contains(name) == true
    }

    // --- Auth API ---
    suspend fun getAnonymToken(params: Map<String, String>): String? {
        return try {
            val response: String = safeRequest(
                baseUrl = "https://api.vk.ru/oauth/",
                path = "get_anonym_token",
                useAuthAgent = true,
                method = HttpMethod.Post,
                params = params
            ).bodyAsText()
            
            android.util.Log.d("VkHttpClient", "Anonym Token Response: $response")
            val json = JSONObject(response)
            json.optJSONObject("response")?.optString("token") ?: json.optString("token")
        } catch (e: Exception) {
            android.util.Log.e("VkHttpClient", "Anonym Token Error", e)
            null
        }
    }

    suspend fun validateAccount(params: Map<String, String>): String {
        // Fenrir: passkey_supported + lang обязательны, иначе сервер может отдать капчу/ошибку
        val full = params.toMutableMap()
        if (!full.containsKey("passkey_supported")) full["passkey_supported"] = "0"
        if (!full.containsKey("lang")) full["lang"] = "ru"
        return safeRequest(
            baseUrl = "https://api.vk.ru/method/",
            path = "auth.validateAccount",
            useAuthAgent = true,
            skipAutoToken = true,
            method = HttpMethod.Post,
            params = full
        ).bodyAsText()
    }

    suspend fun getVerificationMethods(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/method/",
        path = "ecosystem.getVerificationMethods",
        useAuthAgent = true,
        method = HttpMethod.Post,
        params = params
    ).bodyAsText()

    suspend fun validatePhone(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/method/",
        path = "auth.validatePhone",
        useAuthAgent = true,
        method = HttpMethod.Post,
        params = params
    ).bodyAsText()

    suspend fun sendEcosystemOtp(suffix: String, params: Map<String, String>): String {
        val methodName = "ecosystem.sendOtp" + suffix.replaceFirstChar { it.uppercase() }
        return safeRequest(
            baseUrl = "https://api.vk.ru/method/",
            path = methodName,
            useAuthAgent = true,
            method = HttpMethod.Post,
            params = params
        ).bodyAsText()
    }

    suspend fun checkEcosystemOtp(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/method/",
        path = "ecosystem.checkOtp",
        useAuthAgent = true,
        method = HttpMethod.Post,
        params = params
    ).bodyAsText()

    suspend fun directLogin(params: Map<String, String>): String {
        // Fenrir IAuthService.directLogin: lang, device_id, libverify_support, 2fa_supported обязательны
        val full = params.toMutableMap()
        if (!full.containsKey("lang")) full["lang"] = "ru"
        if (!full.containsKey("libverify_support")) full["libverify_support"] = "0"
        return safeRequest(
            baseUrl = "https://api.vk.ru/oauth/",
            path = "token",
            useAuthAgent = true,
            skipAutoToken = true,
            method = HttpMethod.Post,
            params = full
        ).bodyAsText()
    }

    suspend fun authByExchangeToken(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/oauth/",
        path = "auth_by_exchange_token",
        useAuthAgent = true,
        method = HttpMethod.Post,
        params = params
    ).bodyAsText()

    suspend fun getExchangeToken(params: Map<String, String>): String = safeRequest(
        baseUrl = "https://api.vk.ru/method/",
        path = "auth.getExchangeToken",
        useAuthAgent = true,
        method = HttpMethod.Post,
        params = params
    ).bodyAsText()

    // --- Newsfeed API ---
    suspend fun getNewsFeed(
        startFrom: String? = null,
        count: Int = 20,
        filters: String = "post,photo,video"
    ): NewsfeedResponseWrapper = safeRequest(
        path = "method/newsfeed.get",
        params = mapOf(
            "start_from" to (startFrom ?: ""),
            "count" to count.toString(),
            "filters" to filters
        )
    ).body()

    // --- Users API ---
    suspend fun getUsers(
        userIds: String,
        fields: String = "photo_200,about,bdate,city,country,followers_count,counters,status,screen_name"
    ): UsersResponseWrapper = safeRequest(
        path = "method/users.get",
        params = mapOf(
            "user_ids" to userIds,
            "fields" to fields
        )
    ).body()

    // --- Messages API ---
    suspend fun getConversations(
        offset: Int = 0,
        count: Int = 40,
        extended: Int = 1
    ): ConversationsResponseWrapper = safeRequest(
        path = "method/messages.getConversations",
        params = mapOf(
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to extended.toString()
        )
    ).body()

    suspend fun getConversationsById(
        peerIds: String,
        extended: Int = 1
    ): ConversationsByIdResponseWrapper = safeRequest(
        path = "method/messages.getConversationsById",
        params = mapOf(
            "peer_ids" to peerIds,
            "extended" to extended.toString()
        )
    ).body()

    suspend fun getHistory(
        peerId: Long,
        offset: Int = 0,
        count: Int = 30,
        extended: Int = 1
    ): HistoryResponseWrapper = safeRequest(
        path = "method/messages.getHistory",
        params = mapOf(
            "peer_id" to peerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to extended.toString()
        )
    ).body()

    suspend fun sendMessage(
        peerId: Long,
        randomId: Int,
        message: String? = null,
        stickerId: Int? = null,
        attachment: String? = null
    ): SendMessageResponseWrapper = safeRequest(
        path = "method/messages.send",
        params = mutableMapOf(
            "peer_id" to peerId.toString(),
            "random_id" to randomId.toString()
        ).apply {
            message?.let { put("message", it) }
            stickerId?.let { put("sticker_id", it.toString()) }
            attachment?.let { put("attachment", it) }
        }
    ).body()

    suspend fun getLongPollServer(
        needPts: Int = 1,
        lpVersion: Int = 3
    ): LongPollServerResponseWrapper = safeRequest(
        path = "method/messages.getLongPollServer",
        params = mapOf(
            "need_pts" to needPts.toString(),
            "lp_version" to lpVersion.toString()
        )
    ).body()

    suspend fun getDocsUploadServer(
        type: String? = "doc",
        peerId: Long
    ): UploadServerResponseWrapper = safeRequest(
        path = "method/docs.getMessagesUploadServer",
        params = mapOf(
            "type" to (type ?: "doc"),
            "peer_id" to peerId.toString()
        )
    ).body()

    suspend fun saveDoc(
        file: String,
        title: String? = null,
        tags: String? = null
    ): SaveDocResponseWrapper = safeRequest(
        path = "method/docs.save",
        params = mutableMapOf(
            "file" to file
        ).apply {
            title?.let { put("title", it) }
            tags?.let { put("tags", it) }
        }
    ).body()

    suspend fun markAsRead(peerId: Long): BaseOkResponseWrapper = safeRequest(
        path = "method/messages.markAsRead",
        params = mapOf("peer_id" to peerId.toString())
    ).body()

    suspend fun getMessagesById(messageIds: String): String = safeRequest(
        path = "method/messages.getById",
        params = mapOf(
            "message_ids" to messageIds,
            "extended" to "1"
        )
    ).bodyAsText()

    // --- Wall API ---
    suspend fun getWall(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 20,
        extended: Int = 1
    ): WallResponseWrapper = safeRequest(
        path = "method/wall.get",
        params = mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to extended.toString()
        )
    ).body()

    // --- Photos API ---
    suspend fun getPhotos(
        ownerId: Long,
        albumId: String = "profile",
        offset: Int = 0,
        count: Int = 10,
        extended: Int = 1
    ): PhotosResponseWrapper = safeRequest(
        path = "method/photos.get",
        params = mapOf(
            "owner_id" to ownerId.toString(),
            "album_id" to albumId,
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to extended.toString()
        )
    ).body()

    // --- Video API ---
    suspend fun getVideos(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 10,
        extended: Int = 1
    ): VideoResponseWrapper = safeRequest(
        path = "method/video.get",
        params = mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to extended.toString()
        )
    ).body()

    // --- Audio API (BOOM) ---
    suspend fun getAudio(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 100
    ): AudioResponseWrapper = safeRequest(
        path = "method/audio.get",
        isMusic = true,
        params = mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).body()

    suspend fun getAudioCatalog(
        extended: Int = 1,
        sectionId: String? = null,
        startFrom: String? = null
    ): CatalogResponseWrapper = safeRequest(
        path = "method/audio.getCatalog",
        isMusic = true,
        params = mutableMapOf(
            "extended" to extended.toString()
        ).apply {
            sectionId?.let { put("section_id", it) }
            startFrom?.let { put("start_from", it) }
        }
    ).body()

    suspend fun getAudioRecommendations(
        count: Int = 10,
        offset: Int = 0
    ): AudioResponseWrapper = safeRequest(
        path = "method/audio.getRecommendations",
        isMusic = true,
        params = mapOf(
            "count" to count.toString(),
            "offset" to offset.toString()
        )
    ).body()

    suspend fun searchAudio(
        query: String,
        offset: Int = 0,
        count: Int = 50
    ): AudioResponseWrapper = safeRequest(
        path = "method/audio.search",
        isMusic = true,
        params = mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).body()

    suspend fun getAudioPlaylists(
        ownerId: Long,
        offset: Int = 0,
        count: Int = 50
    ): PlaylistsResponseWrapper = safeRequest(
        path = "method/audio.getPlaylists",
        isMusic = true,
        params = mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString()
        )
    ).body()

    suspend fun getAudioPlaylistById(
        ownerId: Long,
        playlistId: Long,
        accessKey: String? = null
    ): PlaylistResponseWrapper = safeRequest(
        path = "method/audio.getPlaylistById",
        isMusic = true,
        params = mutableMapOf(
            "owner_id" to ownerId.toString(),
            "playlist_id" to playlistId.toString()
        ).apply {
            accessKey?.let { put("access_key", it) }
        }
    ).body()

    suspend fun getAudioById(
        audios: String
    ): AudioListResponseWrapper = safeRequest(
        path = "method/audio.getById",
        isMusic = true,
        params = mapOf("audios" to audios)
    ).body()

    suspend fun addAudio(
        audioId: Long,
        ownerId: Long
    ): BaseOkResponseWrapper = safeRequest(
        path = "method/audio.add",
        isMusic = true,
        params = mapOf(
            "audio_id" to audioId.toString(),
            "owner_id" to ownerId.toString()
        )
    ).body()

    suspend fun deleteAudio(
        audioId: Long,
        ownerId: Long
    ): BaseOkResponseWrapper = safeRequest(
        path = "method/audio.delete",
        isMusic = true,
        params = mapOf(
            "audio_id" to audioId.toString(),
            "owner_id" to ownerId.toString()
        )
    ).body()

    // --- Badge API ---
    suspend fun getUserBadges(vkId: Long): BadgeResponse = safeRequest(
        baseUrl = "https://pyminelauncher.vercel.app/",
        path = "api_vtlr/users/$vkId/badges/"
    ).body()
}
