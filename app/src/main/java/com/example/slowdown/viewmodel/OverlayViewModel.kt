package com.example.slowdown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.slowdown.data.local.entity.InterventionRecord
import com.example.slowdown.data.repository.SlowDownRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayViewModel(
    private val repository: SlowDownRepository
) : ViewModel() {

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

    fun recordAndFinish(userChoice: String) {
        viewModelScope.launch {
            val actualWaitTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            repository.recordIntervention(
                InterventionRecord(
                    packageName = packageName,
                    appName = appName,
                    timestamp = System.currentTimeMillis(),
                    interventionType = "countdown",
                    userChoice = userChoice,
                    countdownDuration = initialCountdown,
                    actualWaitTime = actualWaitTime
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
