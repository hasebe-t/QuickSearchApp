package com.mireru.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mireru.app.data.HistoryDao
import com.mireru.app.model.AnalysisResult
import com.mireru.app.model.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : ViewModel() {

    private val gson = Gson()

    val historyItems = historyDao.getAll()
    val selectedResult = MutableStateFlow<AnalysisResult?>(null)

    fun selectItem(item: HistoryItem) {
        selectedResult.value = runCatching {
            gson.fromJson(item.resultJson, AnalysisResult::class.java)
        }.getOrNull()
    }

    fun deleteItem(item: HistoryItem) = viewModelScope.launch {
        historyDao.delete(item)
    }

    fun clearAll() = viewModelScope.launch {
        historyDao.deleteAll()
    }
}
