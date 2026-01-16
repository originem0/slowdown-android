package com.example.slowdown.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * MIUI 特定功能帮助类
 *
 * 小米在 MIUI 中增加了 "后台弹出界面" 权限限制，默认禁止应用从后台启动 Activity。
 * 通过设置 Intent 的 mMiuiFlags = 0x2 可以绕过这个限制。
 *
 * 参考: https://ljd1996.github.io/2021/01/26/Android后台启动的实践之路二/
 */
object MiuiHelper {

    private const val TAG = "SlowDown"
    private const val MIUI_FLAGS_ALLOW_BACKGROUND = 0x2

    /**
     * 检查是否是 MIUI 系统
     */
    fun isMiui(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    /**
     * 尝试绕过 Android 9+ 的反射限制
     * 使用双重反射（Meta-reflection）方法
     */
    fun tryUnsealReflection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return // Android 9 以下不需要
        }

        try {
            // 方法1: 使用双重反射绕过 hidden API 检测
            // 原理：通过 Class.class 的 getDeclaredMethod 来获取 getDeclaredField
            // 这样可以避免被 hidden API 检测拦截
            val forNameMethod = Class::class.java.getDeclaredMethod("forName", String::class.java)
            val vmRuntimeClass = forNameMethod.invoke(null, "dalvik.system.VMRuntime") as Class<*>

            val getMethodMethod = Class::class.java.getDeclaredMethod(
                "getMethod",
                String::class.java,
                arrayOf<Class<*>>()::class.java
            )

            val getRuntime = getMethodMethod.invoke(vmRuntimeClass, "getRuntime", null) as java.lang.reflect.Method
            val vmRuntime = getRuntime.invoke(null)

            val setHiddenApiExemptions = getMethodMethod.invoke(
                vmRuntimeClass,
                "setHiddenApiExemptions",
                arrayOf(Array<String>::class.java)
            ) as java.lang.reflect.Method

            setHiddenApiExemptions.invoke(vmRuntime, arrayOf("L"))
            Log.d(TAG, "[MiuiHelper] Successfully unsealed hidden API via meta-reflection")
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] Meta-reflection failed: ${e.message}")
            // 尝试备用方法
            tryUnsealViaUnsafe()
        }
    }

    /**
     * 备用方法：通过 Unsafe 绕过
     */
    private fun tryUnsealViaUnsafe() {
        try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
            theUnsafeField.isAccessible = true
            val unsafe = theUnsafeField.get(null)

            // 获取 VMRuntime
            val vmRuntimeClass = Class.forName("dalvik.system.VMRuntime")
            val getRuntimeMethod = vmRuntimeClass.getDeclaredMethod("getRuntime")
            getRuntimeMethod.isAccessible = true
            val vmRuntime = getRuntimeMethod.invoke(null)

            // 获取 setHiddenApiExemptions 方法
            val setHiddenApiExemptionsMethod = vmRuntimeClass.getDeclaredMethod(
                "setHiddenApiExemptions",
                Array<String>::class.java
            )
            setHiddenApiExemptionsMethod.isAccessible = true
            setHiddenApiExemptionsMethod.invoke(vmRuntime, arrayOf("L"))

            Log.d(TAG, "[MiuiHelper] Successfully unsealed hidden API via Unsafe")
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] Unsafe method also failed: ${e.message}")
        }
    }

    /**
     * 为 Intent 设置 MIUI 特定标志，允许从后台启动 Activity
     *
     * 小米在 Intent 类中增加了 mMiuiFlags 字段：
     * - 当 (intent.getMiuiFlags() & 0x2) != 0 时，跳过后台启动权限检查
     *
     * @param intent 要修改的 Intent
     * @return true 如果设置成功，false 如果失败（非 MIUI 或反射失败）
     */
    fun addMiuiFlags(intent: Intent): Boolean {
        if (!isMiui()) {
            Log.d(TAG, "[MiuiHelper] Not MIUI device, skip setting MiuiFlags")
            return false
        }

        // 方法1: 尝试调用 addMiuiFlags 方法
        try {
            val addMiuiFlagsMethod = Intent::class.java.getMethod("addMiuiFlags", Int::class.javaPrimitiveType)
            addMiuiFlagsMethod.invoke(intent, MIUI_FLAGS_ALLOW_BACKGROUND)
            Log.d(TAG, "[MiuiHelper] Successfully called addMiuiFlags(0x2)")
            return true
        } catch (e: NoSuchMethodException) {
            Log.d(TAG, "[MiuiHelper] addMiuiFlags method not found, trying field reflection")
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] addMiuiFlags failed: ${e.message}")
        }

        // 方法2: 直接设置 mMiuiFlags 字段
        try {
            val field = Intent::class.java.getDeclaredField("mMiuiFlags")
            field.isAccessible = true
            field.setInt(intent, MIUI_FLAGS_ALLOW_BACKGROUND)
            Log.d(TAG, "[MiuiHelper] Successfully set mMiuiFlags = 0x2 via field reflection")
            return true
        } catch (e: NoSuchFieldException) {
            Log.e(TAG, "[MiuiHelper] mMiuiFlags field not found (may not be MIUI ROM)")
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] Failed to set mMiuiFlags: ${e.message}")
        }

        // 方法3: 尝试使用 setMiuiFlags 方法
        try {
            val setMiuiFlagsMethod = Intent::class.java.getMethod("setMiuiFlags", Int::class.javaPrimitiveType)
            setMiuiFlagsMethod.invoke(intent, MIUI_FLAGS_ALLOW_BACKGROUND)
            Log.d(TAG, "[MiuiHelper] Successfully called setMiuiFlags(0x2)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] setMiuiFlags also failed: ${e.message}")
        }

        return false
    }

    /**
     * 检查应用是否有 MIUI 后台弹出界面权限
     * OpCode 10021 = OP_BACKGROUND_START_ACTIVITY
     *
     * @return true 如果有权限或非 MIUI，false 如果 MIUI 且没有权限
     */
    fun canBackgroundStart(context: Context): Boolean {
        if (!isMiui()) {
            return true // 非 MIUI 默认允许
        }

        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE)
            val opCode = 10021 // MIUI's OP_BACKGROUND_START_ACTIVITY

            // 使用反射调用 checkOpNoThrow
            val checkOpMethod = appOpsManager.javaClass.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )

            val uid = android.os.Process.myUid()
            val result = checkOpMethod.invoke(appOpsManager, opCode, uid, context.packageName) as Int

            // 0 = MODE_ALLOWED, 1 = MODE_IGNORED, 2 = MODE_ERRORED, 3 = MODE_DEFAULT
            val allowed = result == 0 || result == 3
            Log.d(TAG, "[MiuiHelper] canBackgroundStart check: opCode=$opCode, result=$result, allowed=$allowed")
            allowed
        } catch (e: Exception) {
            Log.e(TAG, "[MiuiHelper] Failed to check canBackgroundStart: ${e.message}")
            // 检查失败时假设没有权限
            false
        }
    }
}
