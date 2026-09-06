package com.reforged.client.data.repository

import com.reforged.client.data.local.MessageDao
import com.reforged.client.data.remote.*
import com.reforged.client.data.remote.api.VKService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val vkService: VKService,
    private val messageDao: MessageDao,
    private val okHttpClient: OkHttpClient
) {

    suspend fun getCachedConversations() = messageDao.getConversations()

    suspend fun getConversations(offset: Int = 0, count: Int = 40): Result<ConversationsResponse> {
        return try {
            val response = vkService.getConversations(offset = offset, count = count)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHistory(peerId: Long, offset: Int = 0, count: Int = 30): Result<HistoryResponse> {
        return try {
            val response = vkService.getHistory(peerId = peerId, offset = offset, count = count)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversation(peerId: Long): Result<List<ConversationDto>> = withContext(Dispatchers.IO) {
        try {
            val response = vkService.getConversationsById(peerIds = peerId.toString())
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(peerId: Long, text: String, stickerId: Int? = null, attachments: List<String>? = null): Result<Int> {
        return try {
            val response = vkService.sendMessage(
                peerId = peerId,
                randomId = Random.nextInt(),
                message = text.ifEmpty { null },
                stickerId = stickerId,
                attachment = attachments?.joinToString(",")
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLongPollServer(): Result<LongPollParamsDto> {
        return try {
            val response = vkService.getLongPollServer()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocsUploadServer(peerId: Long): Result<String> {
        return try {
            val response = vkService.getDocsUploadServer(peerId = peerId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response.uploadUrl)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
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
            val response = vkService.saveDoc(file = file)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    val doc = body.response.doc
                    Result.success("doc${doc?.ownerId}_${doc?.id}")
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(peerId: Long): Result<Int> {
        return try {
            val response = vkService.markAsRead(peerId = peerId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Result.success(body.response)
                } else {
                    Result.failure(Exception("VK Error: ${body?.error?.errorMsg}"))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
