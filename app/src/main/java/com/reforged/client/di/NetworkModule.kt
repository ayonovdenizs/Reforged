package com.reforged.client.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.AuthApi
import com.reforged.client.data.remote.BadgeApi
import com.reforged.client.data.remote.api.VKService
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideAuthApi(client: OkHttpClient, gson: Gson): AuthApi {
        return Retrofit.Builder()
            .baseUrl("https://oauth.vk.ru/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVKService(client: OkHttpClient, gson: Gson): VKService {
        return Retrofit.Builder()
            .baseUrl("https://api.vk.ru/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(VKService::class.java)
    }

    @Provides
    @Singleton
    fun provideBadgeApi(client: OkHttpClient, gson: Gson): BadgeApi {
        return Retrofit.Builder()
            .baseUrl("https://pyminelauncher.vercel.app/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BadgeApi::class.java)
    }
}
