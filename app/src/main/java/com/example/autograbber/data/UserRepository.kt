package com.example.autograbber.data

import com.example.autograbber.data.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.net.Uri

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        if (profile.id.isEmpty()) return false
        return try {
            // Use update to avoid overwriting existing fields like isApproved 
            // if we are only saving partial profile data, but since we pass the whole object:
            usersCollection.document(profile.id).set(profile).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun observeUserProfile(uid: String, onUpdate: (UserProfile?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        android.util.Log.d("UserRepository", "Starting observation for UID: $uid")
        return usersCollection.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("UserRepository", "Error observing profile", error)
                onUpdate(null)
                return@addSnapshotListener
            }
            
            val profile = snapshot?.toObject(UserProfile::class.java)
            android.util.Log.d("UserRepository", "Snapshot received. Approved from DB: ${snapshot?.get("approved")}, Mapped Approved: ${profile?.approved}")
            onUpdate(profile)
        }
    }

    suspend fun uploadProfilePicture(uid: String, imageUri: Uri): String? {
        return try {
            val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            android.util.Log.d("UserRepository", "Uploading for UID: $uid, Auth UID: $authUid")
            
            // Re-adding the .jpg extension as it worked on the emulator and is more standard
            val ref = storage.reference.child("profile_pictures/$uid.jpg")
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            
            android.util.Log.d("UserRepository", "Upload successful: $url")
            url
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Upload failed for UID $uid", e)
            if (e is com.google.firebase.storage.StorageException) {
                android.util.Log.e("UserRepository", "Storage Error Code: ${e.errorCode}")
            }
            null
        }
    }
}
