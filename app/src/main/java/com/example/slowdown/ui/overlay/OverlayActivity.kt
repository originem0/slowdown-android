package com.sharonZ.slowdown.ui.overlay

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharonZ.slowdown.R
import com.sharonZ.slowdown.SlowDownApp
import com.sharonZ.slowdown.ui.theme.*
import com.sharonZ.slowdown.util.LocaleHelper
import com.sharonZ.slowdown.util.NotificationHelper
import com.sharonZ.slowdown.util.PackageUtils
import com.sharonZ.slowdown.viewmodel.OverlayViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "SlowDown"
        private const val REMINDER_LOAD_TIMEOUT_MS = 500L  // 自定义提醒语加载超时
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        const val EXTRA_REDIRECT_PACKAGE = "redirect_package"
        const val EXTRA_REDIRECT_APP_NAME = "redirect_app_name"
        const val EXTRA_IS_LIMIT_REACHED = "is_limit_reached"  // 是否已达限额
        const val EXTRA_USED_MINUTES = "used_minutes"  // 已使用分钟数
        const val EXTRA_LIMIT_MINUTES = "limit_minutes"  // 限额分钟数
    }

    private val viewModel: OverlayViewModel by viewModels {
        OverlayViewModel.Factory((application as SlowDownApp).repository)
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(newBase)
            return
        }

        // 使用缓存的语言设置，避免阻塞 I/O（从 1000ms 降至 <1ms）
        val app = newBase.applicationContext as? SlowDownApp
        val language = app?.cachedLanguage ?: "en"

        val localizedContext = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "[Overlay] onCreate called")

        // 取消通知
        NotificationHelper.cancelNotification(this)

        // 设置窗口属性
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
        val redirectAppName = intent.getStringExtra(EXTRA_REDIRECT_APP_NAME) ?: getString(R.string.overlay_redirect_app_default)
        val isLimitReached = intent.getBooleanExtra(EXTRA_IS_LIMIT_REACHED, false)
        val usedMinutes = intent.getIntExtra(EXTRA_USED_MINUTES, 0)
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)

        Log.d(TAG, "[Overlay] packageName=$packageName, appName=$appName, countdown=$countdownSeconds, isLimitReached=$isLimitReached")

        viewModel.startCountdown(packageName, appName, countdownSeconds)

        setContent {
            // 异步加载自定义提醒语，避免阻塞 UI 渲染
            var customReminderText by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                customReminderText = withTimeoutOrNull(REMINDER_LOAD_TIMEOUT_MS) {
                    val texts = (application as SlowDownApp).userPreferences.customReminderTexts.first()
                    val lines = texts.split("\n").filter { it.isNotBlank() }.take(10)
                    lines.randomOrNull()
                }
            }

            SlowDownTheme {
                MindfulOverlayScreen(
                    viewModel = viewModel,
                    appName = appName,
                    totalSeconds = countdownSeconds,
                    redirectPackage = redirectPackage,
                    redirectAppName = redirectAppName,
                    isLimitReached = isLimitReached,
                    usedMinutes = usedMinutes,
                    limitMinutes = limitMinutes,
                    customReminderText = customReminderText,
                    onContinue = {
                        viewModel.recordAndFinish("continued")
                        try {
                            PackageUtils.launchApp(this, packageName)
                        } catch (e: Exception) {
                            Log.e(TAG, "[Overlay] Failed to launch app $packageName: ${e.message}")
                        }
                        finish()
                    },
                    onRedirect = { redirectPkg ->
                        viewModel.recordAndFinish("redirected")
                        try {
                            PackageUtils.launchApp(this, redirectPkg)
                        } catch (e: Exception) {
                            Log.e(TAG, "[Overlay] Failed to launch redirect app $redirectPkg: ${e.message}")
                        }
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
private fun MindfulOverlayScreen(
    viewModel: OverlayViewModel,
    appName: String,
    totalSeconds: Int,
    redirectPackage: String?,
    redirectAppName: String,
    isLimitReached: Boolean,
    usedMinutes: Int,
    limitMinutes: Int,
    customReminderText: String?,
    onContinue: () -> Unit,
    onRedirect: (String) -> Unit,
    onCancel: () -> Unit
) {
    val countdown by viewModel.countdown.collectAsState()
    val canContinue by viewModel.canContinue.collectAsState()

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // 渐变背景色 - 已达限额时用更暖的色调
    val backgroundGradient = if (isLimitReached) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2A2632),  // 偏紫的深色
                Color(0xFF352E42),
                Color(0xFF3D3852)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A2632),  // 深蓝灰
                Color(0xFF243442),  // 较浅的深蓝灰
                Color(0xFF2D4A5E)   // 带一点青色的深蓝
            )
        )
    }

    // 备用纯色背景（确保渐变渲染失败时不会是透明/黑色）
    val fallbackBackground = if (isLimitReached) Color(0xFF2A2632) else Color(0xFF1A2632)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fallbackBackground)  // 底层纯色备用
            .background(backgroundGradient), // 上层渐变
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰 - 柔和的圆形
        BackgroundDecorations(breathScale)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // 顶部提示语 - 根据是否达到限额显示不同文案
            if (isLimitReached) {
                Text(
                    text = stringResource(R.string.overlay_take_a_break),
                    color = Color(0xFFFFB74D).copy(alpha = 0.7f),  // 橙色
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.overlay_limit_reached, appName),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 显示使用时间
                Text(
                    text = stringResource(R.string.overlay_used_today, usedMinutes),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.overlay_deep_breath),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.overlay_about_to_open, appName),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 呼吸圈 + 倒计时
            BreathingCountdownCircle(
                countdown = countdown,
                totalSeconds = totalSeconds,
                canContinue = canContinue,
                breathScale = breathScale,
                isLimitReached = isLimitReached
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 显示自定义提醒语 - 位置移至呼吸圈下方
            if (!customReminderText.isNullOrBlank()) {
                Text(
                    text = "「$customReminderText」",
                    color = Color(0xFFFFD54F),  // 金黄色
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,  // 加粗
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 提示文字
            Text(
                text = stringResource(if (canContinue) R.string.overlay_can_continue else R.string.overlay_follow_circle),
                color = if (canContinue) Sage400 else Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(56.dp))

            // 跳转替代应用按钮
            if (redirectPackage != null) {
                Button(
                    onClick = { onRedirect(redirectPackage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Sage500
                    )
                ) {
                    Text(
                        text = stringResource(R.string.overlay_open_redirect, redirectAppName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 继续使用按钮
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canContinue) Teal500 else Color.Gray.copy(alpha = 0.3f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = if (canContinue)
                        stringResource(R.string.overlay_continue_using, appName)
                    else
                        stringResource(R.string.overlay_please_wait, countdown),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (canContinue) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 返回按钮 - 更低调
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.overlay_give_up_go_home),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BackgroundDecorations(breathScale: Float) {
    // 柔和的背景圆形装饰
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2 - 100)

        // 外层大圆 - 非常淡
        drawCircle(
            color = Color.White.copy(alpha = 0.03f),
            radius = 280.dp.toPx() * breathScale,
            center = center
        )

        // 中层圆
        drawCircle(
            color = Color.White.copy(alpha = 0.02f),
            radius = 220.dp.toPx() * breathScale,
            center = center
        )
    }
}

@Composable
private fun BreathingCountdownCircle(
    countdown: Int,
    totalSeconds: Int,
    canContinue: Boolean,
    breathScale: Float,
    isLimitReached: Boolean = false
) {
    val progress = if (totalSeconds > 0) {
        1f - (countdown.toFloat() / totalSeconds)
    } else 1f

    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    // 根据是否达到限额使用不同的颜色
    val progressColor = if (isLimitReached) Color(0xFFFFB74D) else Teal500  // 橙色 vs 青色

    Box(
        modifier = Modifier.size((180 * breathScale).dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 背景圈 - 淡色
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 进度圈 - 渐变
            if (animatedProgress > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = if (isLimitReached) {
                            listOf(Color(0xFFFFB74D), Color(0xFFFF8A65), Color(0xFFFFB74D))
                        } else {
                            listOf(Teal500, Sage400, Teal500)
                        },
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 中心内容
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (canContinue) {
                Text(
                    text = "✓",
                    fontSize = 48.sp,
                    color = if (isLimitReached) Color(0xFFFFB74D) else Sage400
                )
            } else {
                Text(
                    text = countdown.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
            }
        }
    }
}

// 缓动函数
private val EaseInOutSine: Easing = Easing { fraction ->
    (-(cos(PI * fraction) - 1) / 2).toFloat()
}
