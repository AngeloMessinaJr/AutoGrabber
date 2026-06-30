package com.example.autograbber.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.PlatformFilters
import com.example.autograbber.ui.theme.*
import com.example.autograbber.utils.getPlatformPackageNames
import java.util.Locale

private enum class EditingFilter {
    NONE, MIN_PAY, MIN_TOTAL, MAX_DIST, MAX_ITEMS, MAX_STOPS, MAX_CUSTOMERS, FLEX_LENGTHS
}

private val VALID_FLEX_LENGTHS = listOf(
    "Less than 1 hour",
    "1 hour - 2 hours",
    "2 hours - 3 hours",
    "3 hours - 4 hours",
    "4 hours +"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformFilterScreen(
    platform: Platform,
    filters: PlatformFilters,
    onFiltersChanged: (PlatformFilters) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val platformName = when (platform) {
        Platform.SPARK -> "Spark Preferences"
        Platform.DOORDASH -> "DoorDash Preferences"
        Platform.UBER -> "Uber Preferences"
        Platform.INSTACART -> "Instacart Preferences"
        Platform.FLEX -> "Flex Preferences"
    }

    var showStoreDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var editingFilter by remember { mutableStateOf(EditingFilter.NONE) }
    val colors = LocalV2Colors.current

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
                    .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                }
                
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
                    text = platformName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            
            HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.05f))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "OFFER FILTERS",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterGridItem(
                                label = "Minimum Pay",
                                value = String.format(Locale.US, "$%.2f", filters.minTotalPay),
                                onClick = { editingFilter = EditingFilter.MIN_TOTAL },
                                modifier = Modifier.weight(1f)
                            )
                            if (platform != Platform.FLEX) {
                                FilterGridItem(
                                    label = "Minimum Pay/Mile",
                                    value = String.format(Locale.US, "$%.2f", filters.minPayPerMile),
                                    onClick = { editingFilter = EditingFilter.MIN_PAY },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        if (platform == Platform.FLEX) {
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "BLOCK LENGTHS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                TextButton(
                                    onClick = { editingFilter = EditingFilter.FLEX_LENGTHS },
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Text("Select Lengths", color = V2Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            
                            if (filters.flexBlockLengths.isEmpty()) {
                                Text(
                                    "You have nothing selected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val visibleLengths = filters.flexBlockLengths.filter { it in VALID_FLEX_LENGTHS }
                                    if (visibleLengths.isEmpty()) {
                                        Text(
                                            "You have nothing selected.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textSecondary.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        visibleLengths.chunked(2).forEach { rowLengths ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                rowLengths.forEach { label ->
                                                    Surface(
                                                        color = V2Primary.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, V2Primary.copy(alpha = 0.3f)),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = colors.textPrimary,
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                    }
                                                }
                                                if (rowLengths.size == 1) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (platform != Platform.FLEX) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FilterGridItem(
                                    label = "Maximum Distance",
                                    value = "0-${filters.maxDistanceMiles.toInt()} miles",
                                    onClick = { editingFilter = EditingFilter.MAX_DIST },
                                    modifier = Modifier.weight(1f)
                                )
                                if (platform == Platform.INSTACART || platform == Platform.SPARK) {
                                    FilterGridItem(
                                        label = if (platform == Platform.INSTACART) "Maximum Items" else "Maximum Stops",
                                        value = if (platform == Platform.INSTACART) filters.maxItemCount.toString() else filters.maxStops.toString(),
                                        onClick = {
                                            if (platform == Platform.INSTACART) editingFilter =
                                                EditingFilter.MAX_ITEMS else editingFilter =
                                                EditingFilter.MAX_STOPS
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (platform == Platform.INSTACART) {
                                    FilterGridItem(
                                        label = "Maximum Customers",
                                        value = filters.maxCustomers.toString(),
                                        onClick = { editingFilter = EditingFilter.MAX_CUSTOMERS },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Filler if only one item in second row for 3-item grid
                                if (platform != Platform.INSTACART && platform != Platform.SPARK) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                item { SectionHeader("Automation") }

                item {
                    PreferenceSwitch(
                        label = if (platform == Platform.FLEX) "Automatic Schedule" else "Automatic Accept",
                        description = if (platform == Platform.FLEX) "Automatically schedule blocks that match your criteria." else "Automatically accept offers that match your criteria.",
                        checked = filters.autoAccept,
                        onCheckedChange = { onFiltersChanged(filters.copy(autoAccept = it)) }
                    )
                }

                if (platform == Platform.FLEX) {
                    item {
                        PreferenceSwitch(
                            label = "Automatic Refresh",
                            description = "Keep the offers page constantly updated.",
                            checked = filters.autoRefresh,
                            onCheckedChange = { onFiltersChanged(filters.copy(autoRefresh = it)) }
                        )
                    }
                }

                if (platform != Platform.FLEX) {
                    item {
                        PreferenceSwitch(
                            label = if (platform == Platform.INSTACART) "Automatic Hide" else "Automatic Reject",
                            description = if (platform == Platform.INSTACART) 
                                "Hide offers that do not match your criteria."
                                else "Automatically reject offers that do not match your criteria",
                            checked = filters.autoReject,
                            onCheckedChange = { onFiltersChanged(filters.copy(autoReject = it)) }
                        )
                    }
                }

                if (platform == Platform.INSTACART || platform == Platform.DOORDASH) {
                    item { SectionHeader("Preferences") }

                    if (platform == Platform.INSTACART) {
                        item {
                            PreferenceSwitch(
                                label = "Multiple Stores",
                                description = "Automatically hide multi-store batches.",
                                checked = filters.autoHideMultiRetailer,
                                onCheckedChange = { onFiltersChanged(filters.copy(autoHideMultiRetailer = it)) }
                            )
                        }
                    }

                    item {
                        Surface(
                            color = colors.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (platform == Platform.DOORDASH) "Blocked Merchants" else "Blocked Stores",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    IconButton(onClick = { showStoreDialog = true }) {
                                        Icon(Icons.Default.Add, null, tint = V2Primary)
                                    }
                                }
                                
                                if (filters.blockedStores.isEmpty()) {
                                    Text(
                                        if (platform == Platform.DOORDASH) "No merchants currently blocked." else "No stores currently blocked.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        filters.blockedStores.forEach { store ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.textPrimary.copy(alpha = 0.05f))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(store, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                                IconButton(
                                                    onClick = { 
                                                        onFiltersChanged(filters.copy(blockedStores = filters.blockedStores - store))
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, null, tint = V2Error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (platform == Platform.SPARK) {
                    item { SectionHeader("Preferences") }
                    item {
                        PreferenceSwitch(
                            label = "Apartments",
                            description = "Automatically reject offers with this tag.",
                            checked = filters.excludeApartments,
                            onCheckedChange = { onFiltersChanged(filters.copy(excludeApartments = it)) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Filter Edit Dialogs
    when (editingFilter) {
        EditingFilter.MIN_PAY -> FilterEditDialog(
            label = "Minimum Pay/Mile",
            value = filters.minPayPerMile.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(minPayPerMile = it.toDouble())) },
            valueRange = 0.5f..10.0f,
            isDecimal = true,
            unit = "/mi",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.MIN_TOTAL -> FilterEditDialog(
            label = "Minimum Pay",
            value = filters.minTotalPay.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(minTotalPay = it.toDouble())) },
            valueRange = 1.0f..150.0f,
            isDecimal = true,
            unit = "$",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.MAX_DIST -> FilterEditDialog(
            label = "Maximum Distance",
            value = filters.maxDistanceMiles.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(maxDistanceMiles = it.toDouble())) },
            valueRange = 1f..100f,
            isDecimal = false,
            unit = " miles",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.MAX_ITEMS -> FilterEditDialog(
            label = "Maximum Items",
            value = filters.maxItemCount.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(maxItemCount = it.toInt())) },
            valueRange = 1f..150f,
            isDecimal = false,
            unit = " items",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.MAX_STOPS -> FilterEditDialog(
            label = "Maximum Stops",
            value = filters.maxStops.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(maxStops = it.toInt())) },
            valueRange = 2f..26f,
            isDecimal = false,
            unit = " stops",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.MAX_CUSTOMERS -> FilterEditDialog(
            label = "Maximum Customers",
            value = filters.maxCustomers.toFloat(),
            onValueChange = { onFiltersChanged(filters.copy(maxCustomers = it.toInt())) },
            valueRange = 1f..4f,
            isDecimal = false,
            unit = " customers",
            onDismiss = { editingFilter = EditingFilter.NONE }
        )
        EditingFilter.FLEX_LENGTHS -> {
            AlertDialog(
                onDismissRequest = { editingFilter = EditingFilter.NONE },
                containerColor = colors.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Block Lengths",
                        color = colors.textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VALID_FLEX_LENGTHS.forEach { label ->
                            val isChecked = filters.flexBlockLengths.contains(label)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val newList = if (isChecked) filters.flexBlockLengths - label else filters.flexBlockLengths + label
                                        onFiltersChanged(filters.copy(flexBlockLengths = newList.filter { it in VALID_FLEX_LENGTHS }))
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) V2Primary.copy(alpha = 0.1f) else colors.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, 
                                    if (isChecked) V2Primary else colors.textPrimary.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = if (isChecked) V2Primary else colors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = V2Primary,
                                            uncheckedColor = colors.textSecondary.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { editingFilter = EditingFilter.NONE },
                        colors = ButtonDefaults.buttonColors(containerColor = V2Primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
        EditingFilter.NONE -> {}
    }

    if (showStoreDialog) {
        AlertDialog(
            onDismissRequest = { showStoreDialog = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = { 
                Text(
                    text = if (platform == Platform.DOORDASH) "Block Merchant" else "Block Store", 
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                ) 
            },
            text = {
                OutlinedTextField(
                    value = newStoreName,
                    onValueChange = { newStoreName = it },
                    label = { Text("Enter Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStoreName.isNotBlank()) {
                            onFiltersChanged(filters.copy(blockedStores = filters.blockedStores + newStoreName.trim()))
                            newStoreName = ""
                            showStoreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = V2Primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Block", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStoreDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
