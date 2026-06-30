package com.example.autograbber.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val dateOfBirth: String = "",
    val homeAddress: String = "",
    val phoneNumber: String = "",
    val registrationDate: Long = System.currentTimeMillis(),
    val approved: Boolean = false,
    val hasLifetimeAccess: Boolean = false
) {
    // Required for Firestore's toObject()
    constructor() : this("", "", "", null, "", "", "", System.currentTimeMillis(), false, false)
}
