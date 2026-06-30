package com.example.autograbber.ui.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import com.example.autograbber.data.models.Platform
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey, Parcelable {
    @Serializable
    @Parcelize
    data object Dashboard : Destination
    
    @Serializable
    @Parcelize
    data object Settings : Destination

    @Serializable
    @Parcelize
    data object Notifications : Destination

    @Serializable
    @Parcelize
    data object Account : Destination

    @Serializable
    @Parcelize
    data object Subscription : Destination

    @Serializable
    @Parcelize
    data object Troubleshooting : Destination

    @Serializable
    @Parcelize
    data object Chat : Destination

    @Serializable
    @Parcelize
    data class PlatformFilters(val platform: Platform) : Destination

    @Serializable
    @Parcelize
    data class OfferHistory(val platform: Platform? = null) : Destination
}
