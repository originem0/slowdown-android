package com.sharonZ.slowdown.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharonZ.slowdown.data.local.entity.InterventionRecord
import com.sharonZ.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayViewModel(
    private val repository: SlowDownRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SlowDown"
    }

    private val _countdown = MutableStateFlow(10)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _canContinue = MutableStateFlow(false)
    val canContinue: StateFlow<Boolean> = _canContinue.asStateFlow()

    private var countdownJob: Job? = null
    private var startTime: Long = 0
    private var packageName: String = ""
    private var appName: String = ""
    private var initialCountdown: Int = 10

    fun startCountdown(packageName: String, appName: String, seconds: Int) {
        this.packageName = packageName
        this.appName = appName
        this.initialCountdown = seconds
        this.startTime = System.currentTimeMillis()

        _countdown.value = seconds
        _canContinue.value = false

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = 0
            _canContinue.value = true
        }
    }

    /**
     * 检查服务是否仍然启用
     * 用于在执行用户操作前验证服务状态
     */
    suspend fun isServiceEnabled(): Boolean {
        return repository.serviceEnabled.first()
    }

    /**
     * 记录干预结果
     * 只有在服务启用时才记录，避免服务禁用后仍写入数据
     */
    fun recordAndFinish(userChoice: String) {
        viewModelScope.launch {
            // Bug fix #1: 检查服务是否启用，禁用时跳过记录
            val serviceEnabled = repository.serviceEnabled.first()
            if (!serviceEnabled) {
                Log.d(TAG, "[Overlay] Service disabled, skip recording intervention")
                return@launch
            }

            val actualWaitTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            // 边界值验证：actualWaitTime应该在合理范围内（1-300秒）
            val validatedWaitTime = actualWaitTime.coerceIn(1, 300)

            repository.recordIntervention(
                InterventionRecord(
                    packageName = packageName,
                    appName = appName,
                    timestamp = System.currentTimeMillis(),
                    interventionType = "countdown",
                    userChoice = userChoice,
                    countdownDuration = initialCountdown,
                    actualWaitTime = validatedWaitTime
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    class Factory(private val repository: SlowDownRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OverlayViewModel(repository) as T
        }
    }
}
