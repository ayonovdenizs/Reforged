package com.reforged.client.data.repository

import com.reforged.client.data.remote.BadgeApi
import com.reforged.client.data.remote.BadgeDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val badgeApi: BadgeApi
) {
    private val cache = mutableMapOf<Long, List<BadgeDto>>()

    suspend fun getBadges(vkId: Long): List<BadgeDto> {
        cache[vkId]?.let { return it }

        return try {
            val response = badgeApi.getUserBadges(vkId)
            if (response.isSuccessful) {
                val badges = response.body()?.data?.badges ?: emptyList()
                cache[vkId] = badges
                badges
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
