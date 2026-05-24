package com.mireru.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mireru.app.model.AnalysisType

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val analysisType: String,          // AnalysisType.name
    val source: String,                // "screenshot" | "camera"
    val resultJson: String,            // JSON文字列
    val timestamp: Long = System.currentTimeMillis()
)
