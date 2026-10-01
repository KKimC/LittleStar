package com.example.littlestar.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.littlestar.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정 화면 UI 상태 */
data class SettingsUiState(
    val dailyGoalMinutes: Int = 60,
)

/**
 * 설정 화면 상태를 관리하는 ViewModel.
 * 일일 연습 목표는 SettingsRepository에 영구 보관된다.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.dailyGoalMinutes
        .map { SettingsUiState(dailyGoalMinutes = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDailyGoal(minutes) }
    }
}
