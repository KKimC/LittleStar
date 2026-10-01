package com.example.littlestar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.littlestar.data.local.entity.PracticeSessionEntity
import com.example.littlestar.domain.model.SheetPracticeTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_session WHERE sheetMusicId = :sheetId ORDER BY startedAt DESC")
    fun observeBySheet(sheetId: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT sheetMusicId, SUM(durationSeconds) AS totalSeconds FROM practice_session WHERE status = 'COMPLETED' GROUP BY sheetMusicId")
    fun observeTotalsBySheet(): Flow<List<SheetPracticeTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<PracticeSessionEntity>)

    @Insert
    suspend fun insert(session: PracticeSessionEntity): Long

    @Update
    suspend fun update(session: PracticeSessionEntity)

    @Query("DELETE FROM practice_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE practice_session SET status = 'INTERRUPTED' WHERE status = 'RUNNING'")
    suspend fun interruptAllRunning(): Int
}
