package com.reforged.client.ui.settings

import androidx.lifecycle.ViewModel
import com.reforged.client.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _blockAds = MutableStateFlow(settingsRepository.blockAds)
    val blockAds: StateFlow<Boolean> = _blockAds

    private val _blockRecommended = MutableStateFlow(settingsRepository.blockRecommended)
    val blockRecommended: StateFlow<Boolean> = _blockRecommended

    fun setBlockAds(value: Boolean) {
        settingsRepository.blockAds = value
        _blockAds.value = value
    }

    fun setBlockRecommended(value: Boolean) {
        settingsRepository.blockRecommended = value
        _blockRecommended.value = value
    }
}
