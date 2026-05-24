package com.mireru.app.ui.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mireru.app.data.GeminiRepository
import com.mireru.app.data.HistoryDao
import com.mireru.app.model.AnalysisResult
import com.mireru.app.model.AnalysisType
import com.mireru.app.model.HistoryItem
import com.mireru.app.util.MlKitHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val mlKitHelper: MlKitHelper,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val gson = Gson()

    val bubbleText      = MutableStateFlow("")
    val isAnalyzing     = MutableStateFlow(false)
    val analysisResult  = MutableStateFlow<AnalysisResult?>(null)
    val mlKitLabels     = MutableStateFlow<List<String>>(emptyList()) // リアルタイム検出ラベル

    /**
     * 即時モード：ML Kit でローカル解析 → Gemini でバブル短文
     */
    fun quickCapture(imageCapture: ImageCapture, executor: Executor) {
        isAnalyzing.value = true
        bubbleText.value  = ""
        captureImage(imageCapture, executor) { bitmap ->
            viewModelScope.launch {
                try {
                    // ① ML Kit でローカル解析（オフライン）
                    val mlResult = mlKitHelper.fullAnalyze(bitmap)
                    val localLabels = mlResult.labels.take(3).map { it.label }
                    mlKitLabels.value = localLabels

                    // ② Gemini でクイック解析（ML Kit コンテキスト付き）
                    val (short, reading) = geminiRepository.quickAnalyze(bitmap, mlResult)
                    val displayText = if (reading.isNotBlank()) "$short（$reading）" else short
                    bubbleText.value = displayText

                    // 履歴保存
                    val label = short.ifBlank { localLabels.firstOrNull() ?: "カメラ" }
                    historyDao.insert(HistoryItem(
                        query        = label,
                        analysisType = AnalysisType.CAMERA.name,
                        source       = "camera",
                        resultJson   = gson.toJson(
                            AnalysisResult(
                                type    = AnalysisType.CAMERA,
                                query   = label,
                                short   = short,
                                reading = reading,
                                tags    = localLabels
                            )
                        )
                    ))
                } catch (e: Exception) {
                    bubbleText.value = "解析エラー: ${e.message?.take(30)}"
                } finally {
                    isAnalyzing.value = false
                }
            }
        }
    }

    /**
     * 詳細モード：ML Kit ＋ Gemini でフル解析
     */
    fun fullCapture(imageCapture: ImageCapture, executor: Executor, type: AnalysisType) {
        isAnalyzing.value  = true
        analysisResult.value = null
        captureImage(imageCapture, executor) { bitmap ->
            viewModelScope.launch {
                try {
                    // ML Kit コンテキスト取得
                    val mlResult = mlKitHelper.fullAnalyze(bitmap)
                    mlKitLabels.value = mlResult.labels.take(5).map { it.label }

                    // Gemini フル解析
                    val result = geminiRepository.analyzeImage(bitmap, "カメラ映像", type, mlResult)
                    analysisResult.value = result
                    saveToHistory()
                } catch (e: Exception) {
                    analysisResult.value = AnalysisResult(
                        type  = type,
                        query = "カメラ",
                        error = e.message ?: "エラーが発生しました"
                    )
                } finally {
                    isAnalyzing.value = false
                }
            }
        }
    }

    fun saveToHistory() {
        val result = analysisResult.value ?: return
        viewModelScope.launch {
            historyDao.insert(HistoryItem(
                query        = result.query,
                analysisType = result.type.name,
                source       = "camera",
                resultJson   = gson.toJson(result)
            ))
        }
    }

    private fun captureImage(
        imageCapture: ImageCapture,
        executor: Executor,
        onCaptured: (Bitmap) -> Unit
    ) {
        imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                image.close()
                onCaptured(bitmap)
            }
            override fun onError(exception: ImageCaptureException) {
                isAnalyzing.value = false
                bubbleText.value = "撮影エラー: ${exception.message}"
            }
        })
    }
}
