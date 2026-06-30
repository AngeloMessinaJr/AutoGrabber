package com.example.autograbber.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.autograbber.data.models.FilterPreferences
import com.example.autograbber.data.models.Platform
import com.example.autograbber.ui.theme.*
import com.example.autograbber.utils.getPlatformPackageNames

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    preferences: FilterPreferences,
    isLoggedIn: Boolean,
    isApproved: Boolean,
    hasLifetimeAccess: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToPlatformFilters: (Platform) -> Unit,
    onTogglePlatform: (Platform, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalV2Colors.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AutoGrabber",
                        tint = V2Primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Account", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary,
                    scrolledContainerColor = colors.background
                ),
                windowInsets = WindowInsets.statusBars // Only status bar insets
            )
        },
        containerColor = colors.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasLifetimeAccess) {
                item {
                    SubscriptionBanner(isLoggedIn, onNavigateToAccount)
                }
            }

            val availablePlatforms = listOf(Platform.INSTACART, Platform.DOORDASH, Platform.FLEX)
            items(availablePlatforms) { platform ->
                val isEnabled = when (platform) {
                    Platform.DOORDASH -> preferences.isDoorDashEnabled
                    Platform.INSTACART -> preferences.isInstacartEnabled
                    Platform.FLEX -> preferences.isFlexEnabled
                    else -> false
                }

                val isLocked = !hasLifetimeAccess

                PlatformHubCard(
                    platform = platform,
                    isEnabled = isEnabled,
                    isLocked = isLocked,
                    isComingSoon = false,
                    onToggle = { onTogglePlatform(platform, it) },
                    onFiltersClick = { if (!isLocked) onNavigateToPlatformFilters(platform) },
                    onLaunch = { if (!isLocked) launchDriverApp(context, platform) }
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { expanded = !expanded }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PLATFORMS COMING SOON",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val comingSoonPlatforms = listOf(Platform.SPARK, Platform.UBER)
                            comingSoonPlatforms.forEach { platform ->
                                PlatformHubCard(
                                    platform = platform,
                                    isEnabled = false,
                                    isLocked = true,
                                    isComingSoon = true,
                                    onToggle = { },
                                    onFiltersClick = { },
                                    onLaunch = { }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformHubCard(
    platform: Platform,
    isEnabled: Boolean,
    isLocked: Boolean,
    isComingSoon: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onFiltersClick: () -> Unit,
    onLaunch: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val colors = LocalV2Colors.current
    
    val appIcon = remember(platform) {
        val packages = getPlatformPackageNames(platform)
        var icon: android.graphics.drawable.Drawable? = null
        for (pkg in packages) {
            try {
                icon = packageManager.getApplicationIcon(pkg)
                break
            } catch (_: Exception) {}
        }
        icon
    }

    val platformName = when (platform) {
        Platform.SPARK -> "Spark"
        Platform.DOORDASH -> "DoorDash"
        Platform.UBER -> "Uber"
        Platform.INSTACART -> "Instacart"
        Platform.FLEX -> "Flex"
    }

    val platformIcon = when (platform) {
        Platform.SPARK -> Icons.Default.ShoppingCart
        Platform.DOORDASH -> Icons.Default.Fastfood
        Platform.UBER -> Icons.Default.DirectionsCar
        Platform.INSTACART -> Icons.Default.ShoppingCart
        Platform.FLEX -> Icons.Default.LocalShipping
    }
    
    val iconBgColor = when (platform) {
        Platform.SPARK -> Color(0xFF1B2E1E)
        Platform.DOORDASH -> Color(0xFF2E1B1B)
        Platform.UBER -> Color(0xFF1B222E)
        Platform.INSTACART -> Color(0xFF1B2E1B)
        Platform.FLEX -> Color(0xFF2E2E2E)
    }
    
    val iconTint = when (platform) {
        Platform.SPARK -> V2Success
        Platform.DOORDASH -> V2Error
        Platform.UBER -> V2Primary
        Platform.INSTACART -> Color(0xFF20C997)
        Platform.FLEX -> Color(0xFFFF9900)
    }

    Box {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (appIcon != null) Color.Transparent else iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (appIcon != null) {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = platformName,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        } else {
                            Icon(platformIcon, null, tint = iconTint, modifier = Modifier.size(24.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            platformName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isComingSoon) "Disabled" else if (isLocked) "Disabled" else if (isEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isComingSoon || isLocked) V2Error else if (isEnabled) V2Success else V2Error
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            if (isLocked) {
                                Toast.makeText(context, "Subscription required to access platform.", Toast.LENGTH_SHORT).show()
                                // The toggle will visually flip then flip back because preferences won't update
                            } else {
                                onToggle(it)
                            }
                        },
                        enabled = !isComingSoon,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = V2Primary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.background,
                            disabledCheckedTrackColor = V2Primary.copy(alpha = 0.5f),
                            disabledUncheckedTrackColor = colors.background.copy(alpha = 0.5f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = colors.textPrimary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                if (isComingSoon) {
                                    Toast.makeText(context, "This platform is coming soon", Toast.LENGTH_SHORT).show()
                                } else if (isLocked) {
                                    Toast.makeText(context, "Subscription required to enable platform.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onLaunch()
                                }
                            }
                            .border(0.5.dp, colors.textPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Launch, null, tint = if (isLocked || isComingSoon) Color.Gray else colors.textPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch", color = if (isLocked || isComingSoon) Color.Gray else colors.textPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Surface(
                        color = colors.textPrimary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { 
                                if (isComingSoon) {
                                    Toast.makeText(context, "This platform is coming soon", Toast.LENGTH_SHORT).show()
                                } else if (isLocked) {
                                    Toast.makeText(context, "Subscription required to enable platform.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onFiltersClick()
                                }
                            }
                            .border(0.5.dp, colors.textPrimary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, null, tint = if (isLocked || isComingSoon) Color.Gray else V2Primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Settings", color = if (isLocked || isComingSoon) Color.Gray else colors.textPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionBanner(isLoggedIn: Boolean, onNavigateToAccount: () -> Unit) {
    val colors = LocalV2Colors.current
    Surface(
        color = V2Primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onNavigateToAccount() }
            .border(1.dp, V2Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = V2Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Subscription Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = if (isLoggedIn) "Upgrade to access platforms."
                           else "Create an account or login to upgrade to access platforms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}


private fun launchDriverApp(context: Context, platform: Platform) {
    val packages = getPlatformPackageNames(platform)
    val pm = context.packageManager

    var launchIntent: android.content.Intent? = null
    for (pkg in packages) {
        launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) break
    }

    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        val platformName = when (platform) {
            Platform.SPARK -> "Spark"
            Platform.DOORDASH -> "DoorDash"
            Platform.UBER -> "Uber"
            Platform.INSTACART -> "Instacart"
            Platform.FLEX -> "Flex"
        }
        Toast.makeText(
            context,
            "$platformName is not installed",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF010609)
@Composable
fun DashboardPreview() {
    AutoGrabberTheme(darkTheme = true) {
        DashboardScreen(
            preferences = FilterPreferences(),
            isLoggedIn = false,
            isApproved = false,
            hasLifetimeAccess = false,
            onNavigateToSettings = {},
            onNavigateToNotifications = {},
            onNavigateToAccount = {},
            onNavigateToPlatformFilters = {},
            onTogglePlatform = { _, _ -> }
        )
    }
}
