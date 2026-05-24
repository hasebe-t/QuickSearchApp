package com.mireru.app.model

/** AI解析結果の種別 */
enum class AnalysisType(val label: String) {
    KANJI("漢字・語句"),
    EXPLAIN("詳細説明"),
    TRANSLATE("翻訳"),
    WEB("Web検索"),
    CAMERA("カメラAI")
}

/** AI解析の結果を統一的に保持するデータクラス */
data class AnalysisResult(
    val type: AnalysisType,
    val query: String,
    // 漢字
    val word: String? = null,
    val readings: List<Reading> = emptyList(),
    val meanings: List<String> = emptyList(),
    val example: String? = null,
    val origin: String? = null,
    // 説明・翻訳・カメラ共通
    val summary: String? = null,
    val details: String? = null,
    val funFact: String? = null,
    val related: List<String> = emptyList(),
    // 翻訳
    val translated: String? = null,
    val langFrom: String? = null,
    val langTo: String? = null,
    val reading: String? = null,
    // Web
    val overview: String? = null,
    val points: List<String> = emptyList(),
    // Camera/全般
    val short: String? = null,
    val tags: List<String> = emptyList(),
    // メモ
    val note: String? = null,
    // エラー
    val error: String? = null
)

data class Reading(val type: String, val value: String)
