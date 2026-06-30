package com.example.autograbber.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.autograbber.data.OfferRepository
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.NotificationRepository
import com.example.autograbber.ui.notifications.NotificationsViewModel

class MainViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val offerRepository: OfferRepository,
    private val chatRepository: com.example.autograbber.data.ChatRepository,
    private val notificationRepository: NotificationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(settingsRepository, offerRepository, notificationRepository) as T
        }
        if (modelClass.isAssignableFrom(com.example.autograbber.ui.chat.ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.example.autograbber.ui.chat.ChatViewModel(chatRepository) as T
        }
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(notificationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
