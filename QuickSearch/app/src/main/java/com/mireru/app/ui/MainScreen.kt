package com.mireru.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mireru.app.model.AnalysisType
import com.mireru.app.ui.theme.MireruTheme
import com.mireru.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/** メイン画面 - スクリーンショットモード */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    sharedImageUri: Uri? = null,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bitmap by viewModel.bitmap.collectAsState()
    val interactionMode by viewModel.interactionMode.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val isAnalyzing  by viewModel.isAnalyzing.collectAsState()
    val mlKitStatus  by viewModel.mlKitStatus.collectAsState()

    // 選択矩形の状態（画面座標）
    var selStart by remember { mutableStateOf(Offset.Zero) }
    var selEnd   by remember { mutableStateOf(Offset.Zero) }
    var isSelecting by remember { mutableStateOf(false) }
    var selectionDone by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var showSubWindow by remember { mutableStateOf(false) }

    // ズーム・パン状態
    var scale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // 画像表示領域（Canvas上）
    var imageDisplayRect by remember { mutableStateOf(RectF()) }

    // 他アプリからのSHARE
    LaunchedEffect(sharedImageUri) {
        sharedImageUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                bmp?.let { viewModel.loadBitmap(it) }
            }
        }
    }

    // 画像ピッカー
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                bmp?.let { b ->
                    viewModel.loadBitmap(b)
                    scale = 1f; panOffset = Offset.Zero
                    selectionDone = false; showContextMenu = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("みれる", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { navController.navigate("camera") }) {
                        Icon(Icons.Default.CameraAlt, "カメラ")
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, "履歴")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "設定")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ツールバー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 画像読み込みボタン
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("画像を選択", fontSize = 13.sp)
                }

                Spacer(Modifier.weight(1f))

                // モード切替：選択 / 移動
                ModeToggleButton(
                    currentMode = interactionMode,
                    onModeChange = { viewModel.setInteractionMode(it) }
                )

                // ズームボタン（常に表示）
                ZoomButtons(
                    onZoomIn  = { scale = (scale * 1.5f).coerceAtMost(8f) },
                    onZoomOut = { scale = (scale / 1.5f).coerceAtLeast(0.5f) },
                    onReset   = { scale = 1f; panOffset = Offset.Zero }
                )
            }

            // キャンバスエリア
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D0F14))
            ) {
                if (bitmap == null) {
                    // ヒント表示
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📸", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "画像を選択してください",
                            color = Color(0xFF4A5568),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { imagePicker.launch("image/*") }) {
                            Text("画像を選択")
                        }
                    }
                } else {
                    // 画像 + 選択オーバーレイ
                    ImageCanvasWithSelection(
                        bitmap = bitmap!!,
                        scale = scale,
                        panOffset = panOffset,
                        interactionMode = interactionMode,
                        selStart = selStart,
                        selEnd = selEnd,
                        isSelecting = isSelecting,
                        selectionDone = selectionDone,
                        onImageRectChanged = { imageDisplayRect = it },
                        onScaleChange = { newScale -> scale = newScale.coerceIn(0.5f, 8f) },
                        onPanChange = { delta -> panOffset += delta },
                        onSelectionStart = { offset ->
                            selStart = offset; selEnd = offset
                            isSelecting = true; selectionDone = false
                            showContextMenu = false
                        },
                        onSelectionMove = { offset -> selEnd = offset },
                        onSelectionEnd = { offset ->
                            selEnd = offset
                            isSelecting = false
                            val w = kotlin.math.abs(selEnd.x - selStart.x)
                            val h = kotlin.math.abs(selEnd.y - selStart.y)
                            if (w > 10f && h > 10f) {
                                selectionDone = true
                                contextMenuOffset = offset
                                showContextMenu = true
                            }
                        }
                    )
                }

                // ズームインジケーター
                if (scale != 1f) {
                    Text(
                        "× ${"%.1f".format(scale)}",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(
                                Color(0xCC161920),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color(0xFF00D4AA),
                        fontSize = 12.sp
                    )
                }

                // コンテキストメニュー
                if (showContextMenu) {
                    ContextMenuOverlay(
                        offset = contextMenuOffset,
                        onDismiss = { showContextMenu = false },
                        onAction = { type ->
                            showContextMenu = false
                            scope.launch {
                                viewModel.analyze(
                                    bitmap = bitmap!!,
                                    imageDisplayRect = imageDisplayRect,
                                    selStart = selStart,
                                    selEnd = selEnd,
                                    type = type
                                )
                                showSubWindow = true
                            }
                        }
                    )
                }
            }

            // 結果サブウィンドウ
            if (showSubWindow) {
                SubWindowSheet(
                    result = analysisResult,
                    isLoading = isAnalyzing,
                    onDismiss = { showSubWindow = false },
                    onSaveHistory = { viewModel.saveToHistory() }
                )
            }
        }
    }
}

// ===== 子コンポーネント =====

