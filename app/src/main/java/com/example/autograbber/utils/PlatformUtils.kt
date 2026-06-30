package com.example.autograbber.utils

import com.example.autograbber.data.models.Platform

fun getPlatformPackageNames(platform: Platform): List<String> {
    return when (platform) {
        Platform.SPARK -> listOf("com.walmart.sparkdriver")
        Platform.DOORDASH -> listOf("com.doordash.driverapp", "com.dd.dasher", "com.doordash.driver", "com.dasher.driver")
        Platform.UBER -> listOf("com.ubercab.driver")
        Platform.INSTACART -> listOf("com.instacart.shopper")
        Platform.FLEX -> listOf(
            "com.amazon.flex.rabbit",
            "com.amazon.rabbit", 
            "com.amazon.flex", 
            "com.amazon.flex.driver",
            "com.amazon.flex.app",
            "com.amazon.vxt.client",
            "com.amazon.rabbit.amazonflex",
            "com.amazon.rabbit.rabbit",
            "com.amazon.flex.android",
            "com.amazon.flex.rabbit.android",
            "com.amazon.flex.driver.android"
        )
    }
}
