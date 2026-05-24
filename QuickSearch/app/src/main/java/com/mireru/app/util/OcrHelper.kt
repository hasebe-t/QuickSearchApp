package com.mireru.app.util

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrHelper @Inject constructor() {

    // 日本語対応エンジン（漢字・ひらがな・カタカナ）
    private val japaneseRecognizer =
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    // ラテン文字エンジン（英語・数字）
    private val latinRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Bitmap全体のOCR（日本語 → ラテン文字フォールバック）
     */
    suspend fun recognize(bitmap: Bitmap): String {
        val jpText = recognizeWith(japaneseRecognizer, bitmap)
        if (jpText.isNotBlank()) return jpText
        return recognizeWith(latinRecognizer, bitmap)
    }

    /**
     * 指定領域のOCR
     */
    suspend fun recognizeRegion(bitmap: Bitmap, regionRect: Rect): String {
        val safe = Rect(
            regionRect.left.coerceIn(0, bitmap.width - 1),
            regionRect.top.coerceIn(0, bitmap.height - 1),
            regionRect.right.coerceIn(1, bitmap.width),
            regionRect.bottom.coerceIn(1, bitmap.height)
        )
        if (safe.width() < 4 || safe.height() < 4) return ""
        val cropped = Bitmap.createBitmap(bitmap, safe.left, safe.top, safe.width(), safe.height())
        return recognize(cropped)
    }

    private suspend fun recognizeWith(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap
    ): String = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result -> cont.resume(result.text.trim()) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
}
