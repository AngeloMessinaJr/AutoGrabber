package com.example.autograbber.ui.settings

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.autograbber.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootingScreen(
    onTestNotification: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalV2Colors.current
    val scope = rememberCoroutineScope()
    
    var isChecking by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context, com.example.autograbber.services.UnifiedAccessibilityService::class.java)) }
    var notificationPermissionGranted by remember { 
        mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    // Auto-detect when returning to app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessibilityEnabled = isAccessibilityServiceEnabled(context, com.example.autograbber.services.UnifiedAccessibilityService::class.java)
        notificationPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val allGranted = accessibilityEnabled && notificationPermissionGranted

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background
    ) { _ ->
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            // Header (Matches AccountScreen style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            
            HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.05f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (allGranted) {
                        item {
                            Surface(
                                color = V2Success.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, V2Success.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = V2Success)
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        "All permissions are turned on!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = V2Success
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ChecklistItem(
                            title = "Accessibility Service",
                            description = "Enables automated features and platform workflows.",
                            isPassed = accessibilityEnabled,
                            icon = Icons.Default.SettingsAccessibility,
                            onAction = {
                                openAccessibilitySettings(context)
                            }
                        )
                    }
                    item {
                        ChecklistItem(
                            title = "Notification Service",
                            description = "Allows the app to deliver real-time alerts",
                            isPassed = notificationPermissionGranted,
                            icon = Icons.Default.Notifications,
                            onAction = {
                                onTestNotification()
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            delay(1000) // Simulate checking
                            accessibilityEnabled = isAccessibilityServiceEnabled(context, com.example.autograbber.services.UnifiedAccessibilityService::class.java)
                            notificationPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            isChecking = false
                            onTestNotification() // Automatically test notification as requested
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = V2Primary),
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Verify Permissions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    description: String,
    isPassed: Boolean,
    icon: ImageVector,
    onAction: () -> Unit
) {
    val colors = LocalV2Colors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isPassed) V2Success.copy(alpha = 0.1f) else V2Error.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPassed) V2Success else V2Error
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }

            if (!isPassed) {
                TextButton(onClick = onAction) {
                    Text("FIX", color = V2Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
    val expectedComponentName = android.content.ComponentName(context, service)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)

    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}
