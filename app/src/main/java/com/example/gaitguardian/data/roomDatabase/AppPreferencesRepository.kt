package com.example.gaitguardian.data.roomDatabase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferencesRepository(private val dataStore: DataStore<Preferences>){
    private val CURRENT_USER_VIEW = stringPreferencesKey("current_user")

    // Save Current User into DataStore
    suspend fun saveCurrentUserView(currentUser: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_VIEW] = currentUser
        }
    }

    // Retrieve current view from DataStore
    fun getCurrentUserView(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[CURRENT_USER_VIEW] ?: ""
        }
    }
}