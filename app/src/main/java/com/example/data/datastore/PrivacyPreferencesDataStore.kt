package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_preferences")

enum class VisibilityOption(val code: String, val titleFr: String, val titleEn: String, val descFr: String, val descEn: String) {
    EVERYONE(
        code = "EVERYONE",
        titleFr = "Tout le monde",
        titleEn = "Everyone",
        descFr = "Visible par tous les utilisateurs du réseau",
        descEn = "Visible to all network users"
    ),
    CONTACTS(
        code = "CONTACTS",
        titleFr = "Mes contacts uniquement",
        titleEn = "My contacts only",
        descFr = "Visible uniquement par les numéros enregistrés dans votre répertoire",
        descEn = "Only visible to phone numbers saved in your address book"
    ),
    NOBODY(
        code = "NOBODY",
        titleFr = "Personne",
        titleEn = "Nobody",
        descFr = "Masqué pour tous les utilisateurs",
        descEn = "Hidden from all users"
    );

    companion object {
        fun fromCode(code: String?): VisibilityOption {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EVERYONE
        }
    }
}

/**
 * Data class representing the privacy preferences persisted in Android Jetpack DataStore.
 */
data class PrivacyPreferences(
    val readReceipts: Boolean = true,
    val profilePhotoVisibility: String = "EVERYONE", // EVERYONE, CONTACTS, NOBODY
    val lastSeenVisibility: String = "EVERYONE",      // EVERYONE, CONTACTS, NOBODY
    val onlineStatusVisibility: String = "EVERYONE",   // EVERYONE, SAME_AS_LAST_SEEN
    val groupsAddPermission: String = "EVERYONE",      // EVERYONE, CONTACTS, NOBODY
    val defaultMessageTimer: String = "OFF",          // OFF, 24H, 7D, 90D
    val isBiometricEnabled: Boolean = false,
    val biometricTimeout: String = "IMMEDIATELY",      // IMMEDIATELY, 1_MIN, 5_MIN, 15_MIN, 1_HOUR
    val biometricHideNotifications: Boolean = true
)

/**
 * Repository / Manager for Privacy Preferences using Jetpack DataStore.
 */
class PrivacyPreferencesDataStore(private val context: Context) {

    private object PreferencesKeys {
        val READ_RECEIPTS = booleanPreferencesKey("privacy_read_receipts")
        val PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("privacy_profile_photo_visibility")
        val LAST_SEEN_VISIBILITY = stringPreferencesKey("privacy_last_seen_visibility")
        val ONLINE_STATUS_VISIBILITY = stringPreferencesKey("privacy_online_status_visibility")
        val GROUPS_ADD_PERMISSION = stringPreferencesKey("privacy_groups_add_permission")
        val DEFAULT_MESSAGE_TIMER = stringPreferencesKey("privacy_default_message_timer")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("privacy_biometric_enabled")
        val BIOMETRIC_TIMEOUT = stringPreferencesKey("privacy_biometric_timeout")
        val BIOMETRIC_HIDE_NOTIFS = booleanPreferencesKey("privacy_biometric_hide_notifs")
    }

    val privacyPreferencesFlow: Flow<PrivacyPreferences> = context.privacyDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PrivacyPreferences(
                readReceipts = preferences[PreferencesKeys.READ_RECEIPTS] ?: true,
                profilePhotoVisibility = preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] ?: "EVERYONE",
                lastSeenVisibility = preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] ?: "EVERYONE",
                onlineStatusVisibility = preferences[PreferencesKeys.ONLINE_STATUS_VISIBILITY] ?: "EVERYONE",
                groupsAddPermission = preferences[PreferencesKeys.GROUPS_ADD_PERMISSION] ?: "EVERYONE",
                defaultMessageTimer = preferences[PreferencesKeys.DEFAULT_MESSAGE_TIMER] ?: "OFF",
                isBiometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
                biometricTimeout = preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] ?: "IMMEDIATELY",
                biometricHideNotifications = preferences[PreferencesKeys.BIOMETRIC_HIDE_NOTIFS] ?: true
            )
        }

    suspend fun setReadReceipts(enabled: Boolean) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.READ_RECEIPTS] = enabled
        }
    }

    suspend fun setProfilePhotoVisibility(visibility: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = visibility
        }
    }

    suspend fun setLastSeenVisibility(visibility: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VISIBILITY] = visibility
        }
    }

    suspend fun setOnlineStatusVisibility(visibility: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.ONLINE_STATUS_VISIBILITY] = visibility
        }
    }

    suspend fun setGroupsAddPermission(permission: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.GROUPS_ADD_PERMISSION] = permission
        }
    }

    suspend fun setDefaultMessageTimer(timer: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_MESSAGE_TIMER] = timer
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setBiometricTimeout(timeout: String) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_TIMEOUT] = timeout
        }
    }

    suspend fun setBiometricHideNotifications(hide: Boolean) {
        context.privacyDataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_HIDE_NOTIFS] = hide
        }
    }
}
