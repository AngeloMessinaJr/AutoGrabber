package com.example.autograbber.data

import com.example.autograbber.data.db.NotificationDao
import com.example.autograbber.data.models.AppNotification
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {
    val allNotifications: Flow<List<AppNotification>> = notificationDao.getAllNotifications()

    suspend fun addNotification(notification: AppNotification) {
        notificationDao.insert(notification)
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun delete(notification: AppNotification) {
        notificationDao.delete(notification)
    }

    suspend fun clearAll() {
        notificationDao.deleteAll()
    }
    
    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }
}
