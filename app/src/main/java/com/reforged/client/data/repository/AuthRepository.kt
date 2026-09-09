package com.reforged.client.data.repository

import android.content.Context
import com.reforged.client.R
import com.reforged.client.data.remote.AuthResponse
import com.reforged.client.data.remote.api.VkHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient,
    @ApplicationContext private val context: Context
) {
    private val appId = context.resources.getInteger(R.integer.com_vk_sdk_AppId).toString()
    // Official Android App Secret
    private val appSecret = "hHbZxrka2uZ6jB1inYsH"

    private val deviceId by lazy {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id!!
    }

    suspend fun login(
        username: String,
        password: String,
        captchaSid: String? = null,
        captchaKey: String? = null,
        code: String? = null
    ): Result<AuthResponse> {
        return performLogin(appId, appSecret, username, password, captchaSid, captchaKey, code)
    }

    suspend fun loginMusic(
        username: String,
        password: String,
        captchaSid: String? = null,
        captchaKey: String? = null,
        code: String? = null
    ): Result<AuthResponse> {
        // VK Music (BOOM) Credentials
        return performLogin("6121396", "7bS6v25tZ7pcZ956snS8", username, password, captchaSid, captchaKey, code)
    }

    private suspend fun performLogin(
        clientId: String,
        clientSecret: String,
        username: String,
        password: String,
        captchaSid: String? = null,
        captchaKey: String? = null,
        code: String? = null
    ): Result<AuthResponse> {
        val params = mutableMapOf(
            "grant_type" to "password",
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "username" to username,
            "password" to password,
            "2fa_supported" to "1",
            "force_sms" to "1",
            "v" to "5.119", // Using a version often used with BOOM
            "scope" to "all,offline",
            "device_id" to deviceId
        )

        captchaSid?.let { 
            params["captcha_sid"] = it 
            params["sid"] = it
        }
        captchaKey?.let { params["captcha_key"] = it }
        code?.let { params["code"] = it }

        return try {
            val response = vkHttpClient.login(params)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
