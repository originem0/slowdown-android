package com.sharonZ.slowdown.viewmodel

/**
 * 权限状态数据类，供多个 ViewModel 复用
 */
data class PermissionState(
    // === 必要权限（核心功能必须）===
    val accessibilityEnabled: Boolean = false,           // 无障碍服务 - 检测应用启动
    val overlayEnabled: Boolean = false,                 // 悬浮窗权限 - 显示弹窗
    val usageStatsEnabled: Boolean = false,              // 使用统计权限 - 追踪使用时长

    // === 建议权限（提升稳定性）===
    val batteryOptimizationDisabled: Boolean = false,    // 电池优化 - 防止后台被杀

    // === MIUI 特殊权限 ===
    val isMiui: Boolean = false,
    // MIUI 后台弹出权限 - 必要权限（可通过 API 检测）
    val miuiBackgroundPopupGranted: Boolean = false,
    // 以下权限无法通过 API 检测，使用用户确认方式（建议权限）
    val miuiAutoStartConfirmed: Boolean = false,         // 自启动
    val miuiBackgroundPopupConfirmed: Boolean = false,   // 用户确认（备用）
    val miuiBatterySaverConfirmed: Boolean = false,      // 省电策略设为无限制
    val miuiLockAppConfirmed: Boolean = false            // 在最近任务中锁定应用
) {
    /**
     * 检查是否所有必需权限都已设置
     * 必要权限：无障碍、悬浮窗、使用统计、MIUI 后台弹窗（仅 MIUI）
     */
    val allRequiredPermissionsGranted: Boolean
        get() = accessibilityEnabled &&
                overlayEnabled &&
                usageStatsEnabled &&
                (!isMiui || miuiBackgroundPopupGranted)

    /**
     * 检查 MIUI 建议权限是否需要配置
     * 只有在 MIUI 设备上且核心权限已设置后才需要关注
     */
    val miuiPermissionsNeeded: Boolean
        get() = isMiui && allRequiredPermissionsGranted &&
                (!miuiAutoStartConfirmed || !miuiBatterySaverConfirmed || !miuiLockAppConfirmed)

    /**
     * 获取缺失的必要权限列表
     */
    val missingRequiredPermissions: List<String>
        get() = buildList {
            if (!accessibilityEnabled) add("无障碍服务")
            if (!overlayEnabled) add("悬浮窗权限")
            if (!usageStatsEnabled) add("使用统计权限")
            if (isMiui && !miuiBackgroundPopupGranted) add("后台弹出界面")
        }

    /**
     * 获取缺失的建议权限列表
     */
    val missingRecommendedPermissions: List<String>
        get() = buildList {
            if (!batteryOptimizationDisabled) add("电池优化")
            if (isMiui) {
                if (!miuiAutoStartConfirmed) add("自启动权限")
                if (!miuiBatterySaverConfirmed) add("省电策略")
                if (!miuiLockAppConfirmed) add("锁定应用")
            }
        }

    /**
     * 获取缺失的 MIUI 权限列表（已废弃，使用 missingRequiredPermissions 和 missingRecommendedPermissions）
     */
    @Deprecated("Use missingRequiredPermissions and missingRecommendedPermissions instead")
    val missingMiuiPermissions: List<String>
        get() = buildList {
            if (isMiui) {
                if (!miuiBackgroundPopupGranted) add("后台弹出界面")
                if (!miuiAutoStartConfirmed) add("自启动权限")
                if (!miuiBatterySaverConfirmed) add("省电策略")
                if (!miuiLockAppConfirmed) add("锁定应用")
            }
        }
}
