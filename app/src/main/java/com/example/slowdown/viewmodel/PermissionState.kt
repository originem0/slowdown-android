package com.example.slowdown.viewmodel

/**
 * 权限状态数据类，供多个 ViewModel 复用
 */
data class PermissionState(
    val accessibilityEnabled: Boolean = false,
    val overlayEnabled: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val isMiui: Boolean = false,
    // MIUI 后台弹出权限 - 可通过 API 检测
    val miuiBackgroundPopupGranted: Boolean = false,  // 实际权限状态
    // 以下权限无法通过 API 检测，使用用户确认方式
    val miuiAutoStartConfirmed: Boolean = false,
    val miuiBackgroundPopupConfirmed: Boolean = false,  // 用户确认（备用）
    val miuiBatterySaverConfirmed: Boolean = false,  // 省电策略设为无限制
    val miuiLockAppConfirmed: Boolean = false        // 在最近任务中锁定应用
) {
    /**
     * 检查是否所有必需权限都已设置
     */
    val allRequiredPermissionsGranted: Boolean
        get() = accessibilityEnabled && overlayEnabled

    /**
     * 检查 MIUI 专属权限是否需要配置
     * 只有在 MIUI 设备上且核心权限已设置后才需要关注
     */
    val miuiPermissionsNeeded: Boolean
        get() = isMiui && allRequiredPermissionsGranted &&
                (!miuiAutoStartConfirmed || !miuiBackgroundPopupGranted ||
                 !miuiBatterySaverConfirmed || !miuiLockAppConfirmed)

    /**
     * 获取缺失的权限列表
     */
    val missingPermissions: List<String>
        get() = buildList {
            if (!accessibilityEnabled) add("无障碍服务")
            if (!overlayEnabled) add("悬浮窗权限")
            if (!batteryOptimizationDisabled) add("电池优化")
        }

    /**
     * 获取缺失的 MIUI 权限列表
     */
    val missingMiuiPermissions: List<String>
        get() = buildList {
            if (isMiui) {
                if (!miuiAutoStartConfirmed) add("自启动权限")
                if (!miuiBackgroundPopupGranted) add("后台弹出界面")
                if (!miuiBatterySaverConfirmed) add("省电策略")
                if (!miuiLockAppConfirmed) add("锁定应用")
            }
        }
}
