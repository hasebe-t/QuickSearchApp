package com.mireru.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mireru.app.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val geminiApiKey = settingsDataStore.geminiApiKey.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val googleAccount = settingsDataStore.googleAccount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val analysisLanguage = settingsDataStore.analysisLanguage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "auto"
    )

    fun saveGeminiApiKey(key: String) = viewModelScope.launch {
        settingsDataStore.saveGeminiApiKey(key)
    }

    fun saveGoogleAccount(email: String) = viewModelScope.launch {
        settingsDataStore.saveGoogleAccount(email)
    }

    fun saveLanguage(lang: String) = viewModelScope.launch {
        settingsDataStore.saveAnalysisLanguage(lang)
    }

    fun clearAll() = viewModelScope.launch {
        settingsDataStore.clearAll()
    }
}
