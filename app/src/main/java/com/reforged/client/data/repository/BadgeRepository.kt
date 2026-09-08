package com.reforged.client.data.repository

import com.reforged.client.data.remote.BadgeDto
import com.reforged.client.data.remote.api.VkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val vkHttpClient: VkHttpClient
) {
    private val cache = mutableMapOf<Long, List<BadgeDto>>()

    suspend fun getBadges(vkId: Long): List<BadgeDto> {
        cache[vkId]?.let { return it }

        return try {
            val response = vkHttpClient.getUserBadges(vkId)
            val badges = response.data?.badges ?: emptyList()
            cache[vkId] = badges
            badges
        } catch (e: Exception) {
            emptyList()
        }
    }
}
