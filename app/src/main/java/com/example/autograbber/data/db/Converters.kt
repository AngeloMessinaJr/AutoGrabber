package com.example.autograbber.data.db

import androidx.room.TypeConverter
import com.example.autograbber.data.models.OfferAction
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.StoreDetail
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromPlatform(platform: Platform): String = platform.name

    @TypeConverter
    fun toPlatform(name: String): Platform = Platform.valueOf(name)

    @TypeConverter
    fun fromOfferAction(action: OfferAction): String = action.name

    @TypeConverter
    fun toOfferAction(name: String): OfferAction = OfferAction.valueOf(name)

    @TypeConverter
    fun fromStoreDetailList(value: List<StoreDetail>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toStoreDetailList(value: String?): List<StoreDetail>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
