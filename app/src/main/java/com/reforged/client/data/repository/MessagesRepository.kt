package com.reforged.client.data.repository

import com.reforged.client.data.local.MessageDao
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.dto.common.id.UserId
import com.vk.sdk.api.docs.DocsService
import com.vk.sdk.api.messages.MessagesService
import com.vk.sdk.api.messages.dto.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.random.Random

@Singleton
class MessagesRepository @Inject constructor(
    private val messageDao: MessageDao
) {

    suspend fun getCachedConversations() = messageDao.getConversations()

    suspend fun getConversations(offset: Int = 0, count: Int = 40): Result<MessagesGetConversationsResponseDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(MessagesService().messagesGetConversations(offset = offset, count = count, extended = true), object : VKApiCallback<MessagesGetConversationsResponseDto> {
            override fun success(result: MessagesGetConversationsResponseDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getHistory(peerId: Long, offset: Int = 0, count: Int = 30): Result<MessagesGetHistoryResponseDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(MessagesService().messagesGetHistory(peerId = UserId(peerId), offset = offset, count = count, extended = true), object : VKApiCallback<MessagesGetHistoryResponseDto> {
            override fun success(result: MessagesGetHistoryResponseDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getConversation(peerId: Long): Result<MessagesGetConversationByIdExtendedDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(MessagesService().messagesGetConversationsByIdExtended(peerIds = listOf(UserId(peerId))), object : VKApiCallback<MessagesGetConversationByIdExtendedDto> {
            override fun success(result: MessagesGetConversationByIdExtendedDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun sendMessage(peerId: Long, text: String, stickerId: Int? = null, attachments: List<String>? = null): Result<Int> = suspendCancellableCoroutine { continuation ->
        val request = MessagesService().messagesSend(
            peerId = UserId(peerId),
            message = text,
            randomId = Random.nextInt(),
            stickerId = stickerId,
            attachment = attachments?.joinToString(",")
        )
        VK.execute(request, object : VKApiCallback<Int> {
            override fun success(result: Int) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getLongPollServer(): Result<MessagesLongpollParamsDto> = suspendCancellableCoroutine { continuation ->
        VK.execute(MessagesService().messagesGetLongPollServer(), object : VKApiCallback<MessagesLongpollParamsDto> {
            override fun success(result: MessagesLongpollParamsDto) {
                continuation.resume(Result.success(result))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun getDocsUploadServer(peerId: Long): Result<String> = suspendCancellableCoroutine { continuation ->
        val request = DocsService().docsGetMessagesUploadServer(type = null, peerId = peerId.toInt())
        VK.execute(request, object : VKApiCallback<com.vk.sdk.api.base.dto.BaseUploadServerDto> {
            override fun success(result: com.vk.sdk.api.base.dto.BaseUploadServerDto) {
                continuation.resume(Result.success(result.uploadUrl))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }

    suspend fun uploadDocument(uploadUrl: String, fileBytes: ByteArray, fileName: String): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .build()
            
            val request = Request.Builder().url(uploadUrl).post(body).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty upload response"))
            
            val json = JSONObject(responseString)
            val file = json.optString("file")
            
            if (file.isNotEmpty()) {
                saveDocument(file)
            } else {
                Result.failure(Exception("Upload failed: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveDocument(file: String): Result<String> = suspendCancellableCoroutine { continuation ->
        VK.execute(DocsService().docsSave(file = file), object : VKApiCallback<com.vk.sdk.api.docs.dto.DocsSaveResponseDto> {
            override fun success(result: com.vk.sdk.api.docs.dto.DocsSaveResponseDto) {
                val doc = result.doc
                continuation.resume(Result.success("doc${doc?.ownerId?.value}_${doc?.id}"))
            }

            override fun fail(error: Exception) {
                continuation.resume(Result.failure(error))
            }
        })
    }
}
