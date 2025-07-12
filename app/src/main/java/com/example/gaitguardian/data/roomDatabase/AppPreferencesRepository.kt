package com.example.gaitguardian.data.roomDatabase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferencesRepository(private val dataStore: DataStore<Preferences>){
    private val CURRENT_USER_VIEW = stringPreferencesKey("current_user")
    private val PREVIOUS_DURATION = intPreferencesKey("previous_duration")
    private val LATEST_DURATION = intPreferencesKey("latest_duration")
    private val SAVE_VIDEOS_KEY = booleanPreferencesKey("save_videos")
    private val FIRST_PRIVACY_CHECK = booleanPreferencesKey("first_privacy_check")

    suspend fun setSaveVideos(shouldSave: Boolean) {
        dataStore.edit { prefs ->
            prefs[SAVE_VIDEOS_KEY] = shouldSave
        }
    }

    fun getSaveVideos(): Flow<Boolean> {
        return dataStore.data.map { prefs -> prefs[SAVE_VIDEOS_KEY] ?: false }
    }

    // Check if it's their first video, if yes, ask if they want to save video !
    suspend fun setFirstPrivacyCheck(shouldSave: Boolean) {
        dataStore.edit { prefs ->
            prefs[FIRST_PRIVACY_CHECK] = shouldSave
        }
    }

    fun getFirstPrivacyCheck(): Flow<Boolean> {
        return dataStore.data.map { prefs -> prefs[FIRST_PRIVACY_CHECK] ?: false }
    }

    // Save both previous and latest duration
    suspend fun updateDurations(newDuration: Int) {
        dataStore.edit { prefs ->
            val previous = prefs[LATEST_DURATION] ?: 0
            prefs[PREVIOUS_DURATION] = previous
            prefs[LATEST_DURATION] = newDuration
        }
    }

    fun getPreviousDuration(): Flow<Int> {
        return dataStore.data.map { it[PREVIOUS_DURATION] ?: 0 }
    }

    fun getLatestDuration(): Flow<Int> {
        return dataStore.data.map { it[LATEST_DURATION] ?: 0 }
    }

    // Save Current User into DataStore
    suspend fun saveCurrentUserView(currentUser: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_VIEW] = currentUser
        }
    }

    // Retrieve current view from DataStore
    fun getCurrentUserView(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[CURRENT_USER_VIEW] // returns null if not set
        }
    }
}