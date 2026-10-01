package com.example.littlestar.domain.model

/**
 * 연습 세션 상태.
 *  - RUNNING    : 진행 중
 *  - COMPLETED  : 정상 종료
 *  - INTERRUPTED: 비정상 종료(앱 종료 등) 후 복구 처리된 상태
 */
enum class PracticeSessionStatus { RUNNING, COMPLETED, INTERRUPTED }

/**
 * 한 번의 연습 세션.
 *
 *  - [startedAt]        : wall-clock epoch millis (날짜/통계 표시용)
 *  - [startedRealtimeMs]: SystemClock.elapsedRealtime() 기반 monotonic 값 (정확한 duration 계산용)
 *  - [durationSeconds]  : 종료 시 계산해 저장. RUNNING 중에는 null.
 */
data class PracticeSession(
    val id: Long,
    val sheetMusicId: Long,
    val startedAt: Long,
    val startedRealtimeMs: Long,
    val endedAt: Long?,
    val durationSeconds: Int?,
    val dailyMemo: String?,
    val status: PracticeSessionStatus,
)
