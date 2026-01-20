package com.sharonZ.slowdown.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sharonZ.slowdown.ui.theme.ChartColors
import com.sharonZ.slowdown.ui.theme.Error
import com.sharonZ.slowdown.ui.theme.Success

/**
 * 图表数据段
 */
data class ChartSegment(
    val value: Float,      // 占比 0-1
    val color: Color,      // 颜色
    val label: String      // 应用名
)

/**
 * 圆形进度图组件 - 简洁优化版
 *
 * 有效特性:
 * - 双层圆环(可见的背景层 + 数据层)
 * - 流畅的绘制动画
 * - 精致的虚线空状态
 * - 改进的视觉对比度
 */
@Composable
fun CircularProgressChart(
    segments: List<ChartSegment>,
    totalText: String,
    comparisonText: String?,
    isIncrease: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 20.dp
) {
    // 动画进度 0f -> 1f
    val animatedProgress by animateFloatAsState(
        targetValue = if (segments.isEmpty()) 0f else 1f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "chartProgress"
    )

    // 获取当前主题颜色用于背景层
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 圆环画布
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            // 1. 背景圆环(明显可见的底层)
            drawArc(
                color = surfaceVariant.copy(alpha = 0.4f),  // 提高到40%透明度,确保可见
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (segments.isNotEmpty()) {
                // 2. 数据层(彩色圆弧)
                var startAngle = -90f // 从顶部开始
                val gapAngle = 4f // 间隙增大到4度,更明显

                segments.forEach { segment ->
                    val sweepAngle = (segment.value * 360f * animatedProgress).coerceAtLeast(gapAngle)

                    // 使用纯色,不用渐变(渐变在描边上效果不好)
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = (sweepAngle - gapAngle).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    startAngle += sweepAngle
                }
            } else {
                // 空状态: 精致的虚线圆环
                val dashEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(20f, 12f),  // 更长的虚线段,更美观
                    phase = 0f
                )
                drawArc(
                    color = surfaceVariant.copy(alpha = 0.5f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokePx * 0.7f,
                        cap = StrokeCap.Round,
                        pathEffect = dashEffect
                    )
                )
            }
        }

        // 中心内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 主数值
            Text(
                text = totalText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 对比信息
            if (comparisonText != null && isIncrease != null) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 趋势图标 (使用更清晰的箭头)
                    Text(
                        text = if (isIncrease) "↑" else "↓",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isIncrease) Error else Success,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // 对比文字
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isIncrease) Error else Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 从应用使用数据创建图表段
 */
fun createChartSegments(
    appUsages: List<Pair<String, Int>>, // (appName, minutes)
    totalMinutes: Int
): List<ChartSegment> {
    if (totalMinutes <= 0) return emptyList()

    return appUsages.mapIndexed { index, (appName, minutes) ->
        ChartSegment(
            value = minutes.toFloat() / totalMinutes,
            color = ChartColors[index % ChartColors.size],
            label = appName
        )
    }
}
