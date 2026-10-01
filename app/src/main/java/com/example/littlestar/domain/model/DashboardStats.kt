package com.example.littlestar.domain.model

import java.time.LocalDate

/** 월요일부터 일요일까지의 하루 출석 여부 */
data class DayAttendance(
    val dayLabel: String,
    val practiced: Boolean,
    val isToday: Boolean = false,
)

/** 대시보드에 표시할 연습 통계 */
data class DashboardStats(
    val todayPracticeMinutes: Int,
    val goalMinutes: Int,
    val weeklyPracticeMinutes: Int,
    val weeklyAttendance: List<DayAttendance>,
    /** 이어서 연습하기에 표시할 최근 연습 곡 (없으면 null) */
    val recentSheet: SheetMusic?,
    /** 날짜별 연습 내역(곡 단위로 합산) — 월간 캘린더/일별 상세용 */
    val practiceByDay: Map<LocalDate, List<PracticeOnDay>> = emptyMap(),
) {
    /** 목표 달성률 (0f ~ 1f) */
    val goalProgress: Float
        get() = if (goalMinutes == 0) 0f else (todayPracticeMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)
}

/** 하루 동안 연습한 한 곡의 합산 결과 */
data class PracticeOnDay(
    val sheetId: Long,
    val title: String,
    val totalSeconds: Long,
)
