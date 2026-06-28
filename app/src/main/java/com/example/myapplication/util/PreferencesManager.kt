package com.example.myapplication.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<androidx.datastore.preferences.core.Preferences>
        by preferencesDataStore(name = "app_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }

    val themeMode: Flow<String> = context.settingsDataStore.data.map { it[THEME_MODE] ?: "system" }
    val language: Flow<String> = context.settingsDataStore.data.map { it[LANGUAGE] ?: "en" }
    val monthlyBudget: Flow<Double> = context.settingsDataStore.data.map { it[MONTHLY_BUDGET] ?: 0.0 }
    val biometricLock: Flow<Boolean> = context.settingsDataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setLanguage(lang: String) {
        context.settingsDataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun setMonthlyBudget(amount: Double) {
        context.settingsDataStore.edit { it[MONTHLY_BUDGET] = amount }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.settingsDataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }
}
