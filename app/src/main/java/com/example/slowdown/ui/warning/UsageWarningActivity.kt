package com.example.slowdown.ui.warning

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.slowdown.service.UsageWarningType
import com.example.slowdown.ui.theme.SlowDownTheme
import com.example.slowdown.util.PackageUtils

/**
 * 使用时间警告 Activity
 * 当用户使用时间达到 80% 或 100% 限额时显示的全屏警告界面
 */
class UsageWarningActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SlowDown"
        const val EXTRA_WARNING_TYPE = "warning_type"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_USED_MINUTES = "used_minutes"
        const val EXTRA_LIMIT_MINUTES = "limit_minutes"
        const val EXTRA_REDIRECT_PACKAGE = "redirect_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[UsageWarning] onCreate called")

        // 设置窗口属性 - 让 Activity 显示在锁屏上方并点亮屏幕
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // 获取 Intent 参数
        val warningTypeStr = intent.getStringExtra(EXTRA_WARNING_TYPE) ?: run {
            Log.e(TAG, "[UsageWarning] No warningType in intent, finishing")
            finish()
            return
        }
        val warningType = try {
            UsageWarningType.valueOf(warningTypeStr)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "[UsageWarning] Invalid warningType: $warningTypeStr, finishing")
            finish()
            return
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run {
            Log.e(TAG, "[UsageWarning] No packageName in intent, finishing")
            finish()
            return
        }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val usedMinutes = intent.getIntExtra(EXTRA_USED_MINUTES, 0)
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)
        val redirectPackage = intent.getStringExtra(EXTRA_REDIRECT_PACKAGE)

        Log.d(TAG, "[UsageWarning] warningType=$warningType, packageName=$packageName, " +
                "appName=$appName, used=$usedMinutes, limit=$limitMinutes")

        setContent {
            SlowDownTheme {
                UsageWarningScreen(
                    warningType = warningType,
                    appName = appName,
                    usedMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    redirectPackage = redirectPackage,
                    onContinue = {
                        Log.d(TAG, "[UsageWarning] User chose to continue using $packageName")
                        // 启动目标应用，让用户继续使用
                        PackageUtils.launchApp(this, packageName)
                        finish()
                    },
                    onExit = {
                        Log.d(TAG, "[UsageWarning] User chose to exit $packageName")
                        PackageUtils.goHome(this)
                        finish()
                    },
                    onRedirect = { redirectPkg ->
                        Log.d(TAG, "[UsageWarning] User chose to redirect to $redirectPkg")
                        PackageUtils.launchApp(this, redirectPkg)
                        finish()
                    },
                    onGoHome = {
                        Log.d(TAG, "[UsageWarning] User chose to go home")
                        PackageUtils.goHome(this)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 对于强制模式，返回键应该返回桌面，而不是返回目标应用
        val warningTypeStr = intent.getStringExtra(EXTRA_WARNING_TYPE)
        if (warningTypeStr == UsageWarningType.LIMIT_REACHED_STRICT.name) {
            PackageUtils.goHome(this)
        }
        super.onBackPressed()
    }
}

/**
 * 使用时间警告界面
 */
@Composable
private fun UsageWarningScreen(
    warningType: UsageWarningType,
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    redirectPackage: String?,
    onContinue: () -> Unit,
    onExit: () -> Unit,
    onRedirect: (String) -> Unit,
    onGoHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        when (warningType) {
            UsageWarningType.SOFT_REMINDER -> {
                // SOFT_REMINDER 不应该到这里，它应该显示深呼吸弹窗（OverlayActivity）
                // 但为了代码完整性，我们显示一个简单的提醒
                UsageWarning80PercentContent(
                    appName = appName,
                    usedMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    onContinue = onContinue,
                    onExit = onExit
                )
            }
            UsageWarningType.LIMIT_REACHED_SOFT -> {
                LimitReachedSoftContent(
                    appName = appName,
                    usedMinutes = usedMinutes,
                    redirectPackage = redirectPackage,
                    onRedirect = onRedirect,
                    onContinue = onContinue,
                    onExit = onExit
                )
            }
            UsageWarningType.LIMIT_REACHED_STRICT -> {
                LimitReachedStrictContent(
                    appName = appName,
                    usedMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    redirectPackage = redirectPackage,
                    onRedirect = onRedirect,
                    onGoHome = onGoHome
                )
            }
        }
    }
}

/**
 * 80% 使用时间提醒内容
 */
@Composable
private fun UsageWarning80PercentContent(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    val remainingMinutes = limitMinutes - usedMinutes

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // Icon
        Text(
            text = "\u23F0", // Alarm clock emoji
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "使用时间提醒",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "今日已使用 $usedMinutes 分钟",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "还剩 $remainingMinutes 分钟",
                    color = Color(0xFFFFD54F), // Amber color
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Exit button (recommended action)
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("退出应用", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue button
        TextButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "继续使用",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 100% 软提醒内容（允许继续使用）
 */
@Composable
private fun LimitReachedSoftContent(
    appName: String,
    usedMinutes: Int,
    redirectPackage: String?,
    onRedirect: (String) -> Unit,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // Icon
        Text(
            text = "\u26A0\uFE0F", // Warning emoji
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "已达到今日限额",
            color = Color(0xFFFFB74D), // Orange color
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "今日已使用 $usedMinutes 分钟",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "建议休息一下",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Redirect button (if configured)
        if (redirectPackage != null) {
            Button(
                onClick = { onRedirect(redirectPackage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("去看书吧", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Exit button (recommended)
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("退出应用", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Continue button
        TextButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "继续使用",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 100% 强制关闭内容（不允许继续使用）
 * @param limitMinutes 限额分钟数，0 表示无限制模式（强制阻止）
 */
@Composable
private fun LimitReachedStrictContent(
    appName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    redirectPackage: String?,
    onRedirect: (String) -> Unit,
    onGoHome: () -> Unit
) {
    // 无限制模式：limitMinutes = 0
    val isNoLimitMode = limitMinutes == 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // Icon
        Text(
            text = "\uD83D\uDEAB", // No entry emoji
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title - 根据模式显示不同文案
        Text(
            text = if (isNoLimitMode) "该应用已被限制" else "今日限额已用完",
            color = Color(0xFFEF5350), // Red color
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 根据模式显示不同内容
                if (isNoLimitMode) {
                    Text(
                        text = "已设置为禁止使用",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp
                    )
                } else {
                    Text(
                        text = "今日已使用 $usedMinutes 分钟",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isNoLimitMode) "做点别的事吧!" else "明天再见!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Redirect button (if configured)
        if (redirectPackage != null) {
            Button(
                onClick = { onRedirect(redirectPackage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("去看书吧", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Go home button
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("返回桌面", fontSize = 18.sp)
        }

        // Note: No "continue" option in strict mode
    }
}