@Composable
fun ModeToggleButton(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 選択ボタン（破線四角アイコン）
        FilterChip(
            selected = currentMode == "select",
            onClick = { onModeChange("select") },
            label = { Text("選択", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        )
        // 移動ボタン（手アイコン）
        FilterChip(
            selected = currentMode == "pan",
            onClick = { onModeChange("pan") },
            label = { Text("移動", fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        )
    }
}

@Composable
fun ZoomButtons(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallFloatingActionButton(
            onClick = onZoomIn,
            modifier = Modifier.size(34.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) { Text("＋", fontSize = 14.sp) }
        SmallFloatingActionButton(
            onClick = onReset,
            modifier = Modifier.size(34.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) { Text("1:1", fontSize = 9.sp) }
        SmallFloatingActionButton(
            onClick = onZoomOut,
            modifier = Modifier.size(34.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) { Text("－", fontSize = 14.sp) }
    }
}

/**
 * 画像表示 + タッチインタラクション（選択・パン・ピンチ）
 */
@Composable
fun ImageCanvasWithSelection(
    bitmap: Bitmap,
    scale: Float,
    panOffset: Offset,
    interactionMode: String,
    selStart: Offset,
    selEnd: Offset,
    isSelecting: Boolean,
    selectionDone: Boolean,
    onImageRectChanged: (RectF) -> Unit,
    onScaleChange: (Float) -> Unit,
    onPanChange: (Offset) -> Unit,
    onSelectionStart: (Offset) -> Unit,
    onSelectionMove: (Offset) -> Unit,
    onSelectionEnd: (Offset) -> Unit
) {
    val imageBitmap = bitmap.asImageBitmap()
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // ピンチ＋パン
    val transformModifier = Modifier.pointerInput(interactionMode) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            // 2本指操作（ピンチ＋中点パン）は常に有効
            onScaleChange(scale * zoom)
            onPanChange(pan)
        }
    }

    // 1本指タッチ（モード分岐）
    val touchModifier = Modifier.pointerInput(interactionMode) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val pointers = event.changes.filter { it.pressed }

                when {
                    pointers.size >= 2 -> {
                        // 2本指：何もしない（detectTransformGesturesに任せる）
                        event.changes.forEach { it.consume() }
                    }
                    pointers.size == 1 -> {
                        val ptr = pointers[0]
                        when (event.type) {
                            PointerEventType.Press -> {
                                if (interactionMode == "select") {
                                    onSelectionStart(ptr.position)
                                }
                                ptr.consume()
                            }
                            PointerEventType.Move -> {
                                if (interactionMode == "select") {
                                    onSelectionMove(ptr.position)
                                } else {
                                    // 移動モード：1本指パン
                                    onPanChange(ptr.position - ptr.previousPosition)
                                }
                                ptr.consume()
                            }
                            PointerEventType.Release -> {
                                if (interactionMode == "select") {
                                    onSelectionEnd(ptr.position)
                                }
                                ptr.consume()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(transformModifier)
            .then(touchModifier)
    ) {
        canvasSize = size
        val cw = size.width
        val ch = size.height
        val iw = bitmap.width.toFloat()
        val ih = bitmap.height.toFloat()

        // フィットスケール計算
        val fitScale = minOf(cw / iw, ch / ih) * 0.95f
        val dw = iw * fitScale * scale
        val dh = ih * fitScale * scale
        val dx = (cw - dw) / 2f + panOffset.x
        val dy = (ch - dh) / 2f + panOffset.y

        // 画像表示領域を外部に通知
        onImageRectChanged(RectF(dx, dy, dx + dw, dy + dh))

        // 画像描画
        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(dx.toInt(), dy.toInt()),
            dstSize = IntSize(dw.toInt(), dh.toInt())
        )

        // 画像枠のグロー
        drawRect(
            color = Color(0x3300D4AA),
            topLeft = Offset(dx, dy),
            size = Size(dw, dh),
            style = Stroke(1.5f)
        )

        // 選択矩形を描画
        if (isSelecting || selectionDone) {
            val sx = minOf(selStart.x, selEnd.x)
            val sy = minOf(selStart.y, selEnd.y)
            val sw = kotlin.math.abs(selEnd.x - selStart.x)
            val sh = kotlin.math.abs(selEnd.y - selStart.y)

            // 半透明塗り
            drawRect(
                color = Color(0x2200D4AA),
                topLeft = Offset(sx, sy),
                size = Size(sw, sh)
            )
            // 枠線
            drawRect(
                color = Color(0xFF00D4AA),
                topLeft = Offset(sx, sy),
                size = Size(sw, sh),
                style = Stroke(
                    width = 2f,
                    pathEffect = if (isSelecting) null else PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
            )
            // 四隅のコーナーマーカー
            val cornerSize = 8f
            val corners = listOf(
                Offset(sx, sy), Offset(sx + sw - cornerSize, sy),
                Offset(sx, sy + sh - cornerSize), Offset(sx + sw - cornerSize, sy + sh - cornerSize)
            )
            corners.forEach { c ->
                drawRect(Color(0xFF00D4AA), topLeft = c, size = Size(cornerSize, cornerSize))
            }
        }
    }
}
