package com.sharonZ.slowdown.ui.warning

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.sharonZ.slowdown.R
import com.sharonZ.slowdown.SlowDownApp
import com.sharonZ.slowdown.service.UsageWarningType
import com.sharonZ.slowdown.ui.theme.*
import com.sharonZ.slowdown.util.LocaleHelper
import com.sharonZ.slowdown.util.PackageUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos

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

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(newBase)
            return
        }

        // Get the saved language preference synchronously
        val app = newBase.applicationContext as? SlowDownApp
        val language = if (app != null) {
            runBlocking {
                withTimeoutOrNull(1000L) {  // 1 second timeout
                    app.userPreferences.appLanguage.first()
                } ?: "en"  // Default to English if timeout
            }
        } else {
            "en"
        }

        val localizedContext = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    // 缓慢呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "gentle_breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // 根据警告类型选择背景渐变色
    val backgroundGradient = when (warningType) {
        UsageWarningType.SOFT_REMINDER -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A3640),  // 深蓝灰
                Color(0xFF3A4856),
                Color(0xFF4A5A6A)
            )
        )
        UsageWarningType.LIMIT_REACHED_SOFT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF362A20),  // 暖褐色
                Color(0xFF483C30),
                Color(0xFF5A4E40)
            )
        )
        UsageWarningType.LIMIT_REACHED_STRICT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A2030),  // 深紫红
                Color(0xFF3A2A40),
                Color(0xFF4A3650)
            )
        )
    }

    val fallbackColor = when (warningType) {
        UsageWarningType.SOFT_REMINDER -> Color(0xFF2A3640)
        UsageWarningType.LIMIT_REACHED_SOFT -> Color(0xFF362A20)
        UsageWarningType.LIMIT_REACHED_STRICT -> Color(0xFF2A2030)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fallbackColor)
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰圆
        WarningBackgroundDecorations(breathScale, warningType)

        when (warningType) {
            UsageWarningType.SOFT_REMINDER -> {
                UsageWarning80PercentContent(
                    appName = appName,
                    usedMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    breathScale = breathScale,
                    onContinue = onContinue,
                    onExit = onExit
                )
            }
            UsageWarningType.LIMIT_REACHED_SOFT -> {
                LimitReachedSoftContent(
                    appName = appName,
                    usedMinutes = usedMinutes,
                    breathScale = breathScale,
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
                    breathScale = breathScale,
                    redirectPackage = redirectPackage,
                    onRedirect = onRedirect,
                    onGoHome = onGoHome
                )
            }
        }
    }
}

/**
 * 背景装饰动画圆
 */
@Composable
private fun WarningBackgroundDecorations(breathScale: Float, warningType: UsageWarningType) {
    val decorColor = when (warningType) {
        UsageWarningType.SOFT_REMINDER -> Color(0xFF5A9AAA)       // 青色
        UsageWarningType.LIMIT_REACHED_SOFT -> Color(0xFFD4A574)  // 暖橙
        UsageWarningType.LIMIT_REACHED_STRICT -> Color(0xFFA07898) // 紫红
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height * 0.35f)

        // 外层大圆
        drawCircle(
            color = decorColor.copy(alpha = 0.06f),
            radius = 200.dp.toPx() * breathScale,
            center = center
        )

        // 中层圆
        drawCircle(
            color = decorColor.copy(alpha = 0.04f),
            radius = 150.dp.toPx() * breathScale,
            center = center
        )

        // 内层圆
        drawCircle(
            color = decorColor.copy(alpha = 0.03f),
            radius = 100.dp.toPx() * breathScale,
            center = center
        )
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
    breathScale: Float,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    val remainingMinutes = limitMinutes - usedMinutes

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // 顶部提示语
        Text(
            text = stringResource(R.string.warning_gentle_reminder),
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 主标题
        Text(
            text = stringResource(R.string.warning_remaining_minutes, appName, remainingMinutes),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 副文案
        Text(
            text = stringResource(R.string.warning_used_minutes, usedMinutes),
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(56.dp))

        // 鼓励文案
        Text(
            text = stringResource(R.string.warning_good_time_pause),
            color = Color(0xFF7ECEC4).copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 退出按钮（推荐）
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sage500)
        ) {
            Text(
                text = stringResource(R.string.warning_take_a_rest),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 继续按钮
        TextButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.warning_continue_aware),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium
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
    breathScale: Float,
    redirectPackage: String?,
    onRedirect: (String) -> Unit,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        // 顶部提示语
        Text(
            text = stringResource(R.string.warning_soft_reminder),
            color = Color(0xFFE8C89C).copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 主标题
        Text(
            text = stringResource(R.string.warning_limit_reached, appName),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 使用时间
        Text(
            text = stringResource(R.string.warning_used_minutes, usedMinutes),
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(56.dp))

        // 鼓励文案
        Text(
            text = stringResource(R.string.warning_do_something_else),
            color = Color(0xFFE8C89C).copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 跳转替代应用按钮
        if (redirectPackage != null) {
            Button(
                onClick = { onRedirect(redirectPackage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sage500)
            ) {
                Text(
                    text = stringResource(R.string.warning_go_do_other),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 退出按钮
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text = stringResource(R.string.warning_go_home),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 继续使用按钮
        TextButton(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.warning_use_more),
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodyMedium
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
    breathScale: Float,
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
        // 顶部提示语
        Text(
            text = stringResource(if (isNoLimitMode) R.string.warning_focus_guard else R.string.warning_today_stop),
            color = Color(0xFFD4A0C0).copy(alpha = 0.7f),
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 主标题
        Text(
            text = stringResource(
                if (isNoLimitMode) R.string.warning_app_restricted else R.string.warning_quota_used_up,
                appName
            ),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 使用时间或限制说明
        Text(
            text = if (isNoLimitMode)
                stringResource(R.string.warning_app_not_available)
            else
                stringResource(R.string.warning_used_minutes, usedMinutes),
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(56.dp))

        // 鼓励文案
        Text(
            text = stringResource(if (isNoLimitMode) R.string.warning_give_space else R.string.warning_tomorrow_new_day),
            color = Color(0xFFD4A0C0).copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 跳转替代应用按钮
        if (redirectPackage != null) {
            Button(
                onClick = { onRedirect(redirectPackage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sage500)
            ) {
                Text(
                    text = stringResource(R.string.warning_go_do_other),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 返回桌面按钮
        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text = stringResource(R.string.warning_go_home),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // 没有继续使用选项
    }
}

// 缓动函数
private val EaseInOutSine: Easing = Easing { fraction ->
    (-(cos(PI * fraction) - 1) / 2).toFloat()
}
