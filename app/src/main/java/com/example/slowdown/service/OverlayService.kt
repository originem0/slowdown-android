package com.example.slowdown.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.slowdown.R
import com.example.slowdown.SlowDownApp
import com.example.slowdown.util.PackageUtils
import kotlinx.coroutines.*

class OverlayService : Service() {

    companion object {
        private const val TAG = "SlowDown"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"
        const val EXTRA_REDIRECT_PACKAGE = "redirect_package"
        const val EXTRA_IS_LIMIT_REACHED = "is_limit_reached"
        const val EXTRA_USED_MINUTES = "used_minutes"
        const val EXTRA_LIMIT_MINUTES = "limit_minutes"

        fun start(
            context: Context,
            packageName: String,
            appName: String,
            countdownSeconds: Int,
            redirectPackage: String?,
            isLimitReached: Boolean = false,
            usedMinutes: Int = 0,
            limitMinutes: Int = 0
        ) {
            val intent = Intent(context, OverlayService::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_COUNTDOWN_SECONDS, countdownSeconds)
                putExtra(EXTRA_REDIRECT_PACKAGE, redirectPackage)
                putExtra(EXTRA_IS_LIMIT_REACHED, isLimitReached)
                putExtra(EXTRA_USED_MINUTES, usedMinutes)
                putExtra(EXTRA_LIMIT_MINUTES, limitMinutes)
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var countDownTimer: CountDownTimer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val repository by lazy {
        (application as SlowDownApp).repository
    }

    private var targetPackageName: String = ""
    private var targetAppName: String = ""
    private var countdownDuration: Int = 10
    private var startTime: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "[OverlayService] onStartCommand called")

        if (intent == null) {
            Log.e(TAG, "[OverlayService] Intent is null, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (packageName.isNullOrEmpty()) {
            Log.e(TAG, "[OverlayService] No packageName in intent, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        targetPackageName = packageName
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val countdownSeconds = intent.getIntExtra(EXTRA_COUNTDOWN_SECONDS, 10)
        targetAppName = appName
        countdownDuration = countdownSeconds
        val redirectPackage = intent.getStringExtra(EXTRA_REDIRECT_PACKAGE)

        Log.d(TAG, "[OverlayService] packageName=$packageName, appName=$appName, countdown=$countdownSeconds")

        // Remove existing overlay if any
        removeOverlay()

        // Show new overlay
        showOverlay(packageName, appName, countdownSeconds, redirectPackage)

        return START_NOT_STICKY
    }

    private fun showOverlay(
        packageName: String,
        appName: String,
        countdownSeconds: Int,
        redirectPackage: String?
    ) {
        Log.d(TAG, "[OverlayService] showOverlay called")
        startTime = System.currentTimeMillis()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Inflate layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_window, null)

        // Setup window params - 使用更高优先级的flags确保显示在其他应用上方
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 关键：移除 FLAG_NOT_FOCUSABLE，让窗口获得焦点以保持最高层级
            // 添加 FLAG_LAYOUT_IN_SCREEN 确保全屏覆盖
            // 添加 FLAG_SHOW_WHEN_LOCKED 在锁屏时也能显示
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        // Setup views
        val tvAppName = overlayView?.findViewById<TextView>(R.id.tv_app_name)
        val tvCountdown = overlayView?.findViewById<TextView>(R.id.tv_countdown)
        val tvHint = overlayView?.findViewById<TextView>(R.id.tv_hint)
        val btnRedirect = overlayView?.findViewById<Button>(R.id.btn_redirect)
        val btnContinue = overlayView?.findViewById<Button>(R.id.btn_continue)
        val btnCancel = overlayView?.findViewById<TextView>(R.id.btn_cancel)

        tvAppName?.text = appName

        // Redirect button
        if (redirectPackage != null) {
            btnRedirect?.visibility = View.VISIBLE
            btnRedirect?.setOnClickListener {
                Log.d(TAG, "[OverlayService] Redirect clicked")
                recordAndFinish(packageName, "redirected")
                PackageUtils.launchApp(this, redirectPackage)
            }
        } else {
            btnRedirect?.visibility = View.GONE
        }

        // Continue button - initially disabled
        btnContinue?.isEnabled = false
        btnContinue?.text = "等待中..."
        btnContinue?.setOnClickListener {
            Log.d(TAG, "[OverlayService] Continue clicked")
            recordAndFinish(packageName, "continued")
            PackageUtils.launchApp(this, packageName)
        }

        // Cancel button
        btnCancel?.setOnClickListener {
            Log.d(TAG, "[OverlayService] Cancel clicked")
            recordAndFinish(packageName, "cancelled")
            PackageUtils.goHome(this)
        }

        // Start countdown
        countDownTimer = object : CountDownTimer(countdownSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                tvCountdown?.text = seconds.toString()
            }

            override fun onFinish() {
                tvCountdown?.text = "可以继续了"
                tvCountdown?.textSize = 24f
                tvHint?.visibility = View.GONE
                btnContinue?.isEnabled = true
                btnContinue?.text = "继续访问"
            }
        }.start()

        // Add view to window
        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "[OverlayService] Overlay view added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "[OverlayService] Failed to add overlay view: ${e.message}")
            stopSelf()
        }
    }

    private fun recordAndFinish(packageName: String, userChoice: String) {
        val actualWaitTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        serviceScope.launch {
            try {
                val record = com.example.slowdown.data.local.entity.InterventionRecord(
                    packageName = packageName,
                    appName = targetAppName,
                    timestamp = System.currentTimeMillis(),
                    interventionType = "app_launch",
                    userChoice = userChoice,
                    countdownDuration = countdownDuration,
                    actualWaitTime = actualWaitTime
                )
                repository.recordIntervention(record)
                Log.d(TAG, "[OverlayService] Intervention recorded: $userChoice, waited ${actualWaitTime}s")
            } catch (e: Exception) {
                Log.e(TAG, "[OverlayService] Failed to record intervention: ${e.message}")
            }
        }
        removeOverlay()
        stopSelf()
    }

    private fun removeOverlay() {
        countDownTimer?.cancel()
        countDownTimer = null

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "[OverlayService] Overlay view removed")
            } catch (e: Exception) {
                Log.e(TAG, "[OverlayService] Failed to remove overlay view: ${e.message}")
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "[OverlayService] onDestroy")
        removeOverlay()
        serviceScope.cancel()
    }
}
