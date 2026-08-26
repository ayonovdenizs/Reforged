package com.reforged.client.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.reforged.client.data.repository.MessagesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class LongPollService : Service() {

    @Inject
    lateinit var messagesRepository: MessagesRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startLongPoll()
        }
        return START_STICKY
    }

    private fun startLongPoll() {
        serviceScope.launch {
            while (isActive) {
                val serverResult = messagesRepository.getLongPollServer()
                serverResult.onSuccess { params ->
                    var currentTs = params.ts.toString()
                    val server = params.server
                    val key = params.key

                    while (isActive) {
                        try {
                            val url = "https://$server?act=a_check&key=$key&ts=$currentTs&wait=25&mode=2&version=3"
                            val request = Request.Builder().url(url).build()
                            val response = okHttpClient.newCall(request).execute()
                            val bodyText = response.body?.string()
                            
                            if (bodyText != null) {
                                val json = JSONObject(bodyText)
                                if (json.has("failed")) {
                                    break // Re-fetch server params
                                }
                                currentTs = json.optString("ts", currentTs)
                                val updates = json.optJSONArray("updates")
                                if (updates != null && updates.length() > 0) {
                                    sendBroadcast(Intent("com.reforged.client.NEW_MESSAGE"))
                                }
                            }
                        } catch (e: Exception) {
                            delay(5000)
                            break
                        }
                    }
                }.onFailure {
                    delay(10000)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
