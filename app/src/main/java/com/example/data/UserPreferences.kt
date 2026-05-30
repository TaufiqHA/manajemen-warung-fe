package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val PREF_NAMA_WARUNG = stringPreferencesKey("nama_warung")
        val PREF_ALAMAT_WARUNG = stringPreferencesKey("alamat_warung")
        val PREF_EMAIL = stringPreferencesKey("email")
    }

    val namaWarung: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREF_NAMA_WARUNG] ?: "WARUNG KITA"
    }

    val alamatWarung: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREF_ALAMAT_WARUNG] ?: ""
    }

    val email: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREF_EMAIL] ?: "owner@gmail.com"
    }

    suspend fun saveProfile(nama: String, alamat: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[PREF_NAMA_WARUNG] = nama
            preferences[PREF_ALAMAT_WARUNG] = alamat
            preferences[PREF_EMAIL] = email
        }
    }
}
