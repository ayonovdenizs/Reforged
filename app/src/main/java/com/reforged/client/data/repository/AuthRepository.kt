package com.reforged.client.data.repository

import android.content.Context
import com.reforged.client.R
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.LoginResponse
import com.reforged.client.data.remote.VKApiValidateAccount
import com.reforged.client.data.remote.api.VkHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient,
    private val tokenStorage: TokenStorage,
    @ApplicationContext private val context: Context
) {
    private val appId = context.resources.getInteger(R.integer.com_vk_sdk_AppId).toString()
    // Official Android App Secret
    private val appSecret = "hHbZxrka2uZ6jB1inYsH"
    
    private val json = Json { ignoreUnknownKeys = true }

    private val deviceId by lazy {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id!!
    }

    suspend fun getAnonymToken(): String? {
        val cached = tokenStorage.anonymToken
        val expiry = tokenStorage.anonymTokenExpiry
        if (cached != null && expiry > System.currentTimeMillis()) {
            return cached
        }

        val params = mapOf(
            "client_id" to appId,
            "api_id" to appId,
            "client_secret" to appSecret,
            "v" to "5.199",
            "device_id" to deviceId,
            "https" to "1"
        )

        val token = vkHttpClient.getAnonymToken(params)
        if (token != null) {
            tokenStorage.anonymToken = token
            tokenStorage.anonymTokenExpiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24h
        }
        return token
    }

    suspend fun validateAccount(username: String): Result<VKApiValidateAccount> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("Failed to get anonym token"))
        
        val params = mapOf(
            "login" to username,
            "supported_ways" to "push,email,sms,callreset,password,reserve_code,codegen",
            "force_password" to "false",
            "sak_version" to "15.0.2",
            "flow_type" to "auth_without_password",
            "access_token" to anonymToken,
            "v" to "5.199",
            "https" to "1"
        )

        return try {
            val responseString = vkHttpClient.validateAccount(params)
            val jsonResponse = JSONObject(responseString).optJSONObject("response")
            if (jsonResponse != null) {
                Result.success(json.decodeFromString<VKApiValidateAccount>(jsonResponse.toString()))
            } else {
                Result.failure(Exception("Validation failed: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun directLogin(
        username: String,
        password: String? = null,
        sid: String? = null,
        code: String? = null,
        grantType: String = "without_password"
    ): Result<LoginResponse> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("Failed to get anonym token"))

        val params = mutableMapOf(
            "grant_type" to grantType,
            "api_id" to appId,
            "client_id" to appId,
            "username" to username,
            "2fa_supported" to "1",
            "anonymous_token" to anonymToken,
            "sak_version" to "15.0.2",
            "flow_type" to "tg_flow",
            "scope" to "all",
            "v" to "5.199",
            "https" to "1"
        )

        password?.let { params["password"] = it }
        sid?.let { params["sid"] = it }
        code?.let { params["code"] = it }

        return try {
            val responseString = vkHttpClient.directLogin(params)
            Result.success(json.decodeFromString<LoginResponse>(responseString))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authByExchangeToken(exchangeToken: String): Result<String> {
        val params = mapOf(
            "client_id" to appId,
            "api_id" to appId,
            "exchange_token" to exchangeToken,
            "scope" to "all",
            "initiator" to "expired_token",
            "device_id" to deviceId,
            "sak_version" to "15.0.2",
            "v" to "5.199",
            "https" to "1"
        )

        return try {
            val responseString = vkHttpClient.authByExchangeToken(params)
            // auth_by_exchange_token returns a redirect URL with access_token in fragment
            val token = tryExtractToken(responseString)
            if (token != null) Result.success(token) 
            else Result.failure(Exception("Failed to extract token from: $responseString"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryExtractToken(url: String): String? {
        val regex = "access_token=([^&]+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
}
