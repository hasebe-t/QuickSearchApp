package com.mireru.app.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mireru.app.data.GeminiRepository
import com.mireru.app.data.HistoryDao
import com.mireru.app.model.AnalysisResult
import com.mireru.app.model.AnalysisType
import com.mireru.app.model.HistoryItem
import com.mireru.app.util.ImageCropper
import com.mireru.app.util.MlKitHelper
import com.mireru.app.util.OcrHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

@HiltViewModel
class MainViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val ocrHelper: OcrHelper,
    private val mlKitHelper: MlKitHelper,
    private val imageCropper: ImageCropper,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val gson = Gson()

    val bitmap          = MutableStateFlow<Bitmap?>(null)
    val interactionMode = MutableStateFlow("select")    // "select" | "pan"
    val analysisResult  = MutableStateFlow<AnalysisResult?>(null)
    val isAnalyzing     = MutableStateFlow(false)
    val mlKitStatus     = MutableStateFlow("")          // ML Kit の中間結果を表示

    fun loadBitmap(bmp: Bitmap) { bitmap.value = bmp }
    fun setInteractionMode(mode: String) { interactionMode.value = mode }

    /**
     * メイン解析フロー
     * 1. 選択範囲を切り抜く
     * 2. ML Kit OCR でテキスト抽出
     * 3. ML Kit 画像ラベリング＋オブジェクト検出
     * 4. Gemini で詳細解析
     */
    suspend fun analyze(
        bitmap: Bitmap,
        imageDisplayRect: RectF,
        selStart: Offset,
        selEnd: Offset,
        type: AnalysisType
    ) {
        isAnalyzing.value = true
        analysisResult.value = null
        mlKitStatus.value = "ML Kit で解析中..."

        try {
            val selectRectF = RectF(
                min(selStart.x, selEnd.x),
                min(selStart.y, selEnd.y),
                min(selStart.x, selEnd.x) + abs(selEnd.x - selStart.x),
                min(selStart.y, selEnd.y) + abs(selEnd.y - selStart.y)
            )

            // ① 選択範囲を切り抜く
            val cropped = imageCropper.crop(bitmap, imageDisplayRect, selectRectF)

            // ② ML Kit OCR（テキスト認識）
            val ocrText = cropped?.let {
                mlKitStatus.value = "テキストを認識中..."
                ocrHelper.recognize(it)
            } ?: ""

            // ③ ML Kit 画像解析（ラベリング＋オブジェクト検出）
            val mlKitResult = cropped?.let {
                mlKitStatus.value = "画像を解析中..."
                mlKitHelper.fullAnalyze(it)
            }

            // ④ Gemini で詳細解析
            mlKitStatus.value = "Gemini で詳細解析中..."
            val query = ocrText.ifBlank {
                // OCR テキストがなければ ML Kit ラベルを使用
                mlKitResult?.labels?.firstOrNull()?.label ?: "選択した領域"
            }

            val result = if (ocrText.isNotBlank()) {
                // テキストあり → テキスト解析（ML Kit コンテキスト付き）
                geminiRepository.analyzeText(ocrText, type, mlKitResult)
            } else if (cropped != null) {
                // テキストなし → 画像解析
                geminiRepository.analyzeImage(cropped, query, type, mlKitResult)
            } else {
                AnalysisResult(type = type, query = "不明", error = "選択範囲が認識できませんでした")
            }

            analysisResult.value = result
        } catch (e: Exception) {
            analysisResult.value = AnalysisResult(
                type = type, query = "エラー",
                error = e.message ?: "不明なエラー"
            )
        } finally {
            isAnalyzing.value = false
            mlKitStatus.value = ""
        }
    }

    fun saveToHistory() {
        val result = analysisResult.value ?: return
        viewModelScope.launch {
            historyDao.insert(HistoryItem(
                query        = result.query,
                analysisType = result.type.name,
                source       = "screenshot",
                resultJson   = gson.toJson(result)
            ))
        }
    }
}
