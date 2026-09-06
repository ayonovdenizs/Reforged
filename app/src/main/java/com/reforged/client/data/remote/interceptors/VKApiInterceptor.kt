package com.reforged.client.data.remote.interceptors

import android.content.Context
import android.content.Intent
import com.reforged.client.data.local.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

class VKApiInterceptor(
    private val tokenStorage: TokenStorage,
    private val context: Context,
    private val apiVersion: String = "5.199"
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        
        if (!originalUrl.host.contains("vk.ru") && !originalUrl.host.contains("vk.com")) {
            return chain.proceed(originalRequest)
        }

        val urlBuilder = originalUrl.newBuilder()
        
        if (originalUrl.queryParameter("v") == null) {
            urlBuilder.addQueryParameter("v", apiVersion)
        }
        
        if (originalUrl.queryParameter("access_token") == null) {
            tokenStorage.accessToken?.let {
                urlBuilder.addQueryParameter("access_token", it)
            }
        }
        
        if (originalUrl.queryParameter("lang") == null) {
            urlBuilder.addQueryParameter("lang", "ru")
        }

        val newRequest = originalRequest.newBuilder()
            .url(urlBuilder.build())
            .build()
            
        val response = chain.proceed(newRequest)
        
        // Check for token expiration in response body
        if (response.isSuccessful) {
            val peekBody = response.peekBody(2048).string()
            try {
                val json = JSONObject(peekBody)
                if (json.has("error")) {
                    val error = json.getJSONObject("error")
                    val errorCode = error.optInt("error_code")
                    if (errorCode == 5 || errorCode == 1117) {
                        // Token expired or invalid - broadcast logout
                        val intent = Intent("com.reforged.client.LOGOUT")
                        context.sendBroadcast(intent)
                    }
                }
            } catch (e: Exception) {
                // Not a JSON or other error
            }
        }
        
        return response
    }
}
