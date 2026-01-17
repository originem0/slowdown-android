package com.example.slowdown.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.slowdown.ui.theme.ChartColors
import com.example.slowdown.ui.theme.Error
import com.example.slowdown.ui.theme.Success

/**
 * 图表数据段
 */
data class ChartSegment(
    val value: Float,      // 占比 0-1
    val color: Color,      // 颜色
    val label: String      // 应用名
)

/**
 * 圆形进度图组件
 * - 外圈：各应用占比弧段
 * - 中心：今日总计时长
 * - 底部：与昨天对比
 */
@Composable
fun CircularProgressChart(
    segments: List<ChartSegment>,
    totalText: String,
    comparisonText: String?,
    isIncrease: Boolean?,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 16.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 圆环
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val radius = (this.size.minDimension - strokePx) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val arcSize = Size(radius * 2, radius * 2)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            if (segments.isEmpty()) {
                // 无数据时显示灰色圆环
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            } else {
                // 绘制各应用占比弧段
                var startAngle = -90f // 从顶部开始
                val gapAngle = 2f // 各段之间的间隙

                segments.forEach { segment ->
                    val sweepAngle = (segment.value * 360f).coerceAtLeast(gapAngle)

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
            }
        }

        // 中心文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = totalText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (comparisonText != null && isIncrease != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isIncrease) "▲" else "▼",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isIncrease) Error else Success
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isIncrease) Error else Success
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
