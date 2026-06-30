package com.example.autograbber

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.room.Room
import com.example.autograbber.data.OfferRepository
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.UserRepository
import com.example.autograbber.data.NotificationRepository
import com.example.autograbber.data.db.AppDatabase
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.UserProfile
import com.example.autograbber.notifications.NotificationHelper
import com.example.autograbber.ui.MainViewModel
import com.example.autograbber.ui.MainViewModelFactory
import com.example.autograbber.ui.notifications.NotificationsViewModel
import com.example.autograbber.ui.account.AccountScreen
import com.example.autograbber.ui.dashboard.DashboardScreen
import com.example.autograbber.ui.history.OfferHistoryScreen
import com.example.autograbber.ui.navigation.Destination
import com.example.autograbber.ui.notifications.NotificationsScreen
import com.example.autograbber.ui.settings.PlatformFilterScreen
import com.example.autograbber.ui.settings.SettingsScreen
import com.example.autograbber.ui.theme.*
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var notificationHelper: NotificationHelper

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            notificationHelper.sendTestNotification()
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val offerRepository = OfferRepository(db.offerDao())
        val notificationRepository = NotificationRepository(db.notificationDao())
        
        notificationHelper = NotificationHelper(this)
        val settingsRepository = SettingsRepository(applicationContext)
        val chatRepository = com.example.autograbber.data.ChatRepository(applicationContext)

        FirebaseAppDistribution.getInstance().updateIfNewReleaseAvailable()
            .addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "Update check failed", e)
            }
        
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(settingsRepository, offerRepository, chatRepository, notificationRepository)
            )
            val preferences by viewModel.filterPreferences.collectAsState()
            
            val auth = remember { FirebaseAuth.getInstance() }
            val userRepository = remember { UserRepository() }
            var userProfile by remember { mutableStateOf<UserProfile?>(null) }
            
            // Listen for Auth changes to reset listener
            val authUser by produceState(initialValue = auth.currentUser) {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    value = firebaseAuth.currentUser
                }
                auth.addAuthStateListener(listener)
                awaitDispose { auth.removeAuthStateListener(listener) }
            }

            DisposableEffect(authUser) {
                var listener: com.google.firebase.firestore.ListenerRegistration? = null
                authUser?.let { user ->
                    android.util.Log.d("MainActivity", "Setting up profile listener for: ${user.uid}")
                    listener = userRepository.observeUserProfile(user.uid) { profile ->
                        userProfile = profile
                    }
                } ?: run {
                    userProfile = null
                }
                onDispose {
                    android.util.Log.d("MainActivity", "Removing profile listener")
                    listener?.remove()
                }
            }

            // Update status notification when preferences change
            LaunchedEffect(preferences) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationHelper.updateStatusNotification(preferences)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val scope = rememberCoroutineScope()
            
            val backStack = remember { mutableStateListOf<Destination>(Destination.Dashboard) }
            val navigator = rememberListDetailPaneScaffoldNavigator<Destination>()

            AutoGrabberTheme(
                darkTheme = preferences.isDarkMode
            ) {
                BackHandler(enabled = backStack.size > 1) {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                        if (backStack.size == 1) {
                            scope.launch { navigator.navigateBack() }
                        } else {
                            val prev = backStack.last()
                            scope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, prev)
                            }
                        }
                    }
                }

                val background = LocalV2Colors.current.background
                Scaffold(
                    bottomBar = {
                        val currentDest = backStack.lastOrNull()
                        // Only show bottom nav on main screens
                        if (currentDest == Destination.Dashboard || 
                            currentDest is Destination.OfferHistory ||
                            currentDest == Destination.Notifications ||
                            currentDest == Destination.Settings) {
                            V2BottomNavigation(
                                currentDestination = currentDest,
                                hasUnreadNotifications = viewModel.hasUnreadNotifications.collectAsState(initial = false).value,
                                onNavigate = { dest ->
                                    if (backStack.lastOrNull() != dest) {
                                        backStack.clear()
                                        backStack.add(Destination.Dashboard)
                                        if (dest != Destination.Dashboard) {
                                            backStack.add(dest)
                                            scope.launch {
                                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, dest)
                                            }
                                        } else {
                                            // Resetting back to Dashboard on phone means returning from Detail to List
                                            scope.launch {
                                                navigator.navigateBack()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    containerColor = background
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .fillMaxSize(),
                        color = background
                    ) {
                        ListDetailPaneScaffold(
                            directive = navigator.scaffoldDirective,
                            value = navigator.scaffoldValue,
                            listPane = {
                                NavDisplay(
                                    backStack = listOf(Destination.Dashboard),
                                    onBack = { finish() },
                                    entryDecorators = listOf(
                                        rememberViewModelStoreNavEntryDecorator()
                                    ),
                                    entryProvider = { key ->
                                        val factory = MainViewModelFactory(settingsRepository, offerRepository, chatRepository, notificationRepository)
                                        when (key) {
                                            Destination.Dashboard -> NavEntry(key) {
                                                DashboardScreen(
                                                    preferences = preferences,
                                                    isLoggedIn = userProfile != null,
                                                    isApproved = userProfile?.approved ?: false,
                                                    hasLifetimeAccess = userProfile?.hasLifetimeAccess ?: false,
                                                    onNavigateToSettings = {
                                                        backStack.add(Destination.Settings)
                                                        scope.launch {
                                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, Destination.Settings)
                                                        }
                                                    },
                                                    onNavigateToNotifications = {
                                                        backStack.add(Destination.Notifications)
                                                        scope.launch {
                                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, Destination.Notifications)
                                                        }
                                                    },
                                                    onNavigateToAccount = {
                                                        backStack.add(Destination.Account)
                                                        scope.launch {
                                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, Destination.Account)
                                                        }
                                                    },
                                                    onNavigateToPlatformFilters = { platform ->
                                                        val dest = Destination.PlatformFilters(platform)
                                                        backStack.add(dest)
                                                        scope.launch {
                                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, dest)
                                                        }
                                                    },
                                                    onTogglePlatform = { platform, enabled ->
                                                        val newPrefs = when (platform) {
                                                            Platform.SPARK -> preferences.copy(isSparkEnabled = enabled)
                                                            Platform.DOORDASH -> preferences.copy(isDoorDashEnabled = enabled)
                                                            Platform.UBER -> preferences.copy(isUberEnabled = enabled)
                                                            Platform.INSTACART -> preferences.copy(isInstacartEnabled = enabled)
                                                            Platform.FLEX -> preferences.copy(isFlexEnabled = enabled)
                                                        }
                                                        viewModel.updatePreferences(newPrefs)
                                                    }
                                                )
                                            }
                                            else -> NavEntry(key) { /* Only Dashboard in list pane */ }
                                        }
                                    }
                                )
                            },
                            detailPane = {
                                val currentDetail = backStack.lastOrNull()
                                if (currentDetail != null && currentDetail != Destination.Dashboard) {
                                    when (currentDetail) {
                                        Destination.Settings -> {
                                            SettingsScreen(
                                                preferences = preferences,
                                                onPreferencesChanged = { viewModel.updatePreferences(it) },
                                                onNavigateToTroubleshooting = {
                                                    backStack.add(Destination.Troubleshooting)
                                                    scope.launch {
                                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, Destination.Troubleshooting)
                                                    }
                                                },
                                                onNavigateBack = { 
                                                    if (backStack.size > 1) {
                                                        backStack.removeAt(backStack.size - 1)
                                                        if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                    }
                                                }
                                            )
                                        }
                                        Destination.Troubleshooting -> {
                                            com.example.autograbber.ui.settings.TroubleshootingScreen(
                                                onTestNotification = {
                                                    checkAndSendTestNotification()
                                                },
                                                onNavigateBack = {
                                                    if (backStack.size > 1) {
                                                        backStack.removeAt(backStack.size - 1)
                                                        if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                    }
                                                }
                                            )
                                        }
                                        Destination.Notifications -> {
                                            val notificationsViewModel: NotificationsViewModel = viewModel(
                                                factory = MainViewModelFactory(settingsRepository, offerRepository, chatRepository, notificationRepository)
                                            )
                                            NotificationsScreen(
                                                viewModel = notificationsViewModel
                                            )
                                        }
                                        /*
                                        Destination.Chat -> {
                                            com.example.autograbber.ui.chat.ChatScreen(
                                                viewModel = viewModel(factory = MainViewModelFactory(settingsRepository, offerRepository, chatRepository))
                                            )
                                        }
                                        */
                                        Destination.Account -> {
                                            AccountScreen(
                                                onNavigateToSubscription = {
                                                    backStack.add(Destination.Subscription)
                                                    scope.launch {
                                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, Destination.Subscription)
                                                    }
                                                },
                                                onNavigateBack = {
                                                    if (backStack.size > 1) {
                                                        backStack.removeAt(backStack.size - 1)
                                                        if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                    }
                                                }
                                            )
                                        }
                                        Destination.Subscription -> {
                                            com.example.autograbber.ui.account.SubscriptionScreen(
                                                hasLifetimeAccess = userProfile?.hasLifetimeAccess ?: false,
                                                onUnlockLifetime = {
                                                    userProfile?.let { profile ->
                                                        scope.launch {
                                                            val updated = profile.copy(hasLifetimeAccess = true)
                                                            if (userRepository.saveUserProfile(updated)) {
                                                                userProfile = updated
                                                                android.widget.Toast.makeText(this@MainActivity, "Lifetime Access Unlocked!", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                onNavigateBack = {
                                                    if (backStack.size > 1) {
                                                        backStack.removeAt(backStack.size - 1)
                                                        if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                    }
                                                }
                                            )
                                        }
                                        is Destination.OfferHistory -> {
                                            val platform = currentDetail.platform
                                            val offers by if (platform == null) {
                                                viewModel.allOffers.collectAsState(initial = emptyList())
                                            } else {
                                                viewModel.getOffersByPlatform(platform).collectAsState(initial = emptyList())
                                            }
                                            OfferHistoryScreen(
                                                platform = platform,
                                                offers = offers,
                                                onClearHistory = { p -> 
                                                    if (p != null) {
                                                        viewModel.clearOfferHistory(p)
                                                    } else {
                                                        viewModel.clearAllOfferHistory()
                                                    }
                                                },
                                                onNavigateBack = {
                                                    if (backStack.size > 1) {
                                                        backStack.removeAt(backStack.size - 1)
                                                        if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                    }
                                                }
                                            )
                                        }
                                            is Destination.PlatformFilters -> {
                                                val platform = currentDetail.platform
                                                val filters = when (platform) {
                                                    Platform.SPARK -> preferences.sparkFilters
                                                    Platform.DOORDASH -> preferences.doorDashFilters
                                                    Platform.UBER -> preferences.uberFilters
                                                    Platform.INSTACART -> preferences.instacartFilters
                                                    Platform.FLEX -> preferences.flexFilters
                                                }
                                                PlatformFilterScreen(
                                                    platform = platform,
                                                    filters = filters,
                                                    onFiltersChanged = { newFilters ->
                                                        val newPrefs = when (platform) {
                                                            Platform.SPARK -> preferences.copy(sparkFilters = newFilters)
                                                            Platform.DOORDASH -> preferences.copy(doorDashFilters = newFilters)
                                                            Platform.UBER -> preferences.copy(uberFilters = newFilters)
                                                            Platform.INSTACART -> preferences.copy(instacartFilters = newFilters)
                                                            Platform.FLEX -> preferences.copy(flexFilters = newFilters)
                                                        }
                                                        viewModel.updatePreferences(newPrefs)
                                                    },
                                                    onNavigateBack = {
                                                        if (backStack.size > 1) {
                                                            backStack.removeAt(backStack.size - 1)
                                                            if (backStack.size == 1) scope.launch { navigator.navigateBack() }
                                                        }
                                                    }
                                                )
                                            }
                                        else -> {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndSendTestNotification() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                notificationHelper.sendTestNotification()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun V2BottomNavigation(
    currentDestination: Destination?,
    hasUnreadNotifications: Boolean,
    onNavigate: (Destination) -> Unit
) {
    val colors = LocalV2Colors.current
    Surface(
        color = colors.background,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Dashboard",
                icon = Icons.Default.GridView,
                isSelected = currentDestination == Destination.Dashboard,
                onClick = { onNavigate(Destination.Dashboard) }
            )
            NavItem(
                label = "History",
                icon = Icons.Default.History,
                isSelected = currentDestination is Destination.OfferHistory && currentDestination.platform == null,
                onClick = { onNavigate(Destination.OfferHistory(null)) }
            )
            /*
            NavItem(
                label = "Chat",
                icon = Icons.Default.QuestionAnswer,
                isSelected = currentDestination == Destination.Chat,
                onClick = { onNavigate(Destination.Chat) }
            )
            */
            NavItem(
                label = "Notifications",
                icon = Icons.Default.Notifications,
                isSelected = currentDestination == Destination.Notifications,
                showDot = hasUnreadNotifications,
                onClick = { onNavigate(Destination.Notifications) }
            )
            NavItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                isSelected = currentDestination == Destination.Settings,
                onClick = { onNavigate(Destination.Settings) }
            )
        }
    }
}

@Composable
fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null // Removes the blue box/ripple
            ) { onClick() }
            .padding(8.dp)
    ) {
        val colors = LocalV2Colors.current
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) V2Primary else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .offset(x = 18.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .border(1.5.dp, colors.background, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) V2Primary else colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
