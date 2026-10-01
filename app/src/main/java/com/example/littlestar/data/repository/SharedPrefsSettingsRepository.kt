package com.example.littlestar.data.repository

import android.content.Context
import com.example.littlestar.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences 기반 설정 저장소.
 * 값이 변경되면 StateFlow를 통해 구독자에게 즉시 반영된다.
 */
class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _dailyGoalMinutes = MutableStateFlow(
        prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
    )
    override val dailyGoalMinutes: Flow<Int> = _dailyGoalMinutes.asStateFlow()

    override suspend fun setDailyGoal(minutes: Int) {
        val clamped = minutes.coerceIn(5, 300)
        _dailyGoalMinutes.value = clamped
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
    }

    companion object {
        private const val PREFS_NAME = "pianoflow_settings"
        private const val KEY_DAILY_GOAL = "daily_goal_minutes"
        const val DEFAULT_DAILY_GOAL = 60
    }
}
