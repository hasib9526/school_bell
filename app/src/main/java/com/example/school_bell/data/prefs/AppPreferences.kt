package com.example.school_bell.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "school_bell_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_SCHOOL_NAME = stringPreferencesKey("school_name")
        private val KEY_KIOSK_ENABLED = booleanPreferencesKey("kiosk_enabled")
        private val KEY_AZAN_ENABLED = booleanPreferencesKey("azan_enabled")
        private val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val KEY_SOUND_VERSION = intPreferencesKey("sound_version")
        private val KEY_CALC_METHOD = stringPreferencesKey("calc_method")
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
        private val KEY_SYNC_INTERVAL_HOURS = intPreferencesKey("sync_interval_hours")
        private val KEY_USERNAME = stringPreferencesKey("username")

        const val DEFAULT_SERVER_URL = "http://192.168.1.100:8080"
        const val DEFAULT_SCHOOL_NAME = "Smart School"
        const val DEFAULT_SYNC_INTERVAL = 6
    }

    val authToken: Flow<String?> = context.dataStore.data
        .catchIO()
        .map { it[KEY_AUTH_TOKEN] }

    val deviceId: Flow<String> = context.dataStore.data
        .catchIO()
        .map { prefs ->
            prefs[KEY_DEVICE_ID] ?: run {
                val newId = UUID.randomUUID().toString()
                // Will be saved on next write; return generated ID
                newId
            }
        }

    val serverUrl: Flow<String> = context.dataStore.data
        .catchIO()
        .map { it[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL }

    val schoolName: Flow<String> = context.dataStore.data
        .catchIO()
        .map { it[KEY_SCHOOL_NAME] ?: DEFAULT_SCHOOL_NAME }

    val isKioskEnabled: Flow<Boolean> = context.dataStore.data
        .catchIO()
        .map { it[KEY_KIOSK_ENABLED] ?: false }

    val azanEnabled: Flow<Boolean> = context.dataStore.data
        .catchIO()
        .map { it[KEY_AZAN_ENABLED] ?: true }

    val lastSyncTime: Flow<Long> = context.dataStore.data
        .catchIO()
        .map { it[KEY_LAST_SYNC_TIME] ?: 0L }

    val soundVersion: Flow<Int> = context.dataStore.data
        .catchIO()
        .map { it[KEY_SOUND_VERSION] ?: 0 }

    val calcMethod: Flow<String> = context.dataStore.data
        .catchIO()
        .map { it[KEY_CALC_METHOD] ?: "MUSLIM_WORLD_LEAGUE" }

    val latitude: Flow<Double> = context.dataStore.data
        .catchIO()
        .map { it[KEY_LATITUDE] ?: 0.0 }

    val longitude: Flow<Double> = context.dataStore.data
        .catchIO()
        .map { it[KEY_LONGITUDE] ?: 0.0 }

    val syncIntervalHours: Flow<Int> = context.dataStore.data
        .catchIO()
        .map { it[KEY_SYNC_INTERVAL_HOURS] ?: DEFAULT_SYNC_INTERVAL }

    val username: Flow<String> = context.dataStore.data
        .catchIO()
        .map { it[KEY_USERNAME] ?: "" }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { it.remove(KEY_AUTH_TOKEN) }
    }

    suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    suspend fun saveSchoolName(name: String) {
        context.dataStore.edit { it[KEY_SCHOOL_NAME] = name }
    }

    suspend fun saveKioskEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KIOSK_ENABLED] = enabled }
    }

    suspend fun saveAzanEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AZAN_ENABLED] = enabled }
    }

    suspend fun saveLastSyncTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC_TIME] = time }
    }

    suspend fun saveSoundVersion(version: Int) {
        context.dataStore.edit { it[KEY_SOUND_VERSION] = version }
    }

    suspend fun saveCalcMethod(method: String) {
        context.dataStore.edit { it[KEY_CALC_METHOD] = method }
    }

    suspend fun saveLocation(lat: Double, lon: Double) {
        context.dataStore.edit {
            it[KEY_LATITUDE] = lat
            it[KEY_LONGITUDE] = lon
        }
    }

    suspend fun saveSyncIntervalHours(hours: Int) {
        context.dataStore.edit { it[KEY_SYNC_INTERVAL_HOURS] = hours }
    }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit { it[KEY_USERNAME] = name }
    }

    suspend fun ensureDeviceId(): String {
        var id: String? = null
        context.dataStore.edit { prefs ->
            id = prefs[KEY_DEVICE_ID]
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs[KEY_DEVICE_ID] = id!!
            }
        }
        return id!!
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun Flow<Preferences>.catchIO(): Flow<Preferences> = catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }
}
