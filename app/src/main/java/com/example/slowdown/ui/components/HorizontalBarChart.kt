package com.example.slowdown.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 横向条形图数据
 */
data class BarData(
    val label: String,       // "周一"
    val value: Float,        // 分钟数
    val displayText: String, // "2h"
    val isSelected: Boolean = false
)

/**
 * 横向条形图组件
 * 用于展示本周每天的使用趋势
 */
@Composable
fun HorizontalBarChart(
    data: List<BarData>,
    maxValue: Float,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    selectedBarColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (data.isEmpty()) return

    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val actualMaxValue = maxValue.coerceAtLeast(1f)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        data.forEach { bar ->
            HorizontalBarItem(
                data = bar,
                maxValue = actualMaxValue,
                barColor = if (bar.isSelected) selectedBarColor else barColor,
                backgroundColor = backgroundColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HorizontalBarItem(
    data: BarData,
    maxValue: Float,
    barColor: Color,
    backgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 星期标签
        Text(
            text = data.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (data.isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (data.isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 条形图
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
        ) {
            val fraction = (data.value / maxValue).coerceIn(0f, 1f)
            val primaryColor = barColor
            val primaryLight = barColor.copy(alpha = 0.6f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                // 背景条
                drawRoundRect(
                    color = backgroundColor,
                    cornerRadius = cornerRadius,
                    size = Size(size.width, size.height)
                )

                // 实际数据条
                if (fraction > 0) {
                    val barWidth = size.width * fraction
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor, primaryLight),
                            startX = 0f,
                            endX = barWidth
                        ),
                        cornerRadius = cornerRadius,
                        size = Size(barWidth, size.height)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 时长文字
        Text(
            text = data.displayText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (data.isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (data.isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}
