package com.example.autograbber.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.OfferAction
import com.example.autograbber.data.models.Platform
import com.example.autograbber.ui.theme.LocalV2Colors
import com.example.autograbber.ui.theme.V2Error
import com.example.autograbber.ui.theme.V2Primary
import com.example.autograbber.ui.theme.V2Success
import com.example.autograbber.utils.getPlatformPackageNames
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferHistoryScreen(
    platform: Platform?, // null means show all
    offers: List<GigOffer>,
    onClearHistory: (Platform?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<Platform?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val colors = LocalV2Colors.current
    val sheetState = rememberModalBottomSheetState()
    
    val filteredOffers = remember(offers, selectedFilter, platform) {
        if (platform != null) {
            offers // Already filtered by ViewModel if platform is passed
        } else {
            if (selectedFilter == null) offers
            else offers.filter { it.platform == selectedFilter }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            if (platform == null) {
                FloatingActionButton(
                    onClick = { showFilterMenu = true },
                    containerColor = V2Primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            // Inline header for specific platform view or General History
            if (platform != null) {
                val context = LocalContext.current
                val packageManager = context.packageManager
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (appIcon != null) {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = when(platform) {
                                Platform.SPARK -> "Spark (Coming Soon)"
                                Platform.DOORDASH -> "DoorDash"
                                Platform.UBER -> "Uber (Coming Soon)"
                                Platform.INSTACART -> "Instacart"
                                Platform.FLEX -> "Flex"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    
                    if (filteredOffers.isNotEmpty()) {
                        TextButton(onClick = { onClearHistory(platform) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                }
                HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.05f))
            }

            if (filteredOffers.isNotEmpty() && platform == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onClearHistory(platform ?: selectedFilter) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }

            if (filteredOffers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colors.textSecondary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No offers detected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOffers) { offer ->
                        OfferItem(offer)
                    }
                }
            }
        }

        if (showFilterMenu) {
            ModalBottomSheet(
                onDismissRequest = { showFilterMenu = false },
                sheetState = sheetState,
                containerColor = colors.surface,
                contentColor = colors.textPrimary,
                dragHandle = { BottomSheetDefaults.DragHandle(color = colors.textSecondary.copy(alpha = 0.3f)) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        "Filter By Platform",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    FilterMenuItem("All Platforms", selectedFilter == null) {
                        selectedFilter = null
                        showFilterMenu = false
                    }
                    FilterMenuItem("Instacart", selectedFilter == Platform.INSTACART) {
                        selectedFilter = Platform.INSTACART
                        showFilterMenu = false
                    }
                    FilterMenuItem("DoorDash", selectedFilter == Platform.DOORDASH) {
                        selectedFilter = Platform.DOORDASH
                        showFilterMenu = false
                    }
                    FilterMenuItem("Flex", selectedFilter == Platform.FLEX) {
                        selectedFilter = Platform.FLEX
                        showFilterMenu = false
                    }
                    FilterMenuItem("Spark (Coming Soon)", selectedFilter == Platform.SPARK) {
                        selectedFilter = Platform.SPARK
                        showFilterMenu = false
                    }
                    FilterMenuItem("Uber (Coming Soon)", selectedFilter == Platform.UBER) {
                        selectedFilter = Platform.UBER
                        showFilterMenu = false
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterMenuItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalV2Colors.current
    Surface(
        color = if (isSelected) V2Primary.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = V2Primary, unselectedColor = colors.textSecondary.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) V2Primary else colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun OfferItem(offer: GigOffer) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = configuration.locales[0]
    val dateFormat = remember(locale) { SimpleDateFormat("MMM d, h:mm a", locale) }
    val dateString = remember(offer.timestamp, dateFormat) { dateFormat.format(Date(offer.timestamp)) }
    val colors = LocalV2Colors.current

    val context = LocalContext.current
    val packageManager = context.packageManager
    val appIcon = remember(offer.platform) {
        val packages = getPlatformPackageNames(offer.platform)
        var icon: android.graphics.drawable.Drawable? = null
        for (pkg in packages) {
            try {
                icon = packageManager.getApplicationIcon(pkg)
                break
            } catch (_: Exception) {}
        }
        icon
    }

    val statusColor = when (offer.action) {
        OfferAction.ACCEPTED -> V2Success
        OfferAction.REJECTED -> V2Error
        OfferAction.HIDDEN -> V2Error
        OfferAction.IGNORED -> colors.textSecondary.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Store and Pay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (appIcon != null) {
                        AsyncImage(
                            model = appIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Column {
                        when (offer.platform) {
                            Platform.INSTACART if !offer.storeDetails.isNullOrEmpty() -> {
                                offer.storeDetails.forEach { store ->
                                    StoreItem(store)
                                }
                            }
                            Platform.INSTACART -> {
                                StoreItem(com.example.autograbber.data.models.StoreDetail(offer.storeName))
                            }
                            else -> {
                                Text(
                                    text = offer.storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = String.format(Locale.US, "$%.2f", offer.pay),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Middle Row: Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (offer.platform != Platform.FLEX && offer.platform != Platform.DOORDASH) {
                        Text(
                            text = if (offer.platform == Platform.INSTACART) {
                                val customerText = if (offer.stops == 1) "customer" else "customers"
                                "${offer.distance} mi (${String.format(Locale.US, "$%.2f/mi", offer.payPerMile)}) • ${offer.stops} $customerText"
                            } else {
                                val stopText = if (offer.stops == 1) "stop" else "stops"
                                "${offer.distance} mi (${String.format(Locale.US, "$%.2f/mi", offer.payPerMile)}) • ${offer.stops} $stopText"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else if (offer.platform == Platform.DOORDASH) {
                        Text(
                            text = "${offer.distance} mi (${String.format(Locale.US, "$%.2f/mi", offer.payPerMile)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    if (!offer.itemCount.isNullOrEmpty()) {
                        Text(
                            text = offer.itemCount,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Row: Status Badge
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
            ) {
                val displayStatus = when (offer.platform) {
                    Platform.INSTACART -> {
                        when (offer.action) {
                            OfferAction.HIDDEN -> "Batch Hidden"
                            OfferAction.ACCEPTED -> "Auto Accepted"
                            else -> offer.action.name
                        }
                    }
                    Platform.DOORDASH -> {
                        when (offer.action) {
                            OfferAction.ACCEPTED -> "AUTO ACCEPTED"
                            OfferAction.REJECTED -> "AUTO REJECTED"
                            else -> offer.action.name
                        }
                    }
                    else -> {
                        val actionName = if (offer.action == OfferAction.HIDDEN) "HIDDEN" else offer.action.name
                        actionName + (if (offer.reason != null) ": ${offer.reason}" else "")
                    }
                }

                Text(
                    text = displayStatus,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun StoreItem(store: com.example.autograbber.data.models.StoreDetail) {
    var showAddress by remember { mutableStateOf(false) }
    val colors = LocalV2Colors.current
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                enabled = !store.address.isNullOrEmpty(),
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showAddress = !showAddress }
        ) {
            Text(
                text = store.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            if (!store.address.isNullOrEmpty()) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Address",
                    tint = V2Primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                )
            }
        }
        if (showAddress && !store.address.isNullOrEmpty()) {
            Text(
                text = store.address,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}
