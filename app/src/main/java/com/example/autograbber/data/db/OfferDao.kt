package com.example.autograbber.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.Platform
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {
    @Query("SELECT * FROM gig_offers WHERE platform = :platform ORDER BY timestamp DESC")
    fun getOffersByPlatform(platform: Platform): Flow<List<GigOffer>>

    @Query("SELECT * FROM gig_offers ORDER BY timestamp DESC")
    fun getAllOffers(): Flow<List<GigOffer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: GigOffer)

    @Query("SELECT * FROM gig_offers WHERE timestamp = :timestamp AND platform = :platform LIMIT 1")
    suspend fun getOfferByTimestamp(timestamp: Long, platform: Platform): GigOffer?

    @Query("DELETE FROM gig_offers WHERE platform = :platform")
    suspend fun clearHistory(platform: Platform)

    @Query("DELETE FROM gig_offers")
    suspend fun clearAllHistory()
}
