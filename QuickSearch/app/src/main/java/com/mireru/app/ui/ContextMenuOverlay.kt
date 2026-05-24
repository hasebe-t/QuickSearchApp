package com.mireru.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mireru.app.model.AnalysisType

data class MenuItem(
    val type: AnalysisType,
    val icon: String,
    val label: String,
    val sub: String
)

val menuItems = listOf(
    MenuItem(AnalysisType.KANJI,    "漢", "漢字・語句を調べる",  "読み方・意味・用例・成り立ち"),
    MenuItem(AnalysisType.EXPLAIN,  "💡", "詳しく説明する",       "概要・背景・豆知識"),
    MenuItem(AnalysisType.TRANSLATE,"🌐", "翻訳する",             "日英・英日・多言語対応"),
    MenuItem(AnalysisType.WEB,      "🔍", "ウェブで検索",         "最新情報・ニュース"),
)

@Composable
fun ContextMenuOverlay(
    offset: Offset,
    onDismiss: () -> Unit,
    onAction: (AnalysisType) -> Unit
) {
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // メニュー表示位置の調整
    val menuWidth = 230.dp
    val xDp = with(density) { offset.x.toDp() }
    val yDp = with(density) { offset.y.toDp() }
    val adjustedX = (xDp - menuWidth / 2).coerceIn(8.dp, screenWidth - menuWidth - 8.dp)

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            with(density) { adjustedX.roundToPx() },
            with(density) { (yDp - 20.dp).roundToPx() }
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            modifier = Modifier.width(menuWidth),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column {
                // ヘッダー
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF252A3A))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "調べる方法を選択",
                        color = Color(0xFF8892A4),
                        fontSize = 11.sp,
                        letterSpacing = 0.1.em
                    )
                }
                HorizontalDivider(color = Color(0xFF2A3045))

                // メニュー項目
                menuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(item.type) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = when (item.type) {
                                        AnalysisType.KANJI     -> Color(0x337C8CF8)
                                        AnalysisType.EXPLAIN   -> Color(0x2200D4AA)
                                        AnalysisType.TRANSLATE -> Color(0x33FF6B35)
                                        AnalysisType.WEB       -> Color(0x22FFFFFF)
                                        else -> Color(0x22FFFFFF)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.icon, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.label, color = Color(0xFFE8EAF0), fontSize = 14.sp)
                            Text(item.sub,   color = Color(0xFF4A5568), fontSize = 11.sp)
                        }
                    }
                    if (item != menuItems.last()) {
                        HorizontalDivider(color = Color(0x332A3045))
                    }
                }
            }
        }
    }
}
