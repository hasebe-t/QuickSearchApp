package com.mireru.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mireru.app.ui.*
import com.mireru.app.ui.theme.MireruTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 他アプリからのSHARE受け取り
        @Suppress("DEPRECATION")
        val sharedImageUri: Uri? = if (intent?.action == Intent.ACTION_SEND)
            intent.getParcelableExtra(Intent.EXTRA_STREAM) else null

        setContent {
            MireruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var permissionsGranted by remember { mutableStateOf(hasAllPermissions(context)) }

                    if (!permissionsGranted) {
                        PermissionRequestScreen(onGranted = { permissionsGranted = true })
                    } else {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") {
                                MainScreen(
                                    navController    = navController,
                                    sharedImageUri   = sharedImageUri
                                )
                            }
                            composable("camera") {
                                CameraScreen(navController = navController)
                            }
                            composable("history") {
                                HistoryScreen(navController = navController)
                            }
                            composable("settings") {
                                SettingsScreen(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
