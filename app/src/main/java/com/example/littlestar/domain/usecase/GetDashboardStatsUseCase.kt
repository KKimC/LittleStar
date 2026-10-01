package com.example.littlestar.domain.usecase

import com.example.littlestar.core.util.koreanLabel
import com.example.littlestar.domain.model.DashboardStats
import com.example.littlestar.domain.model.DayAttendance
import com.example.littlestar.domain.model.PracticeOnDay
import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.PracticeSessionStatus
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.SettingsRepository
import com.example.littlestar.domain.repository.SheetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 대시보드 연습 통계 조회 유스케이스.
 * "이번 주"는 월요일 시작 기준으로 계산하고, 목표는 설정에서 가져온다.
 */
class GetDashboardStatsUseCase(
    private val sheetRepository: SheetRepository,
    private val practiceRepository: PracticeRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<DashboardStats> = combine(
        practiceRepository.observeAllSessions(),
        sheetRepository.observeAllSheets(),
        settingsRepository.dailyGoalMinutes,
    ) { sessions, sheets, goal -> compute(sessions, sheets, goal) }

    private fun compute(
        sessions: List<PracticeSession>,
        sheets: List<SheetMusic>,
        goalMinutes: Int,
    ): DashboardStats {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        val completed = sessions.filter {
            it.status == PracticeSessionStatus.COMPLETED && it.durationSeconds != null
        }

        fun dateOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

        val todaySeconds = completed.filter { dateOf(it.startedAt) == today }.sumOf { it.durationSeconds!! }
        val weekSeconds = completed.filter {
            val d = dateOf(it.startedAt)
            !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
        }.sumOf { it.durationSeconds!! }

        val attendance = (0L..6L).map { offset ->
            val day = startOfWeek.plusDays(offset)
            DayAttendance(
                dayLabel = day.dayOfWeek.koreanLabel(),
                practiced = completed.any { dateOf(it.startedAt) == day },
                isToday = day == today,
            )
        }

        val recentSheet = completed.maxByOrNull { it.startedAt }
            ?.let { latest -> sheets.firstOrNull { it.id == latest.sheetMusicId } }

        val practiceByDay = completed
            .groupBy { dateOf(it.startedAt) }
            .mapValues { (_, daySessions) ->
                daySessions.groupBy { it.sheetMusicId }
                    .map { (sheetId, sheetSessions) ->
                        PracticeOnDay(
                            sheetId = sheetId,
                            title = sheets.firstOrNull { it.id == sheetId }?.title ?: "알 수 없는 곡",
                            totalSeconds = sheetSessions.sumOf { it.durationSeconds!!.toLong() },
                        )
                    }
                    .sortedByDescending { it.totalSeconds }
            }

        return DashboardStats(
            todayPracticeMinutes = (todaySeconds / 60).toInt(),
            goalMinutes = goalMinutes,
            weeklyPracticeMinutes = (weekSeconds / 60).toInt(),
            weeklyAttendance = attendance,
            recentSheet = recentSheet,
            practiceByDay = practiceByDay,
        )
    }
}
