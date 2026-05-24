package com.mireru.app.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mireru.app.model.AnalysisType
import com.mireru.app.ui.viewmodel.CameraViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var showBubble by remember { mutableStateOf(false) }
    var bubbleOffset by remember { mutableStateOf(Offset.Zero) }
    val bubbleText by viewModel.bubbleText.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val fullResult by viewModel.analysisResult.collectAsState()
    var showSubWindow by remember { mutableStateOf(false) }
    var camMode by remember { mutableStateOf("instant") }

    // CameraX セットアップ
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    }}

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カメラモード") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "設定", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // カメラプレビュー
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camMode) {
                        detectTapGestures { offset ->
                            showBubble = false
                            bubbleOffset = offset
                            scope.launch {
                                if (camMode == "instant") {
                                    viewModel.quickCapture(imageCapture, executor, offset)
                                    showBubble = true
                                } else {
                                    viewModel.fullCapture(imageCapture, executor, AnalysisType.CAMERA)
                                    showSubWindow = true
                                }
                            }
                        }
                    }
            )

            // グリッドオーバーレイ
            CameraGridOverlay()

            // AIバッジ
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CamBadge("● LIVE AI", Color(0xFFFF4757))
                CamBadge("AI 解析モード", Color(0xFF00D4AA))
            }

            // ML Kit リアルタイムラベル表示
            if (mlKitLabels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mlKitLabels.take(3).forEach { label ->
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = Color(0xCC1E2230),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4AA).copy(alpha = 0.5f))
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color(0xFF00D4AA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // タップバブル（即時モード）
            if (showBubble) {
                CameraBubble(
                    offset = bubbleOffset,
                    text = bubbleText,
                    isLoading = isAnalyzing,
                    onExpand = {
                        showBubble = false
                        scope.launch {
                            viewModel.fullCapture(imageCapture, executor, AnalysisType.CAMERA)
                            showSubWindow = true
                        }
                    },
                    onDismiss = { showBubble = false }
                )
            }

            // ボトムコントロール
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xDD161920))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👆 文字や物をタップして調べる",
                    color = Color(0xFF4A5568), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("instant" to "即時", "detail" to "詳細").forEach { (mode, label) ->
                        FilterChip(
                            selected = camMode == mode,
                            onClick = { camMode = mode },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 詳細結果ウィンドウ
        if (showSubWindow) {
            SubWindowSheet(
                result = fullResult,
                isLoading = isAnalyzing,
                onDismiss = { showSubWindow = false },
                onSaveHistory = { viewModel.saveToHistory() }
            )
        }
    }
}

@Composable
fun CameraGridOverlay() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val paint = Color.White.copy(alpha = 0.12f)
        for (i in 1..2) {
            drawLine(paint, androidx.compose.ui.geometry.Offset(w * i / 3f, 0f),
                androidx.compose.ui.geometry.Offset(w * i / 3f, h), 1f)
            drawLine(paint, androidx.compose.ui.geometry.Offset(0f, h * i / 3f),
                androidx.compose.ui.geometry.Offset(w, h * i / 3f), 1f)
        }
    }
}

@Composable
fun CamBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = color, fontSize = 11.sp)
    }
}

@Composable
fun CameraBubble(
    offset: Offset,
    text: String,
    isLoading: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.ui.window.Popup(
        offset = with(density) {
            androidx.compose.ui.unit.IntOffset(
                (offset.x + 10.dp.toPx()).toInt(),
                (offset.y - 80.dp.toPx()).toInt()
            )
        },
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E2230),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D4AA)),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(12.dp).widthIn(max = 220.dp)) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp),
                            color = Color(0xFF00D4AA), strokeWidth = 2.dp)
                        Text("解析中...", color = Color(0xFF8892A4), fontSize = 12.sp)
                    }
                } else {
                    Text(text, color = Color(0xFFE8EAF0), fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("詳しく見る →",
                        modifier = Modifier
                            .align(Alignment.End)
                            .pointerInput(Unit) { detectTapGestures { onExpand() } },
                        color = Color(0xFF00D4AA), fontSize = 11.sp)
                }
            }
        }
    }
}
