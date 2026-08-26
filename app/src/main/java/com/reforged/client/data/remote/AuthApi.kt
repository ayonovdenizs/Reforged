package com.reforged.client.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AuthApi {
    @GET("token")
    suspend fun login(
        @QueryMap params: Map<String, String>
    ): Response<AuthResponse>
}

data class AuthResponse(
    val access_token: String? = null,
    val user_id: Long? = null,
    val secret: String? = null,
    val expires_in: Int? = null,
    val error: String? = null,
    val error_description: String? = null,
    val captcha_sid: String? = null,
    val captcha_img: String? = null,
    val validation_type: String? = null,
    val validation_sid: String? = null,
    val phone_mask: String? = null
)
