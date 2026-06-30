package com.example.autograbber.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.autograbber.data.models.FilterPreferences
import com.example.autograbber.data.models.PlatformFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SPARK_MIN_PAY = doublePreferencesKey("spark_min_pay")
        val SPARK_MAX_DIST = doublePreferencesKey("spark_max_dist")
        val SPARK_MIN_TOTAL_PAY = doublePreferencesKey("spark_min_total_pay")
        val SPARK_MAX_STOPS = intPreferencesKey("spark_max_stops")
        val SPARK_EXCLUDE_APTS = booleanPreferencesKey("spark_exclude_apts")
        val SPARK_AUTO_ACCEPT = booleanPreferencesKey("spark_auto_accept")
        val SPARK_AUTO_REJECT = booleanPreferencesKey("spark_auto_reject")

        val DOORDASH_MIN_PAY = doublePreferencesKey("doordash_min_pay")
        val DOORDASH_MAX_DIST = doublePreferencesKey("doordash_max_dist")
        val DOORDASH_MIN_TOTAL_PAY = doublePreferencesKey("doordash_min_total_pay")
        val DOORDASH_MAX_STOPS = intPreferencesKey("doordash_max_stops")
        val DOORDASH_EXCLUDE_APTS = booleanPreferencesKey("doordash_exclude_apts")
        val DOORDASH_BLOCKED_STORES = stringSetPreferencesKey("doordash_blocked_stores")
        val DOORDASH_AUTO_ACCEPT = booleanPreferencesKey("doordash_auto_accept")
        val DOORDASH_AUTO_REJECT = booleanPreferencesKey("doordash_auto_reject")

        val UBER_MIN_PAY = doublePreferencesKey("uber_min_pay")
        val UBER_MAX_DIST = doublePreferencesKey("uber_max_dist")
        val UBER_MIN_TOTAL_PAY = doublePreferencesKey("uber_min_total_pay")
        val UBER_MAX_STOPS = intPreferencesKey("uber_max_stops")
        val UBER_EXCLUDE_APTS = booleanPreferencesKey("uber_exclude_apts")
        val UBER_AUTO_ACCEPT = booleanPreferencesKey("uber_auto_accept")
        val UBER_AUTO_REJECT = booleanPreferencesKey("uber_auto_reject")
        
        val INSTACART_MIN_PAY = doublePreferencesKey("instacart_min_pay")
        val INSTACART_MAX_DIST = doublePreferencesKey("instacart_max_dist")
        val INSTACART_MIN_TOTAL_PAY = doublePreferencesKey("instacart_min_total_pay")
        val INSTACART_MAX_ITEMS = intPreferencesKey("instacart_max_items")
        val INSTACART_MAX_CUSTOMERS = intPreferencesKey("instacart_max_customers")
        val INSTACART_AUTO_HIDE_MULTI = booleanPreferencesKey("instacart_auto_hide_multi")
        val INSTACART_BLOCKED_STORES = stringSetPreferencesKey("instacart_blocked_stores")
        val INSTACART_AUTO_ACCEPT = booleanPreferencesKey("instacart_auto_accept")
        val INSTACART_AUTO_REJECT = booleanPreferencesKey("instacart_auto_reject")

        val FLEX_MIN_PAY = doublePreferencesKey("flex_min_pay")
        val FLEX_MAX_DIST = doublePreferencesKey("flex_max_dist")
        val FLEX_MIN_TOTAL_PAY = doublePreferencesKey("flex_min_total_pay")
        val FLEX_MAX_STOPS = intPreferencesKey("flex_max_stops")
        val FLEX_EXCLUDE_APTS = booleanPreferencesKey("flex_exclude_apts")
        val FLEX_AUTO_ACCEPT = booleanPreferencesKey("flex_auto_accept")
        val FLEX_AUTO_REJECT = booleanPreferencesKey("flex_auto_reject")
        val FLEX_START_TIME = stringPreferencesKey("flex_start_time")
        val FLEX_END_TIME = stringPreferencesKey("flex_end_time")
        val FLEX_BLOCK_LENGTHS = stringSetPreferencesKey("flex_block_lengths")
        val FLEX_AUTO_REFRESH = booleanPreferencesKey("flex_auto_refresh")

        val SPARK_ENABLED = booleanPreferencesKey("spark_enabled")
        val DOORDASH_ENABLED = booleanPreferencesKey("doordash_enabled")
        val UBER_ENABLED = booleanPreferencesKey("uber_enabled")
        val INSTACART_ENABLED = booleanPreferencesKey("instacart_enabled")
        val FLEX_ENABLED = booleanPreferencesKey("flex_enabled")
        val IS_AUTO_ACCEPT_ENABLED = booleanPreferencesKey("is_auto_accept_enabled")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    val filterPreferencesFlow: Flow<FilterPreferences> = context.dataStore.data
        .map { preferences ->
            FilterPreferences(
                sparkFilters = PlatformFilters(
                    minPayPerMile = preferences[PreferencesKeys.SPARK_MIN_PAY] ?: 1.5,
                    maxDistanceMiles = preferences[PreferencesKeys.SPARK_MAX_DIST] ?: 10.0,
                    minTotalPay = preferences[PreferencesKeys.SPARK_MIN_TOTAL_PAY] ?: 5.0,
                    maxStops = preferences[PreferencesKeys.SPARK_MAX_STOPS] ?: 2,
                    excludeApartments = preferences[PreferencesKeys.SPARK_EXCLUDE_APTS] ?: false,
                    autoAccept = preferences[PreferencesKeys.SPARK_AUTO_ACCEPT] ?: false,
                    autoReject = preferences[PreferencesKeys.SPARK_AUTO_REJECT] ?: true
                ),
                doorDashFilters = PlatformFilters(
                    minPayPerMile = preferences[PreferencesKeys.DOORDASH_MIN_PAY] ?: 1.5,
                    maxDistanceMiles = preferences[PreferencesKeys.DOORDASH_MAX_DIST] ?: 10.0,
                    minTotalPay = preferences[PreferencesKeys.DOORDASH_MIN_TOTAL_PAY] ?: 5.0,
                    maxStops = preferences[PreferencesKeys.DOORDASH_MAX_STOPS] ?: 2,
                    excludeApartments = preferences[PreferencesKeys.DOORDASH_EXCLUDE_APTS] ?: false,
                    blockedStores = preferences[PreferencesKeys.DOORDASH_BLOCKED_STORES]?.toList() ?: emptyList(),
                    autoAccept = preferences[PreferencesKeys.DOORDASH_AUTO_ACCEPT] ?: false,
                    autoReject = preferences[PreferencesKeys.DOORDASH_AUTO_REJECT] ?: true
                ),
                uberFilters = PlatformFilters(
                    minPayPerMile = preferences[PreferencesKeys.UBER_MIN_PAY] ?: 1.5,
                    maxDistanceMiles = preferences[PreferencesKeys.UBER_MAX_DIST] ?: 10.0,
                    minTotalPay = preferences[PreferencesKeys.UBER_MIN_TOTAL_PAY] ?: 5.0,
                    maxStops = preferences[PreferencesKeys.UBER_MAX_STOPS] ?: 2,
                    excludeApartments = preferences[PreferencesKeys.UBER_EXCLUDE_APTS] ?: false,
                    autoAccept = preferences[PreferencesKeys.UBER_AUTO_ACCEPT] ?: false,
                    autoReject = preferences[PreferencesKeys.UBER_AUTO_REJECT] ?: true
                ),
                instacartFilters = PlatformFilters(
                    minPayPerMile = preferences[PreferencesKeys.INSTACART_MIN_PAY] ?: 1.5,
                    maxDistanceMiles = preferences[PreferencesKeys.INSTACART_MAX_DIST] ?: 10.0,
                    minTotalPay = preferences[PreferencesKeys.INSTACART_MIN_TOTAL_PAY] ?: 5.0,
                    maxItemCount = preferences[PreferencesKeys.INSTACART_MAX_ITEMS] ?: 50,
                    maxCustomers = preferences[PreferencesKeys.INSTACART_MAX_CUSTOMERS] ?: 3,
                    autoHideMultiRetailer = preferences[PreferencesKeys.INSTACART_AUTO_HIDE_MULTI] ?: false,
                    blockedStores = preferences[PreferencesKeys.INSTACART_BLOCKED_STORES]?.toList() ?: emptyList(),
                    autoAccept = preferences[PreferencesKeys.INSTACART_AUTO_ACCEPT] ?: false,
                    autoReject = preferences[PreferencesKeys.INSTACART_AUTO_REJECT] ?: true
                ),
                flexFilters = PlatformFilters(
                    minPayPerMile = preferences[PreferencesKeys.FLEX_MIN_PAY] ?: 1.5,
                    maxDistanceMiles = preferences[PreferencesKeys.FLEX_MAX_DIST] ?: 10.0,
                    minTotalPay = preferences[PreferencesKeys.FLEX_MIN_TOTAL_PAY] ?: 5.0,
                    maxStops = preferences[PreferencesKeys.FLEX_MAX_STOPS] ?: 2,
                    excludeApartments = preferences[PreferencesKeys.FLEX_EXCLUDE_APTS] ?: false,
                    autoAccept = preferences[PreferencesKeys.FLEX_AUTO_ACCEPT] ?: false,
                    autoReject = preferences[PreferencesKeys.FLEX_AUTO_REJECT] ?: true,
                    flexStartTime = preferences[PreferencesKeys.FLEX_START_TIME] ?: "12:00 AM",
                    flexEndTime = preferences[PreferencesKeys.FLEX_END_TIME] ?: "12:00 AM",
                    flexBlockLengths = preferences[PreferencesKeys.FLEX_BLOCK_LENGTHS]?.toList() ?: listOf("Less than 1 hour", "1 hour - 2 hours", "2 hours - 3 hours", "3 hours - 4 hours", "4 hours +"),
                    autoRefresh = preferences[PreferencesKeys.FLEX_AUTO_REFRESH] ?: false
                ),
                isSparkEnabled = preferences[PreferencesKeys.SPARK_ENABLED] ?: true,
                isDoorDashEnabled = preferences[PreferencesKeys.DOORDASH_ENABLED] ?: true,
                isUberEnabled = preferences[PreferencesKeys.UBER_ENABLED] ?: true,
                isInstacartEnabled = preferences[PreferencesKeys.INSTACART_ENABLED] ?: true,
                isFlexEnabled = preferences[PreferencesKeys.FLEX_ENABLED] ?: true,
                isAutoAcceptEnabled = preferences[PreferencesKeys.IS_AUTO_ACCEPT_ENABLED] ?: true,
                isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: true
            )
        }

    suspend fun updateFilterPreferences(filterPreferences: FilterPreferences) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPARK_MIN_PAY] = filterPreferences.sparkFilters.minPayPerMile
            preferences[PreferencesKeys.SPARK_MAX_DIST] = filterPreferences.sparkFilters.maxDistanceMiles
            preferences[PreferencesKeys.SPARK_MIN_TOTAL_PAY] = filterPreferences.sparkFilters.minTotalPay
            preferences[PreferencesKeys.SPARK_MAX_STOPS] = filterPreferences.sparkFilters.maxStops
            preferences[PreferencesKeys.SPARK_EXCLUDE_APTS] = filterPreferences.sparkFilters.excludeApartments
            preferences[PreferencesKeys.SPARK_AUTO_ACCEPT] = filterPreferences.sparkFilters.autoAccept
            preferences[PreferencesKeys.SPARK_AUTO_REJECT] = filterPreferences.sparkFilters.autoReject

            preferences[PreferencesKeys.DOORDASH_MIN_PAY] = filterPreferences.doorDashFilters.minPayPerMile
            preferences[PreferencesKeys.DOORDASH_MAX_DIST] = filterPreferences.doorDashFilters.maxDistanceMiles
            preferences[PreferencesKeys.DOORDASH_MIN_TOTAL_PAY] = filterPreferences.doorDashFilters.minTotalPay
            preferences[PreferencesKeys.DOORDASH_MAX_STOPS] = filterPreferences.doorDashFilters.maxStops
            preferences[PreferencesKeys.DOORDASH_EXCLUDE_APTS] = filterPreferences.doorDashFilters.excludeApartments
            preferences[PreferencesKeys.DOORDASH_BLOCKED_STORES] = filterPreferences.doorDashFilters.blockedStores.toSet()
            preferences[PreferencesKeys.DOORDASH_AUTO_ACCEPT] = filterPreferences.doorDashFilters.autoAccept
            preferences[PreferencesKeys.DOORDASH_AUTO_REJECT] = filterPreferences.doorDashFilters.autoReject

            preferences[PreferencesKeys.UBER_MIN_PAY] = filterPreferences.uberFilters.minPayPerMile
            preferences[PreferencesKeys.UBER_MAX_DIST] = filterPreferences.uberFilters.maxDistanceMiles
            preferences[PreferencesKeys.UBER_MIN_TOTAL_PAY] = filterPreferences.uberFilters.minTotalPay
            preferences[PreferencesKeys.UBER_MAX_STOPS] = filterPreferences.uberFilters.maxStops
            preferences[PreferencesKeys.UBER_EXCLUDE_APTS] = filterPreferences.uberFilters.excludeApartments
            preferences[PreferencesKeys.UBER_AUTO_ACCEPT] = filterPreferences.uberFilters.autoAccept
            preferences[PreferencesKeys.UBER_AUTO_REJECT] = filterPreferences.uberFilters.autoReject

            preferences[PreferencesKeys.INSTACART_MIN_PAY] = filterPreferences.instacartFilters.minPayPerMile
            preferences[PreferencesKeys.INSTACART_MAX_DIST] = filterPreferences.instacartFilters.maxDistanceMiles
            preferences[PreferencesKeys.INSTACART_MIN_TOTAL_PAY] = filterPreferences.instacartFilters.minTotalPay
            preferences[PreferencesKeys.INSTACART_MAX_ITEMS] = filterPreferences.instacartFilters.maxItemCount
            preferences[PreferencesKeys.INSTACART_MAX_CUSTOMERS] = filterPreferences.instacartFilters.maxCustomers
            preferences[PreferencesKeys.INSTACART_AUTO_HIDE_MULTI] = filterPreferences.instacartFilters.autoHideMultiRetailer
            preferences[PreferencesKeys.INSTACART_BLOCKED_STORES] = filterPreferences.instacartFilters.blockedStores.toSet()
            preferences[PreferencesKeys.INSTACART_AUTO_ACCEPT] = filterPreferences.instacartFilters.autoAccept
            preferences[PreferencesKeys.INSTACART_AUTO_REJECT] = filterPreferences.instacartFilters.autoReject

            preferences[PreferencesKeys.FLEX_MIN_PAY] = filterPreferences.flexFilters.minPayPerMile
            preferences[PreferencesKeys.FLEX_MAX_DIST] = filterPreferences.flexFilters.maxDistanceMiles
            preferences[PreferencesKeys.FLEX_MIN_TOTAL_PAY] = filterPreferences.flexFilters.minTotalPay
            preferences[PreferencesKeys.FLEX_MAX_STOPS] = filterPreferences.flexFilters.maxStops
            preferences[PreferencesKeys.FLEX_EXCLUDE_APTS] = filterPreferences.flexFilters.excludeApartments
            preferences[PreferencesKeys.FLEX_AUTO_ACCEPT] = filterPreferences.flexFilters.autoAccept
            preferences[PreferencesKeys.FLEX_AUTO_REJECT] = filterPreferences.flexFilters.autoReject
            preferences[PreferencesKeys.FLEX_START_TIME] = filterPreferences.flexFilters.flexStartTime
            preferences[PreferencesKeys.FLEX_END_TIME] = filterPreferences.flexFilters.flexEndTime
            preferences[PreferencesKeys.FLEX_BLOCK_LENGTHS] = filterPreferences.flexFilters.flexBlockLengths.toSet()
            preferences[PreferencesKeys.FLEX_AUTO_REFRESH] = filterPreferences.flexFilters.autoRefresh

            preferences[PreferencesKeys.SPARK_ENABLED] = filterPreferences.isSparkEnabled
            preferences[PreferencesKeys.DOORDASH_ENABLED] = filterPreferences.isDoorDashEnabled
            preferences[PreferencesKeys.UBER_ENABLED] = filterPreferences.isUberEnabled
            preferences[PreferencesKeys.INSTACART_ENABLED] = filterPreferences.isInstacartEnabled
            preferences[PreferencesKeys.FLEX_ENABLED] = filterPreferences.isFlexEnabled
            preferences[PreferencesKeys.IS_AUTO_ACCEPT_ENABLED] = filterPreferences.isAutoAcceptEnabled
            preferences[PreferencesKeys.IS_DARK_MODE] = filterPreferences.isDarkMode
        }
    }
}
