package com.example.littlestar.domain.repository

import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.SheetPracticeTotal
import kotlinx.coroutines.flow.Flow

/**
 * 연습 세션 제공 계약.
 */
interface PracticeRepository {
    fun observeSessions(sheetId: Long): Flow<List<PracticeSession>>
    fun observeAllSessions(): Flow<List<PracticeSession>>
    fun observeTotalsBySheet(): Flow<List<SheetPracticeTotal>>

    /** 연습 세션을 새로 시작(RUNNING)하고 id를 반환한다. */
    suspend fun insertSession(session: PracticeSession): Long

    /** 세션을 갱신한다(종료 시 endedAt/durationSeconds/status 저장). */
    suspend fun updateSession(session: PracticeSession)

    /** 연습 기록(세션) 하나를 삭제한다. */
    suspend fun deleteSession(sessionId: Long)

    /** 비정상 종료로 남은 RUNNING 세션을 INTERRUPTED로 정리한다. */
    suspend fun interruptRunningSessions()
}
