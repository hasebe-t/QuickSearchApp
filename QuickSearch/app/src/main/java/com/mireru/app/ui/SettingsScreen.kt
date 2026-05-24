package com.mireru.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mireru.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val geminiApiKey   by viewModel.geminiApiKey.collectAsState()
    val googleAccount  by viewModel.googleAccount.collectAsState()
    val analysisLang   by viewModel.analysisLanguage.collectAsState()

    // 編集中の値（保存前）
    var editApiKey     by remember(geminiApiKey)  { mutableStateOf(geminiApiKey) }
    var editEmail      by remember(googleAccount) { mutableStateOf(googleAccount) }
    var apiKeyVisible  by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var savedSnackbar  by remember { mutableStateOf(false) }

    LaunchedEffect(savedSnackbar) {
        if (savedSnackbar) {
            kotlinx.coroutines.delay(2000)
            savedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161920)
                )
            )
        },
        snackbarHost = {
            if (savedSnackbar) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00D4AA)
                    ) {
                        Text(
                            "✓ 保存しました",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            color = Color(0xFF0D0F14),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ===== Googleアカウント セクション =====
            SettingsSection(
                icon = Icons.Default.AccountCircle,
                iconTint = Color(0xFF4285F4),
                title = "Googleアカウント",
                subtitle = "Gemini API の利用に必要です"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Gemini APIキーは Google AI Studio で取得できます。\n" +
                        "取得したAPIキーを下の「Gemini APIキー」欄に貼り付けてください。",
                        color = Color(0xFF8892A4),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    // Google AI Studio へのリンク説明
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A2340),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔗", fontSize = 18.sp)
                            Column {
                                Text(
                                    "APIキー取得先",
                                    color = Color(0xFF4285F4),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "aistudio.google.com/app/apikey",
                                    color = Color(0xFF8892A4),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Googleアカウントメール入力
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Googleアカウント（メールアドレス）") },
                        placeholder = { Text("your@gmail.com", color = Color(0xFF4A5568)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = Color(0xFF8892A4))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.saveGoogleAccount(editEmail)
                            savedSnackbar = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("アカウントを保存")
                    }

                    // 保存済み表示
                    if (googleAccount.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D2010)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF00D4AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "登録済み: $googleAccount",
                                    color = Color(0xFF00D4AA),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ===== Gemini APIキー セクション =====
            SettingsSection(
                icon = Icons.Default.Key,
                iconTint = Color(0xFF00D4AA),
                title = "Gemini APIキー",
                subtitle = "AI解析に使用します（必須）"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // APIキー入力フィールド
                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        label = { Text("Gemini APIキー") },
                        placeholder = { Text("AIzaSy...", color = Color(0xFF4A5568)) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, null, tint = Color(0xFF8892A4))
                        },
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    if (apiKeyVisible) "隠す" else "表示",
                                    tint = Color(0xFF8892A4)
                                )
                            }
                        },
                        visualTransformation = if (apiKeyVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsTextFieldColors(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // 保存ボタン
                    Button(
                        onClick = {
                            viewModel.saveGeminiApiKey(editApiKey)
                            savedSnackbar = true
                        },
                        enabled = editApiKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D4AA),
                            disabledContainerColor = Color(0xFF1E2230)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "APIキーを保存",
                            color = if (editApiKey.isNotBlank()) Color(0xFF0D0F14) else Color(0xFF4A5568)
                        )
                    }

                    // 設定状態表示
                    if (geminiApiKey.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D2010)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF00D4AA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "APIキー設定済み（${geminiApiKey.take(8)}...）",
                                    color = Color(0xFF00D4AA),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A1010)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = Color(0xFFFF6B35),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "APIキー未設定：AI解析が使用できません",
                                    color = Color(0xFFFF6B35),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ===== 解析言語 セクション =====
            SettingsSection(
                icon = Icons.Default.Language,
                iconTint = Color(0xFF7C8CF8),
                title = "解析言語",
                subtitle = "OCRとAI解析の言語設定"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("auto", "自動判定", "日本語・英語を自動で判定"),
                        Triple("ja",   "日本語",   "漢字・ひらがな・カタカナ優先"),
                        Triple("en",   "英語",     "ラテン文字・英語優先")
                    ).forEach { (value, label, desc) ->
                        LangOptionRow(
                            label = label,
                            description = desc,
                            selected = analysisLang == value,
                            onClick = { viewModel.saveLanguage(value) }
                        )
                    }
                }
            }

            // ===== ML Kit 説明 セクション =====
            SettingsSection(
                icon = Icons.Default.Psychology,
                iconTint = Color(0xFFFFD700),
                title = "AI解析エンジン",
                subtitle = "使用中の技術"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("🔤", "テキスト認識（ML Kit）", "日本語・英語の文字を認識"),
                        Triple("🏷️", "画像ラベリング（ML Kit）", "写真から物体・シーンを特定"),
                        Triple("📦", "オブジェクト検出（ML Kit）", "物体を検出して追跡"),
                        Triple("🤖", "詳細解析（Gemini 1.5 Flash）", "AI による詳細な情報取得")
                    ).forEach { (icon, title, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(icon, fontSize = 18.sp)
                            Column {
                                Text(title, color = Color(0xFFE8EAF0), fontSize = 13.sp)
                                Text(desc,  color = Color(0xFF8892A4), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ===== 危険ゾーン =====
            SettingsSection(
                icon = Icons.Default.DeleteForever,
                iconTint = Color(0xFFFF4757),
                title = "データ管理",
                subtitle = "設定・APIキーをリセット"
            ) {
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4757)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4757).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("すべての設定をリセット")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // リセット確認ダイアログ
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF1E2230),
            title = {
                Text("設定をリセット", color = Color(0xFFE8EAF0))
            },
            text = {
                Text(
                    "APIキー・Googleアカウント・言語設定がすべて削除されます。",
                    color = Color(0xFF8892A4)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    editApiKey = ""; editEmail = ""
                    showClearDialog = false
                }) {
                    Text("リセット", color = Color(0xFFFF4757))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("キャンセル", color = Color(0xFF8892A4))
                }
            }
        )
    }
}

// ===== 共通コンポーネント =====

@Composable
private fun SettingsSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF161920),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3045))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.15f)
                ) {
                    Icon(
                        icon, null,
                        tint = iconTint,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Column {
                    Text(title, color = Color(0xFFE8EAF0), fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    Text(subtitle, color = Color(0xFF8892A4), fontSize = 11.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun LangOptionRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF7C8CF8) else Color(0xFF2A3045)
    val bgColor     = if (selected) Color(0x1A7C8CF8) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF7C8CF8),
                    unselectedColor = Color(0xFF4A5568)
                )
            )
            Column {
                Text(label, color = Color(0xFFE8EAF0), fontSize = 14.sp)
                Text(description, color = Color(0xFF8892A4), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF00D4AA),
    unfocusedBorderColor = Color(0xFF2A3045),
    focusedLabelColor    = Color(0xFF00D4AA),
    unfocusedLabelColor  = Color(0xFF8892A4),
    cursorColor          = Color(0xFF00D4AA),
    focusedTextColor     = Color(0xFFE8EAF0),
    unfocusedTextColor   = Color(0xFFE8EAF0),
    focusedContainerColor   = Color(0xFF1E2230),
    unfocusedContainerColor = Color(0xFF1E2230)
)
