package com.example.littlestar.data.repository

import com.example.littlestar.data.local.dao.PracticeSessionDao
import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.SheetPracticeTotal
import com.example.littlestar.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPracticeRepository(
    private val practiceSessionDao: PracticeSessionDao,
) : PracticeRepository {

    override fun observeSessions(sheetId: Long): Flow<List<PracticeSession>> =
        practiceSessionDao.observeBySheet(sheetId).map { list -> list.map { it.toDomain() } }

    override fun observeAllSessions(): Flow<List<PracticeSession>> =
        practiceSessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTotalsBySheet(): Flow<List<SheetPracticeTotal>> =
        practiceSessionDao.observeTotalsBySheet()

    override suspend fun insertSession(session: PracticeSession): Long =
        practiceSessionDao.insert(session.toEntity())

    override suspend fun updateSession(session: PracticeSession) =
        practiceSessionDao.update(session.toEntity())

    override suspend fun deleteSession(sessionId: Long) {
        practiceSessionDao.deleteById(sessionId)
    }

    override suspend fun interruptRunningSessions() {
        practiceSessionDao.interruptAllRunning()
    }
}
