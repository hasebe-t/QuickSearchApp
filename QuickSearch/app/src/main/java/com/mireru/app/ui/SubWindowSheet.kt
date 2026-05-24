package com.mireru.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.mireru.app.model.AnalysisResult
import com.mireru.app.model.AnalysisType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubWindowSheet(
    result: AnalysisResult?,
    isLoading: Boolean,
    loadingText: String = "AIが解析中...",
    onDismiss: () -> Unit,
    onSaveHistory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161920),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            // ヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                result?.let { r ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (r.type) {
                            AnalysisType.KANJI     -> Color(0x337C8CF8)
                            AnalysisType.EXPLAIN   -> Color(0x2200D4AA)
                            AnalysisType.TRANSLATE -> Color(0x33FF6B35)
                            AnalysisType.WEB       -> Color(0x22FFFFFF)
                            AnalysisType.CAMERA    -> Color(0x33FFD700)
                        }
                    ) {
                        Text(
                            r.type.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            color = when (r.type) {
                                AnalysisType.KANJI     -> Color(0xFF7C8CF8)
                                AnalysisType.EXPLAIN   -> Color(0xFF00D4AA)
                                AnalysisType.TRANSLATE -> Color(0xFFFF6B35)
                                AnalysisType.WEB       -> Color(0xFF8892A4)
                                AnalysisType.CAMERA    -> Color(0xFFFFD700)
                            },
                            fontSize = 11.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        result.query,
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF8892A4),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onSaveHistory, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Bookmark, "保存", tint = Color(0xFF8892A4))
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "閉じる", tint = Color(0xFF8892A4))
                }
            }
            HorizontalDivider(color = Color(0xFF2A3045))

            // コンテンツ
            if (isLoading || result == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00D4AA), strokeWidth = 2.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(loadingText, color = Color(0xFF4A5568), fontSize = 13.sp)
                    }
                }
            } else if (result.error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚠️ ${result.error}", color = Color(0xFFFF4757), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { ResultContent(result) }
                }
            }
        }
    }
}

@Composable
fun ResultContent(result: AnalysisResult) {
    when (result.type) {
        AnalysisType.KANJI -> KanjiResult(result)
        AnalysisType.EXPLAIN -> ExplainResult(result)
        AnalysisType.TRANSLATE -> TranslateResult(result)
        AnalysisType.WEB -> WebResult(result)
        AnalysisType.CAMERA -> CameraResult(result)
    }
}

@Composable
fun KanjiResult(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 大きな漢字表示
        Text(
            result.word ?: result.query,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 72.sp,
            fontFamily = FontFamily.Serif,
            color = Color(0xFFE8EAF0),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // 読み方
        ResultSection("読み方") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                result.readings.forEach { reading ->
                    ReadingChip(reading.type, reading.value)
                }
            }
        }
        // 意味
        ResultSection("意味") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                result.meanings.forEach { meaning ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x2200D4AA)
                    ) {
                        Text(
                            "・$meaning",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(0xFF00D4AA), fontSize = 13.sp
                        )
                    }
                }
            }
        }
        result.example?.let { ResultSection("用例") { ResultTextBox(it) } }
        result.origin?.let { ResultSection("成り立ち・語源") { ResultTextBox(it) } }
        result.note?.let { FactBox(it) }
    }
}

@Composable
fun ExplainResult(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        result.summary?.let { ResultSection("概要") { ResultTextBox(it, highlight = true) } }
        result.details?.let { ResultSection("詳しく") { ResultTextBox(it) } }
        result.funFact?.let { ResultSection("豆知識") { FactBox(it) } }
        if (result.related.isNotEmpty()) {
            ResultSection("関連キーワード") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.related.forEach { tag ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF252A3A)) {
                            Text(tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color(0xFF8892A4), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranslateResult(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E2230)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${result.langFrom} → ${result.langTo}",
                    color = Color(0xFF4A5568), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(result.query, color = Color(0xFF8892A4), fontSize = 14.sp)
                Text("↓", color = Color(0xFF4A5568), fontSize = 14.sp, modifier = Modifier.padding(4.dp))
                Text(result.translated ?: "", color = Color(0xFFE8EAF0),
                    fontSize = 24.sp, fontFamily = FontFamily.Serif)
                result.reading?.let {
                    if (it.isNotBlank()) Text(it, color = Color(0xFF00D4AA), fontSize = 12.sp)
                }
            }
        }
        result.note?.let { ResultSection("補足・ニュアンス") { ResultTextBox(it) } }
    }
}

@Composable
fun WebResult(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        result.overview?.let { ResultSection("概要") { ResultTextBox(it, highlight = true) } }
        if (result.points.isNotEmpty()) {
            ResultSection("ポイント") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.points.forEach { point ->
                        ResultTextBox("・$point")
                    }
                }
            }
        }
        result.note?.let { FactBox(it) }
    }
}

@Composable
fun CameraResult(result: AnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        result.short?.let { ResultSection("識別結果") { ResultTextBox(it, highlight = true) } }
        result.reading?.let { if (it.isNotBlank()) ResultSection("読み方") { ResultTextBox(it) } }
        result.details?.let { ResultSection("詳細") { ResultTextBox(it) } }
        if (result.tags.isNotEmpty()) {
            ResultSection("タグ") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF252A3A)) {
                            Text(tag, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color(0xFF8892A4), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ===== 共通パーツ =====

@Composable
fun ResultSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label.uppercase(), color = Color(0xFF4A5568), fontSize = 10.sp, letterSpacing = 0.12.em)
        content()
    }
}

@Composable
fun ResultTextBox(text: String, highlight: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) Color(0x0D00D4AA) else Color(0xFF1E2230),
        border = if (highlight) androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400D4AA)) else null
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            color = Color(0xFFE8EAF0),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ReadingChip(type: String, value: String) {
    val (bg, fg) = when {
        type.contains("音") -> Color(0x337C8CF8) to Color(0xFF7C8CF8)
        type.contains("訓") -> Color(0x2200D4AA) to Color(0xFF00D4AA)
        else               -> Color(0x33FF6B35) to Color(0xFFFF6B35)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text("$type：$value", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = fg, fontSize = 13.sp)
    }
}

@Composable
fun FactBox(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x0D00D4AA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300D4AA))
    ) {
        Text(
            "💡 $text",
            modifier = Modifier.padding(12.dp),
            color = Color(0xFFE8EAF0),
            fontSize = 13.sp,
            lineHeight = 21.sp
        )
    }
}
