package com.example.slowdown.ui.overlay

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slowdown.SlowDownApp
import com.example.slowdown.service.OverlayService
import com.example.slowdown.ui.theme.SlowDownTheme
import com.example.slowdown.util.NotificationHelper
import com.example.slowdown.util.PackageUtils
import com.example.slowdown.viewmodel.OverlayViewModel

class OverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SlowDown"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        const val EXTRA_REDIRECT_PACKAGE = "redirect_package"
    }

    private val viewModel: OverlayViewModel by viewModels {
        OverlayViewModel.Factory((application as SlowDownApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[Overlay] onCreate called")

        // 取消通知（因为 Full-Screen Intent 已经启动了这个 Activity）
        NotificationHelper.cancelNotification(this)

        // 停止 OverlayService（因为 Activity 已经显示）
        stopService(android.content.Intent(this, OverlayService::class.java))

        // 设置窗口属性 - 关键：让 Activity 显示在锁屏上方并点亮屏幕
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            Log.e(TAG, "[Overlay] No packageName in intent, finishing")
            finish()
            return
        }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val countdownSeconds = intent.getIntExtra(EXTRA_COUNTDOWN_SECONDS, 10)
        val redirectPackage = intent.getStringExtra(EXTRA_REDIRECT_PACKAGE)

        Log.d(TAG, "[Overlay] packageName=$packageName, appName=$appName, countdown=$countdownSeconds")

        viewModel.startCountdown(packageName, appName, countdownSeconds)

        setContent {
            SlowDownTheme {
                OverlayScreen(
                    viewModel = viewModel,
                    appName = appName,
                    redirectPackage = redirectPackage,
                    onContinue = {
                        viewModel.recordAndFinish("continued")
                        // 启动目标应用，让用户继续使用
                        PackageUtils.launchApp(this, packageName)
                        finish()
                    },
                    onRedirect = { redirectPkg ->
                        viewModel.recordAndFinish("redirected")
                        PackageUtils.launchApp(this, redirectPkg)
                        finish()
                    },
                    onCancel = {
                        viewModel.recordAndFinish("cancelled")
                        PackageUtils.goHome(this)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        viewModel.recordAndFinish("cancelled")
        PackageUtils.goHome(this)
        super.onBackPressed()
    }
}

@Composable
private fun OverlayScreen(
    viewModel: OverlayViewModel,
    appName: String,
    redirectPackage: String?,
    onContinue: () -> Unit,
    onRedirect: (String) -> Unit,
    onCancel: () -> Unit
) {
    val countdown by viewModel.countdown.collectAsState()
    val canContinue by viewModel.canContinue.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "你正在打开",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appName,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = if (canContinue) "可以继续了" else countdown.toString(),
                color = if (canContinue) Color.Green else Color.White,
                fontSize = if (canContinue) 24.sp else 72.sp,
                textAlign = TextAlign.Center
            )

            if (!canContinue) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请等待倒计时结束",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Redirect button (if configured)
            if (redirectPackage != null) {
                Button(
                    onClick = { onRedirect(redirectPackage) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("打开微信读书", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Continue button
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) MaterialTheme.colorScheme.primary else Color.Gray,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (canContinue) "继续访问" else "等待中...",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cancel link
            TextButton(onClick = onCancel) {
                Text(text = "返回桌面", color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
