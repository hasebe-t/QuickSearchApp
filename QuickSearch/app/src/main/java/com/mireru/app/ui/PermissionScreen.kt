package com.mireru.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/** 必要な権限をリストアップ */
fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}.toTypedArray()

/** 権限がすべて許可されているか確認 */
fun hasAllPermissions(context: android.content.Context): Boolean =
    requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun PermissionRequestScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf(hasAllPermissions(context))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) onGranted()
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) onGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "みれる",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF00D4AA)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI調査アプリ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8892A4)
        )
        Spacer(Modifier.height(40.dp))

        Text(
            "アプリの使用には以下の権限が必要です",
            color = Color(0xFF8892A4),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        // 権限説明カード
        listOf(
            Triple(Icons.Default.CameraAlt,  "カメラ",     "リアルタイムAI解析に使用"),
            Triple(Icons.Default.Photo,       "写真・画像", "スクリーンショット読み込みに使用")
        ).forEach { (icon, title, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, null, tint = Color(0xFF00D4AA))
                    Column {
                        Text(title, color = Color(0xFFE8EAF0), fontSize = 14.sp)
                        Text(desc,  color = Color(0xFF8892A4), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { launcher.launch(requiredPermissions()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA))
        ) {
            Text("権限を許可してはじめる", color = Color(0xFF0D0F14), fontSize = 15.sp)
        }
    }
}
