package com.mireru.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mireru.app.model.AnalysisType
import com.mireru.app.model.HistoryItem
import com.mireru.app.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState(initial = emptyList())
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }
    var showSubWindow by remember { mutableStateOf(false) }
    val analysisResult by viewModel.selectedResult.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("調査履歴 ${historyItems.size}件") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, "すべて削除", tint = Color(0xFFFF4757))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("調査履歴はありません", color = Color(0xFF4A5568))
                    Text("スクリーンショットやカメラで\n調べてみましょう",
                        color = Color(0xFF2A3045), fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onClick = {
                            viewModel.selectItem(item)
                            showSubWindow = true
                        },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }

        // 詳細表示
        if (showSubWindow && analysisResult != null) {
            SubWindowSheet(
                result = analysisResult,
                isLoading = false,
                onDismiss = { showSubWindow = false },
                onSaveHistory = {}
            )
        }

        // 削除確認ダイアログ
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("履歴を削除") },
                text = { Text("すべての調査履歴を削除しますか？") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }) { Text("削除", color = Color(0xFFFF4757)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("キャンセル") }
                }
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val typeEnum = runCatching { AnalysisType.valueOf(item.analysisType) }.getOrNull()
    val icon = when (typeEnum) {
        AnalysisType.KANJI     -> "漢"
        AnalysisType.EXPLAIN   -> "💡"
        AnalysisType.TRANSLATE -> "🌐"
        AnalysisType.WEB       -> "🔍"
        AnalysisType.CAMERA    -> "📷"
        null -> "📋"
    }
    val dateStr = SimpleDateFormat("M/d HH:mm", Locale.JAPAN)
        .format(Date(item.timestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161920),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3045))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E2230)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.query, color = Color(0xFFE8EAF0), fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    typeEnum?.let {
                        Surface(shape = RoundedCornerShape(10.dp),
                            color = typeColor(it).copy(alpha = 0.2f)) {
                            Text(it.label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = typeColor(it), fontSize = 10.sp)
                        }
                    }
                    Text(dateStr, color = Color(0xFF4A5568), fontSize = 10.sp)
                    Text(if (item.source == "camera") "📷" else "📸", fontSize = 10.sp)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, "削除", tint = Color(0xFF2A3045), modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun typeColor(type: AnalysisType) = when (type) {
    AnalysisType.KANJI     -> Color(0xFF7C8CF8)
    AnalysisType.EXPLAIN   -> Color(0xFF00D4AA)
    AnalysisType.TRANSLATE -> Color(0xFFFF6B35)
    AnalysisType.WEB       -> Color(0xFF8892A4)
    AnalysisType.CAMERA    -> Color(0xFFFFD700)
}
