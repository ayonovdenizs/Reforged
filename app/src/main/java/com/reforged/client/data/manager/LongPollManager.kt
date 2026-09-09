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
    data class NewMessage(
        val messageId: Long,
        val peerId: Long,
        val timestamp: Long,
        val text: String,
        val isOut: Boolean,
        val randomId: Long = 0,
        val cmid: Int = 0,
        val isFull: Boolean = true
    ) : LongPollEvent()
    data class MessageFlags(val messageId: Long, val flags: Int, val peerId: Long) : LongPollEvent()
    data class Typing(val peerId: Long, val userId: Long) : LongPollEvent()
    data class Read(val peerId: Long, val messageId: Long, val isOut: Boolean) : LongPollEvent()
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
                // V=10, need_pts=1
                messagesRepository.getLongPollServer(needPts = 1, lpVersion = 10).onSuccess { params ->
                    var ts = params.ts.toString()
                    val server = params.server
                    val key = params.key

                    while (isActive) {
                        try {
                            // MODE = 2 (Attachments) + 8 (Extended Events) + 128 (random_id) = 138
                            val url = "https://$server?act=a_check&key=$key&ts=$ts&wait=25&mode=138&version=10"
                            val request = Request.Builder().url(url).build()
                            val response = okHttpClient.newCall(request).execute()
                            val body = response.body?.string() ?: break
                            
                            val json = JSONObject(body)
                            if (json.has("failed")) {
                                val failed = json.getInt("failed")
                                when (failed) {
                                    1 -> ts = json.optString("ts", ts)
                                    2, 3 -> break 
                                    4 -> break 
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
                                        2 -> { // Flags set
                                            _events.emit(LongPollEvent.MessageFlags(update.getLong(1), update.getInt(2), update.getLong(3)))
                                        }
                                        3 -> { // Flags reset
                                            _events.emit(LongPollEvent.MessageFlags(update.getLong(1), -update.getInt(2), update.getLong(3)))
                                        }
                                        4 -> { // New message
                                            val messageId = update.getLong(1)
                                            val flags = update.getInt(2)
                                            val peerId = update.getLong(3)
                                            val timestamp = update.getLong(4)
                                            val text = update.getString(5)
                                            val extra = update.optJSONObject(6)
                                            val attachments = update.optJSONObject(7)
                                            val randomId = if (update.length() > 8) update.optLong(8, 0) else 0
                                            val cmid = if (update.length() > 9) update.optInt(9, 0) else 0

                                            val isOut = (flags and 2) != 0
                                            
                                            // Check if message is full (no attachments, no fwd, no reply)
                                            val hasMedia = attachments?.has("attach1_type") == true
                                            val hasFwd = extra?.has("fwd") == true
                                            val hasReply = extra?.has("reply") == true
                                            val isFull = !hasMedia && !hasFwd && !hasReply

                                            _events.emit(LongPollEvent.NewMessage(
                                                messageId = messageId,
                                                peerId = peerId,
                                                timestamp = timestamp,
                                                text = text,
                                                isOut = isOut,
                                                randomId = randomId,
                                                cmid = cmid,
                                                isFull = isFull
                                            ))
                                            _events.emit(LongPollEvent.RefreshConversations)
                                        }
                                        6, 7 -> { // Read incoming/outgoing
                                            _events.emit(LongPollEvent.Read(update.getLong(1), update.getLong(2), type == 7))
                                        }
                                        61, 63 -> { // Typing in PM / Text
                                            val userId = update.getLong(1)
                                            _events.emit(LongPollEvent.Typing(userId, userId))
                                        }
                                        62, 64 -> { // Typing in Chat / Text
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
