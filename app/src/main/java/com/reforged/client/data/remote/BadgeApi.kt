package com.reforged.client.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface BadgeApi {
    @GET("api_vtlr/users/{vk_id}/badges/")
    suspend fun getUserBadges(
        @Path("vk_id") vkId: Long
    ): Response<BadgeResponse>
}

data class BadgeResponse(
    val status: String,
    val data: BadgeData? = null,
    val message: String? = null
)

data class BadgeData(
    val vk_id: Long,
    val badges: List<BadgeDto>
)

data class BadgeDto(
    val type: Int,
    val slug: String,
    val label: String,
    val icon: String,
    val priority: Int
)
