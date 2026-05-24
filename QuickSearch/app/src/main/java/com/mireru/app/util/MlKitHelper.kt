package com.mireru.app.util

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** ML Kit の検出結果 */
data class LabelResult(val label: String, val confidence: Float)
data class DetectedObject(val labels: List<String>, val boundingBox: RectF, val trackingId: Int?)

@Singleton
class MlKitHelper @Inject constructor() {

    // 画像ラベリング（信頼度 0.65 以上のみ）
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.65f).build()
    )

    // オブジェクト検出（複数オブジェクト + 分類有効）
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )

    /**
     * 画像ラベリング：写っているものを列挙
     */
    suspend fun labelImage(bitmap: Bitmap): List<LabelResult> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    cont.resume(labels.map { LabelResult(it.text, it.confidence) })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * オブジェクト検出：物体の位置と種別を返す
     */
    suspend fun detectObjects(bitmap: Bitmap): List<DetectedObject> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            objectDetector.process(image)
                .addOnSuccessListener { objects ->
                    cont.resume(objects.map { obj ->
                        DetectedObject(
                            labels = obj.labels.map { it.text },
                            boundingBox = RectF(obj.boundingBox),
                            trackingId = obj.trackingId
                        )
                    })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /**
     * 統合解析：OCRテキスト + ラベル + オブジェクトをまとめて返す
     */
    suspend fun fullAnalyze(bitmap: Bitmap): MlKitResult {
        val labels = runCatching { labelImage(bitmap) }.getOrDefault(emptyList())
        val objects = runCatching { detectObjects(bitmap) }.getOrDefault(emptyList())
        return MlKitResult(labels = labels, objects = objects)
    }
}

data class MlKitResult(
    val labels: List<LabelResult>,
    val objects: List<DetectedObject>
) {
    /** Gemini に渡す文字列サマリー */
    fun toPromptContext(): String = buildString {
        if (labels.isNotEmpty()) {
            append("【ML Kit 画像ラベル】\n")
            labels.take(5).forEach { append("・${it.label}（${(it.confidence * 100).toInt()}%）\n") }
        }
        if (objects.isNotEmpty()) {
            append("【ML Kit 検出オブジェクト】\n")
            objects.forEach { obj ->
                if (obj.labels.isNotEmpty()) append("・${obj.labels.joinToString(", ")}\n")
            }
        }
    }
}
