package com.reforged.client.di

import android.content.Context
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.interceptors.VKApiInterceptor
import com.reforged.client.data.remote.NewsfeedInterceptor
import com.reforged.client.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        tokenStorage: TokenStorage
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(NewsfeedInterceptor(settingsRepository))
            .addInterceptor(VKApiInterceptor(tokenStorage, context))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VKAndroidApp/8.5-14400 (Android 13; SDK 33; arm64-v8a; Xiaomi; ru; 2340x1080)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
