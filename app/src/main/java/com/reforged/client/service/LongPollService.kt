package com.reforged.client.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.reforged.client.data.manager.LongPollManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LongPollService : Service() {

    @Inject
    lateinit var longPollManager: LongPollManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "messages")
            .setContentTitle("Reforged")
            .setContentText("Получение сообщений в реальном времени...")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
            
        startForeground(101, notification)
        
        longPollManager.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        longPollManager.stop()
    }
}
