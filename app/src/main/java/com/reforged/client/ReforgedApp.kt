package com.reforged.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reforged.client.data.remote.NewsfeedInterceptor
import com.reforged.client.data.repository.SettingsRepository
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiConfig
import com.vk.api.sdk.VKOkHttpProvider
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject

@HiltAndroidApp
class ReforgedApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tokenStorage: com.reforged.client.data.local.TokenStorage

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        com.google.firebase.FirebaseApp.initializeApp(this)
        com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        createNotificationChannels()
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val settingsRepository = SettingsRepository(this)
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(NewsfeedInterceptor(settingsRepository))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VKAndroidApp/8.5-14400 (Android 13; SDK 33; arm64-v8a; Xiaomi; ru; 2340x1080)")
                    .build()
                chain.proceed(request)
            }
            .build()

        val config = VKApiConfig(
            context = this,
            appId = resources.getInteger(R.integer.com_vk_sdk_AppId),
            okHttpProvider = object : VKOkHttpProvider() {
                override fun getClient(): OkHttpClient = okHttpClient
                override fun updateClient(f: BuilderUpdateFunction) {}
            },
            clientSecret = "hHbZxrka2uZ6jB1inYsH",
            version = "5.199",
            apiHostProvider = { "api.vk.ru" }
        )
        
        VK.initialize(this)
        VK.setConfig(config)
        
        // Ensure token is synced with VK SDK if it exists in our storage
        if (!VK.isLoggedIn() && tokenStorage.accessToken != null) {
            VK.saveAccessToken(
                userId = com.vk.dto.common.id.UserId(tokenStorage.userId),
                accessToken = tokenStorage.accessToken!!,
                secret = null,
                expiresInSec = -1,
                createdMs = System.currentTimeMillis()
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                "messages",
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            val musicChannel = NotificationChannel(
                "music",
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(musicChannel)
        }
    }
}
