package com.reforged.client.data.remote.interceptors

import com.reforged.client.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class VKMusicInterceptor(
    private val tokenStorage: TokenStorage,
    private val apiVersion: String = "5.119" // Using a version that works well with BOOM
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        val urlBuilder = originalUrl.newBuilder()
        
        if (originalUrl.queryParameter("v") == null) {
            urlBuilder.addQueryParameter("v", apiVersion)
        }
        
        if (originalUrl.queryParameter("access_token") == null) {
            tokenStorage.musicAccessToken?.let {
                urlBuilder.addQueryParameter("access_token", it)
            } ?: tokenStorage.accessToken?.let {
                // Fallback to main token if music token is not yet available
                urlBuilder.addQueryParameter("access_token", it)
            }
        }
        
        if (originalUrl.queryParameter("lang") == null) {
            urlBuilder.addQueryParameter("lang", "ru")
        }

        val newRequest = originalRequest.newBuilder()
            .url(urlBuilder.build())
            .header("User-Agent", "VKMusic/2.1.2 (Android 11; SDK 30; arm64-v8a; Google Pixel 4; ru)")
            .build()
            
        return chain.proceed(newRequest)
    }
}
