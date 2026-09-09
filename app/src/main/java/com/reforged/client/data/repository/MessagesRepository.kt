package com.reforged.client.data.repository

import com.reforged.client.data.local.MessageDao
import com.reforged.client.data.remote.*
import com.reforged.client.data.remote.api.VkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MessagesRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient,
    private val messageDao: MessageDao,
    private val okHttpClient: OkHttpClient
) {

    suspend fun getCachedConversations() = messageDao.getConversations()

    suspend fun getConversations(offset: Int = 0, count: Int = 40): Result<ConversationsResponse> {
        return try {
            val wrapper = vkHttpClient.getConversations(offset = offset, count = count)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Conversations not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistory(peerId: Long, offset: Int = 0, count: Int = 30): Result<HistoryResponse> {
        return try {
            val wrapper = vkHttpClient.getHistory(peerId = peerId, offset = offset, count = count)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("History not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageById(messageId: Long): Result<MessageDto> {
        return try {
            val responseString = vkHttpClient.getMessagesById(messageId.toString())
            val jsonResponse = JSONObject(responseString).optJSONObject("response")
            val items = jsonResponse?.optJSONArray("items")
            if (items != null && items.length() > 0) {
                Result.success(Json { ignoreUnknownKeys = true }.decodeFromString<MessageDto>(items.getJSONObject(0).toString()))
            } else {
                Result.failure(Exception("Message not found: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversation(peerId: Long): Result<List<ConversationDto>> = withContext(Dispatchers.IO) {
        try {
            val wrapper = vkHttpClient.getConversationsById(peerIds = peerId.toString())
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Conversation not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        peerId: Long,
        text: String,
        stickerId: Int? = null,
        attachments: List<String>? = null,
        randomId: Int = Random.nextInt()
    ): Result<Int> {
        return try {
            val wrapper = vkHttpClient.sendMessage(
                peerId = peerId,
                randomId = randomId,
                message = text.ifEmpty { null },
                stickerId = stickerId,
                attachment = attachments?.joinToString(",")
            )
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Send failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLongPollServer(needPts: Int = 1, lpVersion: Int = 3): Result<LongPollParamsDto> {
        return try {
            val wrapper = vkHttpClient.getLongPollServer(needPts = needPts, lpVersion = lpVersion)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("LongPoll params not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocsUploadServer(peerId: Long): Result<String> {
        return try {
            val wrapper = vkHttpClient.getDocsUploadServer(peerId = peerId)
            if (wrapper.response != null) {
                Result.success(wrapper.response.uploadUrl)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Upload server not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(uploadUrl: String, fileBytes: ByteArray, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
                .build()
            
            val request = Request.Builder().url(uploadUrl).post(body).build()
            val response = okHttpClient.newCall(request).execute()
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

    private suspend fun saveDocument(file: String): Result<String> {
        return try {
            val wrapper = vkHttpClient.saveDoc(file = file)
            if (wrapper.response != null) {
                val doc = wrapper.response.doc
                Result.success("doc${doc?.ownerId}_${doc?.id}")
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Save failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(peerId: Long): Result<Int> {
        return try {
            val wrapper = vkHttpClient.markAsRead(peerId = peerId)
            if (wrapper.response != null) {
                Result.success(wrapper.response)
            } else if (wrapper.error != null) {
                Result.failure(Exception("VK Error: ${wrapper.error.errorMsg}"))
            } else {
                Result.failure(Exception("Mark as read failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
