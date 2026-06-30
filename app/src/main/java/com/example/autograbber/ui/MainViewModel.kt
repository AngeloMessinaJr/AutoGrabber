package com.example.autograbber.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autograbber.data.OfferRepository
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.NotificationRepository
import com.example.autograbber.data.models.FilterPreferences
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.Platform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsRepository: SettingsRepository,
    private val offerRepository: OfferRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val filterPreferences: StateFlow<FilterPreferences> = settingsRepository.filterPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FilterPreferences()
        )

    val hasUnreadNotifications: StateFlow<Boolean> = notificationRepository.allNotifications
        .map { notifications -> notifications.any { !it.isRead } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updatePreferences(preferences: FilterPreferences) {
        viewModelScope.launch {
            settingsRepository.updateFilterPreferences(preferences)
        }
    }

    fun getOffersByPlatform(platform: Platform): Flow<List<GigOffer>> = 
        offerRepository.getOffersByPlatform(platform)

    val allOffers: Flow<List<GigOffer>> = offerRepository.getAllOffers()

    fun clearOfferHistory(platform: Platform) {
        viewModelScope.launch {
            offerRepository.clearHistory(platform)
        }
    }

    fun clearAllOfferHistory() {
        viewModelScope.launch {
            offerRepository.clearAllHistory()
        }
    }
}
