package com.reforged.client.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var blockAds: Boolean
        get() = prefs.getBoolean("block_ads", true)
        set(value) = prefs.edit().putBoolean("block_ads", value).apply()

    var blockRecommended: Boolean
        get() = prefs.getBoolean("block_recommended", true)
        set(value) = prefs.edit().putBoolean("block_recommended", value).apply()
}
