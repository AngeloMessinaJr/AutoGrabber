package com.example.autograbber.data.db

import androidx.room.*
import com.example.autograbber.data.models.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotification)

    @Update
    suspend fun update(notification: AppNotification)

    @Delete
    suspend fun delete(notification: AppNotification)

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE app_notifications SET isRead = 1")
    suspend fun markAllAsRead()
}
