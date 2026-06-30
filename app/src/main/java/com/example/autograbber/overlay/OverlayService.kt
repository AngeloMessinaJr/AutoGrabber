package com.example.autograbber.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.autograbber.R
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.models.FilterPreferences
import com.example.autograbber.data.models.Platform
import com.example.autograbber.ui.settings.DualInputPreference
import com.example.autograbber.ui.settings.PreferenceSwitch
import com.example.autograbber.ui.settings.SectionHeader
import com.example.autograbber.ui.theme.AutoGrabberTheme
import com.example.autograbber.ui.theme.V2Background
import com.example.autograbber.ui.theme.V2Primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var floatingButtonView: ComposeView? = null
    private var popupView: ComposeView? = null

    private lateinit var settingsRepository: SettingsRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private val lifecycleRegistry = LifecycleRegistry(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(applicationContext)

        startForeground(1002, createNotification())
        showFloatingButton()
    }

    private fun createNotification(): Notification {
        val channelId = "overlay_service"
        val channel = NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AutoGrabber Overlay")
            .setContentText("Overlay controls active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun showFloatingButton() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        floatingButtonView = ComposeView(this).apply {
            setContent {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(V2Primary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { togglePopup() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Adjust", tint = Color.White)
                }
            }
        }
        
        floatingButtonView?.let { view ->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
            windowManager.addView(view, params)
        }
    }

    private fun togglePopup() {
        if (popupView == null) {
            showPopup()
        } else {
            hidePopup()
        }
    }

    private fun showPopup() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        popupView = ComposeView(this).apply {
            setContent {
                val preferences by settingsRepository.filterPreferencesFlow.collectAsState(initial = FilterPreferences())
                
                AutoGrabberTheme(darkTheme = true) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = V2Background,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Quick Adjust",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                IconButton(onClick = { hidePopup() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Platform.entries.forEach { platform ->
                                val isEnabled = when (platform) {
                                    Platform.SPARK -> preferences.isSparkEnabled
                                    Platform.DOORDASH -> preferences.isDoorDashEnabled
                                    Platform.UBER -> preferences.isUberEnabled
                                    Platform.INSTACART -> preferences.isInstacartEnabled
                                    Platform.FLEX -> preferences.isFlexEnabled
                                }
                                val name = when (platform) {
                                    Platform.SPARK -> "Spark"
                                    Platform.DOORDASH -> "DoorDash"
                                    Platform.UBER -> "Uber"
                                    Platform.INSTACART -> "Instacart"
                                    Platform.FLEX -> "Flex"
                                }
                                
                                SectionHeader(name)
                                PreferenceSwitch(
                                    label = "Active",
                                    description = "Enable $name prioritization",
                                    checked = isEnabled,
                                    onCheckedChange = { enabled ->
                                        val newPrefs = when (platform) {
                                            Platform.SPARK -> preferences.copy(isSparkEnabled = enabled)
                                            Platform.DOORDASH -> preferences.copy(isDoorDashEnabled = enabled)
                                            Platform.UBER -> preferences.copy(isUberEnabled = enabled)
                                            Platform.INSTACART -> preferences.copy(isInstacartEnabled = enabled)
                                            Platform.FLEX -> preferences.copy(isFlexEnabled = enabled)
                                        }
                                        serviceScope.launch { settingsRepository.updateFilterPreferences(newPrefs) }
                                    }
                                )
                                
                                if (isEnabled) {
                                    val filters = when (platform) {
                                        Platform.SPARK -> preferences.sparkFilters
                                        Platform.DOORDASH -> preferences.doorDashFilters
                                        Platform.UBER -> preferences.uberFilters
                                        Platform.INSTACART -> preferences.instacartFilters
                                        Platform.FLEX -> preferences.flexFilters
                                    }
                                    
                                    DualInputPreference(
                                        label = "Min Pay / Mile",
                                        value = filters.minPayPerMile.toFloat(),
                                        onValueChange = { val newFilters = filters.copy(minPayPerMile = it.toDouble())
                                            updateFilters(platform, preferences, newFilters) },
                                        valueRange = 0.5f..5.0f
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        
        popupView?.let { view ->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
            windowManager.addView(view, params)
        }
    }

    private fun updateFilters(platform: Platform, preferences: FilterPreferences, newFilters: com.example.autograbber.data.models.PlatformFilters) {
        val newPrefs = when (platform) {
            Platform.SPARK -> preferences.copy(sparkFilters = newFilters)
            Platform.DOORDASH -> preferences.copy(doorDashFilters = newFilters)
            Platform.UBER -> preferences.copy(uberFilters = newFilters)
            Platform.INSTACART -> preferences.copy(instacartFilters = newFilters)
            Platform.FLEX -> preferences.copy(flexFilters = newFilters)
        }
        serviceScope.launch { settingsRepository.updateFilterPreferences(newPrefs) }
    }

    private fun hidePopup() {
        popupView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            popupView = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        floatingButtonView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        hidePopup()
    }
}
