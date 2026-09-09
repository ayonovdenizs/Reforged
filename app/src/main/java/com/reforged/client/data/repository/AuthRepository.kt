package com.reforged.client.data.repository

import android.content.Context
import com.reforged.client.R
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
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
            android.util.Log.d("AuthRepository", "Using cached anonym token")
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

        android.util.Log.d("AuthRepository", "Fetching new anonym token with appId: $appId")
        val token = vkHttpClient.getAnonymToken(params)
        if (token != null) {
            tokenStorage.anonymToken = token
            tokenStorage.anonymTokenExpiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24h
            android.util.Log.d("AuthRepository", "New anonym token saved")
        } else {
            android.util.Log.e("AuthRepository", "Failed to fetch anonym token from VK")
        }
        return token
    }

    suspend fun validateAccount(username: String, captchaSuccessToken: String? = null): Result<VKApiValidateAccount> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("Failed to get anonym token"))
        
        val params = mutableMapOf(
            "login" to username,
            "supported_ways" to "push,email,sms,callreset,password,reserve_code,codegen",
            "force_password" to "false",
            "sak_version" to "15.0.2",
            "flow_type" to "auth_without_password",
            "access_token" to anonymToken,
            "v" to "5.199",
            "https" to "1"
        )
        captchaSuccessToken?.let { params["success_token"] = it }

        return try {
            val responseString = vkHttpClient.validateAccount(params)
            android.util.Log.d("AuthRepository", "Validate Account Response: $responseString")
            val jsonObject = JSONObject(responseString)
            
            val jsonResponse = jsonObject.optJSONObject("response")
            if (jsonResponse != null) {
                Result.success(json.decodeFromString<VKApiValidateAccount>(jsonResponse.toString()))
            } else {
                val error = jsonObject.optJSONObject("error")
                if (error != null) {
                    android.util.Log.d("AuthRepository", "Detected error object in response")
                    Result.success(VKApiValidateAccount(error = json.decodeFromString<AuthError>(error.toString())))
                } else {
                    android.util.Log.e("AuthRepository", "No response or error object found")
                    Result.failure(Exception("Validation failed: $responseString"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Parsing error in validateAccount", e)
            Result.failure(e)
        }
    }

    suspend fun getVerificationMethods(sid: String): Result<List<VerificationMethod>> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("No anonym token"))
        val params = mapOf(
            "sid" to sid,
            "access_token" to anonymToken,
            "api_id" to appId,
            "v" to "5.199",
            "https" to "1",
            "device_id" to deviceId
        )

        return try {
            val responseString = vkHttpClient.getVerificationMethods(params)
            val jsonResponse = JSONObject(responseString).optJSONObject("response")
            if (jsonResponse != null) {
                val data = json.decodeFromString<EcosystemVerificationMethods>(jsonResponse.toString())
                Result.success(data.methods ?: emptyList())
            } else {
                Result.failure(Exception("Methods fetch failed: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEcosystemOtp(sid: String, method: String, username: String): Result<EcosystemSendOtp> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("No anonym token"))
        val params = mapOf(
            "sid" to sid,
            "access_token" to anonymToken,
            "api_id" to appId,
            "v" to "5.199",
            "https" to "1",
            "device_id" to deviceId
        )

        return try {
            val responseString = if (method == "phone" || method == "sms") {
                vkHttpClient.validatePhone(params + ("phone" to username))
            } else {
                vkHttpClient.sendEcosystemOtp(method, params)
            }
            
            val jsonResponse = JSONObject(responseString).optJSONObject("response")
            if (jsonResponse != null) {
                Result.success(json.decodeFromString<EcosystemSendOtp>(jsonResponse.toString()))
            } else {
                Result.failure(Exception("OTP send failed: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkEcosystemOtp(sid: String, method: String, code: String): Result<EcosystemCheckOtp> {
        val anonymToken = getAnonymToken() ?: return Result.failure(Exception("No anonym token"))
        val params = mapOf(
            "sid" to sid,
            "verification_method" to method,
            "code" to code,
            "access_token" to anonymToken,
            "api_id" to appId,
            "v" to "5.199",
            "https" to "1",
            "device_id" to deviceId
        )

        return try {
            val responseString = vkHttpClient.checkEcosystemOtp(params)
            val jsonResponse = JSONObject(responseString).optJSONObject("response")
            if (jsonResponse != null) {
                Result.success(json.decodeFromString<EcosystemCheckOtp>(jsonResponse.toString()))
            } else {
                Result.failure(Exception("OTP check failed: $responseString"))
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
        grantType: String = "without_password",
        captchaSuccessToken: String? = null
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
        captchaSuccessToken?.let { params["success_token"] = it }

        return try {
            val responseString = vkHttpClient.directLogin(params)
            Result.success(json.decodeFromString<LoginResponse>(responseString))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAndWarmToken(accessToken: String): Result<String> {
        return try {
            // 1. Get exchange token
            val params = mapOf(
                "access_token" to accessToken,
                "v" to "5.199"
            )
            val exchangeResponse = vkHttpClient.getExchangeToken(params)
            val exchangeToken = JSONObject(exchangeResponse).optJSONObject("response")?.optString("token")
                ?: return Result.failure(Exception("Failed to get exchange token"))

            // 2. Auth by exchange token
            authByExchangeToken(exchangeToken)
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
