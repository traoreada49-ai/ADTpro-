package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Centralized thread-safe initializer for Firebase services across the application.
 */
object FirebaseInitHelper {
    private const val TAG = "FirebaseInitHelper"
    @Volatile
    private var isCloudStorageActive: Boolean? = null

    @Synchronized
    fun ensureInitialized(context: Context): FirebaseApp {
        val appContext = context.applicationContext ?: context
        return if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            FirebaseApp.getInstance()
        } else {
            FirebaseApp.initializeApp(appContext)
                ?: throw IllegalStateException("FirebaseApp could not be initialized from google-services.json")
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        return try {
            ensureInitialized(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FirebaseAuth: ${e.message}")
            null
        }
    }

    fun hasAuthenticatedUser(context: Context): Boolean {
        return try {
            val auth = getAuth(context)
            auth?.currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun ensureAuthenticated(context: Context): Boolean {
        return hasAuthenticatedUser(context)
    }

    fun isCloudStorageAvailable(): Boolean = isCloudStorageActive ?: true

    fun markCloudStorageUnavailable() {
        isCloudStorageActive = false
        Log.w(TAG, "Cloud storage bucket marked as unavailable; falling back to high-res local encrypted vault.")
    }

    fun getStorage(context: Context): FirebaseStorage? {
        if (isCloudStorageActive == false) {
            return null
        }
        return try {
            ensureInitialized(context)
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Error obtaining FirebaseStorage: ${e.message}")
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            ensureInitialized(context)
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining FirebaseFirestore: ${e.message}")
            null
        }
    }
}
