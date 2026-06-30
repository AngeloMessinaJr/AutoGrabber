package com.example.autograbber.data

import com.example.autograbber.data.db.OfferDao
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.Platform
import kotlinx.coroutines.flow.Flow

class OfferRepository(private val offerDao: OfferDao) {

    fun getOffersByPlatform(platform: Platform): Flow<List<GigOffer>> = 
        offerDao.getOffersByPlatform(platform)

    fun getAllOffers(): Flow<List<GigOffer>> = offerDao.getAllOffers()

    suspend fun addOffer(offer: GigOffer) {
        offerDao.insertOffer(offer)
    }

    suspend fun clearHistory(platform: Platform) {
        offerDao.clearHistory(platform)
    }

    suspend fun clearAllHistory() {
        offerDao.clearAllHistory()
    }
}
