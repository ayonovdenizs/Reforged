package com.reforged.client.data.repository

import android.content.Context
import com.google.gson.Gson
import com.reforged.client.R
import com.reforged.client.data.remote.AuthApi
import com.reforged.client.data.remote.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    @ApplicationContext private val context: Context
) {
    private val appId = context.resources.getInteger(R.integer.com_vk_sdk_AppId).toString()
    // Official Android App Secret
    private val appSecret = "hHbZxrka2uZ6jB1inYsH"

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
            "scope" to "all,offline"
        )

        captchaSid?.let { 
            params["captcha_sid"] = it 
            params["sid"] = it
        }
        captchaKey?.let { params["captcha_key"] = it }
        code?.let { params["code"] = it }

        return try {
            val response = authApi.login(params)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty body"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        val authResponse = Gson().fromJson(errorBody, AuthResponse::class.java)
                        Result.success(authResponse)
                    } catch (e: Exception) {
                        Result.failure(Exception("Network error: ${response.code()}"))
                    }
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
