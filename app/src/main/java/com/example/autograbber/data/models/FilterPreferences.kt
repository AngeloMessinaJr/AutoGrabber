package com.example.autograbber.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PlatformFilters(
    val minPayPerMile: Double = 1.5,
    val maxDistanceMiles: Double = 10.0,
    val minTotalPay: Double = 5.0,
    val maxStops: Int = 2,
    val excludeApartments: Boolean = false,
    val autoAccept: Boolean = false,
    val autoReject: Boolean = true,
    val autoRefresh: Boolean = false,
    
    // Advanced Instacart/Specific Filters
    val maxItemCount: Int = 50,
    val maxCustomers: Int = 3, // 1-3 commonly, up to 4 for some
    val autoHideMultiRetailer: Boolean = false,
    val blockedStores: List<String> = emptyList(),
    
    // Flex specific
    val flexStartTime: String = "12:00 AM",
    val flexEndTime: String = "12:00 AM",
    val flexBlockLengths: List<String> = listOf("Less than 1 hour", "1 hour - 2 hours", "2 hours - 3 hours", "3 hours - 4 hours", "4 hours +")
)

@Serializable
data class FilterPreferences(
    val sparkFilters: PlatformFilters = PlatformFilters(),
    val doorDashFilters: PlatformFilters = PlatformFilters(),
    val uberFilters: PlatformFilters = PlatformFilters(),
    val instacartFilters: PlatformFilters = PlatformFilters(),
    val flexFilters: PlatformFilters = PlatformFilters(),
    val isSparkEnabled: Boolean = true,
    val isDoorDashEnabled: Boolean = true,
    val isUberEnabled: Boolean = true,
    val isInstacartEnabled: Boolean = true,
    val isFlexEnabled: Boolean = true,
    val isAutoAcceptEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    
    // Mocked Stats for V2 UI
    val todaysEarnings: Double = 156.75,
    val earningsGrowth: Double = 23.6,
    val totalTrips: Int = 12,
    val tripsIncrease: Int = 2,
    val onlineTimeHours: Int = 4,
    val onlineTimeMinutes: Int = 32,
    val onlineTimeIncreaseHours: Int = 1,
    val onlineTimeIncreaseMinutes: Int = 5
)
