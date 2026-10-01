package com.example.littlestar.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 앱 설정(일일 연습 목표 등) 제공 계약.
 */
interface SettingsRepository {
    val dailyGoalMinutes: Flow<Int>
    suspend fun setDailyGoal(minutes: Int)
}
