package com.example.autograbber.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.autograbber.MainActivity
import com.example.autograbber.R
import com.example.autograbber.data.models.FilterPreferences

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "autograbber_status"
        private const val CHANNEL_NAME = "AutoGrabber Status"
        private const val CHANNEL_DESCRIPTION = "Active status of platforms"
        private const val STATUS_NOTIFICATION_ID = 1000
        private const val ALERT_NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun updateStatusNotification(preferences: FilterPreferences) {
        val anyEnabled = preferences.isDoorDashEnabled || preferences.isInstacartEnabled || preferences.isFlexEnabled
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom Layout
        val remoteViews = RemoteViews(context.packageName, R.layout.notification_status)
        
        if (anyEnabled) {
            remoteViews.setTextViewText(R.id.status_text, "Waiting for Offers...")
            remoteViews.setViewVisibility(R.id.status_progress, View.VISIBLE)
        } else {
            remoteViews.setTextViewText(R.id.status_text, "All Platforms Disabled")
            remoteViews.setViewVisibility(R.id.status_progress, View.GONE)
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bot)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(STATUS_NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
            }
        }
    }

    fun sendAlertNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bot)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                // Use a unique ID to allow stacking of alerts
                val uniqueId = ALERT_NOTIFICATION_ID + (System.currentTimeMillis() % 10000).toInt()
                notify(uniqueId, builder.build())
            } catch (_: SecurityException) {
            }
        }
    }

    fun sendTestNotification() {
        sendAlertNotification(
            title = "AutoGrabber Test",
            message = "This is a test notification to verify the service is working correctly."
        )
    }
}
