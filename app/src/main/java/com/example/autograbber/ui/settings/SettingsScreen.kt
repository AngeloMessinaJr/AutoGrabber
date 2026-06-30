package com.example.autograbber.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autograbber.data.models.FilterPreferences
import com.example.autograbber.ui.theme.*
import com.google.firebase.appdistribution.FirebaseAppDistribution
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: FilterPreferences,
    onPreferencesChanged: (FilterPreferences) -> Unit,
    onNavigateToTroubleshooting: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalV2Colors.current
    val scope = rememberCoroutineScope()
    
    var isCheckingUpdates by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background
    ) { _ ->
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Text(
                        "APPEARANCE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PreferenceSwitch(
                            label = "App Theme",
                            description = "Switch between light and dark modes.",
                            checked = preferences.isDarkMode,
                            onCheckedChange = { onPreferencesChanged(preferences.copy(isDarkMode = it)) }
                        )
                    }
                }

                item {
                    Text(
                        "SYSTEM",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Surface(
                        color = colors.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToTroubleshooting() },
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
                    ) {
                        Column {
                            SettingsRow(
                                title = "Permissions",
                                subtitle = "Ensure all required access is granted.",
                                onClick = onNavigateToTroubleshooting
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colors.textPrimary.copy(alpha = 0.05f)
                            )

                            SettingsRow(
                                title = "Check for Updates",
                                subtitle = if (isCheckingUpdates) "Checking..." else "Check for the latest version",
                                icon = Icons.Default.SystemUpdate,
                                onClick = {
                                    if (!isCheckingUpdates) {
                                        isCheckingUpdates = true
                                        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
                                            .addOnCompleteListener {
                                                isCheckingUpdates = false
                                            }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val colors = LocalV2Colors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                color = V2Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = V2Primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    AutoGrabberTheme(darkTheme = true) {
        SettingsScreen(
            preferences = FilterPreferences(),
            onPreferencesChanged = {},
            onNavigateToTroubleshooting = {},
            onNavigateBack = {}
        )
    }
}
