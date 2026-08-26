package com.reforged.client.data.manager

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.reforged.client.data.repository.MessagesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class LongPollEvent {
    data class NewMessage(val peerId: Long, val messageId: Long) : LongPollEvent()
    data class Typing(val peerId: Long, val userId: Long) : LongPollEvent()
    object RefreshConversations : LongPollEvent()
}

@Singleton
class LongPollManager @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val okHttpClient: OkHttpClient
) {
    private val _events = MutableSharedFlow<LongPollEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<LongPollEvent> = _events

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                messagesRepository.getLongPollServer().onSuccess { params ->
                    var ts = params.ts.toString()
                    val server = params.server
                    val key = params.key

                    while (isActive) {
                        try {
                            val url = "https://$server?act=a_check&key=$key&ts=$ts&wait=25&mode=2&version=3"
                            val request = Request.Builder().url(url).build()
                            val response = okHttpClient.newCall(request).execute()
                            val body = response.body?.string() ?: break
                            
                            val json = JSONObject(body)
                            if (json.has("failed")) {
                                val failed = json.getInt("failed")
                                when (failed) {
                                    1 -> ts = json.optString("ts", ts) // History outdated, use new ts
                                    2, 3 -> break // Key or Server expired
                                    4 -> break // Version expired
                                }
                                continue 
                            }
                            
                            ts = json.optString("ts", ts)
                            val updates = json.optJSONArray("updates")
                            if (updates != null) {
                                for (i in 0 until updates.length()) {
                                    val update = updates.getJSONArray(i)
                                    val type = update.getInt(0)
                                    
                                    when (type) {
                                        4 -> { // New message
                                            val messageId = update.getLong(1)
                                            val peerId = update.getLong(3)
                                            _events.emit(LongPollEvent.NewMessage(peerId, messageId))
                                            _events.emit(LongPollEvent.RefreshConversations)
                                        }
                                        61 -> { // Typing in PM
                                            val userId = update.getLong(1)
                                            _events.emit(LongPollEvent.Typing(userId, userId))
                                        }
                                        62 -> { // Typing in Chat
                                            val userId = update.getLong(1)
                                            val chatId = update.getLong(2)
                                            _events.emit(LongPollEvent.Typing(2000000000L + chatId, userId))
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            FirebaseCrashlytics.getInstance().recordException(e)
                            delay(5000)
                            break
                        }
                    }
                }.onFailure { e ->
                    FirebaseCrashlytics.getInstance().recordException(e)
                    delay(10000)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
