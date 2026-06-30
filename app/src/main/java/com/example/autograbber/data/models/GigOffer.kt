package com.example.autograbber.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

enum class OfferAction {
    ACCEPTED, REJECTED, IGNORED, HIDDEN
}

@Serializable
@Parcelize
data class StoreDetail(
    val name: String = "",
    val address: String? = null
) : Parcelable

@Serializable
@Parcelize
@Entity(tableName = "gig_offers")
data class GigOffer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: Platform = Platform.INSTACART,
    val pay: Double = 0.0,
    val distance: Double = 0.0,
    val payPerMile: Double = 0.0,
    val stops: Int = 1,
    val storeName: String = "",
    val storeDetails: List<StoreDetail>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val action: OfferAction = OfferAction.IGNORED,
    val reason: String? = null,
    val itemCount: String? = null
) : Parcelable
