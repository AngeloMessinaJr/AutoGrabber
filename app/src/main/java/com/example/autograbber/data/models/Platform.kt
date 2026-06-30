package com.example.autograbber.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Platform : Parcelable {
    SPARK,
    DOORDASH,
    UBER,
    INSTACART,
    FLEX
}
