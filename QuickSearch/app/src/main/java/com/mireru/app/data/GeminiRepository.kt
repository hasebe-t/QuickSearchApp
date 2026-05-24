package com.mireru.app.data

import android.graphics.Bitmap
import android.util.Base64
import com.mireru.app.model.AnalysisResult
import com.mireru.app.model.AnalysisType
import com.mireru.app.model.Reading
import com.mireru.app.util.MlKitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    // Gemini 1.5 Flash エンドポイント
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ========== PUBLIC API ==========

    /** テキストのみで解析（OCRテキスト → Gemini） */
    suspend fun analyzeText(
        text: String,
        type: AnalysisType,
        mlKitContext: MlKitResult? = null
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = settingsDataStore.geminiApiKey.first()
        if (apiKey.isBlank()) return@withContext noKeyError(type, text)

        val prompt = buildTextPrompt(text, type, mlKitContext)
        val body = buildTextRequestBody(prompt)
        val raw = callGemini(apiKey, body)
        parseResponse(text, type, raw)
    }

    /** 画像＋ML Kitコンテキストで解析（Gemini Vision） */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        label: String,
        type: AnalysisType,
        mlKitContext: MlKitResult? = null
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = settingsDataStore.geminiApiKey.first()
        if (apiKey.isBlank()) return@withContext noKeyError(type, label)

        val base64 = bitmapToBase64(bitmap)
        val prompt = buildImagePrompt(label, type, mlKitContext)
        val body = buildImageRequestBody(base64, prompt)
        val raw = callGemini(apiKey, body)
        parseResponse(label, type, raw)
    }

    /** カメラ映像クイック解析（短文） */
    suspend fun quickAnalyze(
        bitmap: Bitmap,
        mlKitContext: MlKitResult? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiKey = settingsDataStore.geminiApiKey.first()
        if (apiKey.isBlank()) return@withContext Pair("APIキー未設定", "")

        val base64 = bitmapToBase64(bitmap)
        val contextHint = mlKitContext?.toPromptContext() ?: ""
        val prompt = """
            この画像に写っているメインの対象を特定し、一行（30文字以内）で説明してください。
            読み方がある場合は読み方も返してください。
            ${if (contextHint.isNotBlank()) "参考情報:\n$contextHint" else ""}
            JSON形式のみで返答（説明・バッククォート不要）:
            {"short":"説明","reading":"読み方（なければ空）"}
        """.trimIndent()

        val body = buildImageRequestBody(base64, prompt)
        val raw = runCatching { callGemini(apiKey, body) }.getOrDefault("{}")
        return@withContext try {
            val j = JSONObject(raw.cleanJson())
            Pair(j.optString("short", ""), j.optString("reading", ""))
        } catch (e: Exception) {
            Pair(raw.take(40), "")
        }
    }

    // ========== PROMPTS ==========

    private fun buildTextPrompt(text: String, type: AnalysisType, mlKit: MlKitResult?): String {
        val ctx = mlKit?.toPromptContext()?.let { "\n参考情報:\n$it" } ?: ""
        return when (type) {
            AnalysisType.KANJI -> """
                あなたは日本語の辞書・教育AIです。「$text」について以下のJSON形式のみで返答（説明・バッククォート不要）:
                {"type":"kanji","word":"$text","readings":[{"type":"音読み","value":""},{"type":"訓読み","value":""}],"meanings":["意味1","意味2"],"example":"用例文","origin":"成り立ち・語源","note":"補足情報"}$ctx
            """.trimIndent()
            AnalysisType.EXPLAIN -> """
                「$text」について詳しく説明してください。JSON形式のみで返答:
                {"type":"explain","title":"$text","summary":"2-3文の要約","details":"詳しい説明4-5文","fun_fact":"面白い豆知識","related":["関連1","関連2","関連3"]}$ctx
            """.trimIndent()
            AnalysisType.TRANSLATE -> """
                「$text」を翻訳してください。JSON形式のみで返答:
                {"type":"translate","original":"$text","translated":"翻訳結果","lang_from":"元言語","lang_to":"翻訳先言語","reading":"発音・読み方","note":"翻訳の補足"}$ctx
            """.trimIndent()
            AnalysisType.WEB -> """
                「$text」について最新の情報を含む概要をまとめてください。JSON形式のみで返答:
                {"type":"web","query":"$text","overview":"概要3-4文","points":["ポイント1","ポイント2","ポイント3"],"note":"補足"}$ctx
            """.trimIndent()
            AnalysisType.CAMERA -> """
                「$text」について簡潔に説明してください。JSON形式のみで返答:
                {"type":"camera","subject":"$text","short":"一行の説明","reading":"読み方","detail":"詳細2-3文","tags":["タグ1","タグ2"]}$ctx
            """.trimIndent()
        }
    }

    private fun buildImagePrompt(label: String, type: AnalysisType, mlKit: MlKitResult?): String {
        val ctx = mlKit?.toPromptContext()?.let { "\nML Kit解析結果:\n$it" } ?: ""
        return when (type) {
            AnalysisType.KANJI -> """
                画像内のテキストや文字「$label」について説明してください。JSON形式のみで返答:
                {"type":"kanji","word":"$label","readings":[{"type":"読み","value":""}],"meanings":["意味"],"example":"用例","origin":"語源","note":"補足"}$ctx
            """.trimIndent()
            AnalysisType.EXPLAIN -> """
                この画像に写っているもの「$label」について詳しく説明してください。JSON形式のみで返答:
                {"type":"explain","title":"$label","summary":"要約","details":"詳しい説明","fun_fact":"豆知識","related":["関連1","関連2"]}$ctx
            """.trimIndent()
            AnalysisType.TRANSLATE -> """
                画像内の文字やテキストを翻訳してください。JSON形式のみで返答:
                {"type":"translate","original":"$label","translated":"翻訳","lang_from":"元言語","lang_to":"日本語","reading":"読み方","note":"補足"}$ctx
            """.trimIndent()
            AnalysisType.WEB -> """
                この画像に写っているもの「$label」の概要をまとめてください。JSON形式のみで返答:
                {"type":"web","query":"$label","overview":"概要","points":["ポイント1","ポイント2"],"note":"補足"}$ctx
            """.trimIndent()
            AnalysisType.CAMERA -> """
                この画像に写っているものを特定し説明してください。JSON形式のみで返答:
                {"type":"camera","subject":"$label","short":"一行説明","reading":"読み方","detail":"詳細説明","tags":["タグ1","タグ2"]}$ctx
            """.trimIndent()
        }
    }

    // ========== HTTP ==========

    private fun buildTextRequestBody(prompt: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 1024)
            })
        }.toString()
    }

    private fun buildImageRequestBody(base64Image: String, prompt: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 1024)
            })
        }.toString()
    }

    private fun callGemini(apiKey: String, bodyJson: String): String {
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("レスポンスが空です")
        if (!response.isSuccessful) {
            val errMsg = runCatching {
                JSONObject(responseBody).getJSONObject("error").getString("message")
            }.getOrDefault("APIエラー ${response.code}")
            throw Exception(errMsg)
        }

        // Gemini レスポンスから text を抽出
        val json = JSONObject(responseBody)
        return json
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }

    // ========== RESPONSE PARSER ==========

    private fun parseResponse(query: String, type: AnalysisType, raw: String): AnalysisResult {
        return try {
            val j = JSONObject(raw.cleanJson())
            when (type) {
                AnalysisType.KANJI -> AnalysisResult(
                    type = type, query = query,
                    word = j.optString("word", query),
                    readings = j.optJSONArray("readings")?.let { arr ->
                        (0 until arr.length()).map { i ->
                            val r = arr.getJSONObject(i)
                            Reading(r.optString("type"), r.optString("value"))
                        }
                    } ?: emptyList(),
                    meanings = j.optJSONArray("meanings")?.toStringList() ?: emptyList(),
                    example = j.optString("example").ifBlank { null },
                    origin  = j.optString("origin").ifBlank { null },
                    note    = j.optString("note").ifBlank { null }
                )
                AnalysisType.EXPLAIN -> AnalysisResult(
                    type = type, query = query,
                    summary = j.optString("summary").ifBlank { null },
                    details = j.optString("details").ifBlank { null },
                    funFact = j.optString("fun_fact").ifBlank { null },
                    related = j.optJSONArray("related")?.toStringList() ?: emptyList()
                )
                AnalysisType.TRANSLATE -> AnalysisResult(
                    type = type, query = query,
                    translated = j.optString("translated").ifBlank { null },
                    langFrom   = j.optString("lang_from").ifBlank { null },
                    langTo     = j.optString("lang_to").ifBlank { null },
                    reading    = j.optString("reading").ifBlank { null },
                    note       = j.optString("note").ifBlank { null }
                )
                AnalysisType.WEB -> AnalysisResult(
                    type = type, query = query,
                    overview = j.optString("overview").ifBlank { null },
                    points   = j.optJSONArray("points")?.toStringList() ?: emptyList(),
                    note     = j.optString("note").ifBlank { null }
                )
                AnalysisType.CAMERA -> AnalysisResult(
                    type = type, query = query,
                    short   = j.optString("short").ifBlank { null },
                    reading = j.optString("reading").ifBlank { null },
                    details = j.optString("detail").ifBlank { null },
                    tags    = j.optJSONArray("tags")?.toStringList() ?: emptyList()
                )
            }
        } catch (e: Exception) {
            AnalysisResult(type = type, query = query, error = "解析エラー: ${e.message}")
        }
    }

    // ========== HELPERS ==========

    private fun noKeyError(type: AnalysisType, query: String) = AnalysisResult(
        type = type, query = query,
        error = "Gemini APIキーが設定されていません。\n設定画面からAPIキーを登録してください。"
    )

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        val max = 800
        val scaled = if (bitmap.width > max || bitmap.height > max) {
            val ratio = max.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun String.cleanJson() =
        replace(Regex("```json\\s*"), "").replace(Regex("```\\s*"), "").trim()

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
