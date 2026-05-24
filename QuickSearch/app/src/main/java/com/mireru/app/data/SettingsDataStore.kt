package com.mireru.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mireru_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val GEMINI_API_KEY   = stringPreferencesKey("gemini_api_key")
        val GOOGLE_ACCOUNT   = stringPreferencesKey("google_account_email")
        val ANALYSIS_LANG    = stringPreferencesKey("analysis_language") // "ja" | "en" | "auto"
    }

    // Gemini APIキー
    val geminiApiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_API_KEY] ?: "" }

    // Googleアカウントメール
    val googleAccount: Flow<String> = context.dataStore.data.map { it[GOOGLE_ACCOUNT] ?: "" }

    // 解析言語設定
    val analysisLanguage: Flow<String> = context.dataStore.data.map { it[ANALYSIS_LANG] ?: "auto" }

    suspend fun saveGeminiApiKey(key: String) {
        context.dataStore.edit { it[GEMINI_API_KEY] = key.trim() }
    }

    suspend fun saveGoogleAccount(email: String) {
        context.dataStore.edit { it[GOOGLE_ACCOUNT] = email.trim() }
    }

    suspend fun saveAnalysisLanguage(lang: String) {
        context.dataStore.edit { it[ANALYSIS_LANG] = lang }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
