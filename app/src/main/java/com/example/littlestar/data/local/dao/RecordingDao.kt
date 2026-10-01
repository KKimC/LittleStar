package com.example.littlestar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.littlestar.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recording WHERE sheetMusicId = :sheetId ORDER BY isRepresentative DESC, createdAt DESC")
    fun observeBySheet(sheetId: Long): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recording WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("DELETE FROM recording WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recording SET isRepresentative = 0 WHERE sheetMusicId = :sheetId")
    suspend fun clearRepresentative(sheetId: Long)

    @Query("UPDATE recording SET isRepresentative = 1 WHERE id = :id")
    suspend fun setRepresentative(id: Long)

    /** 해당 곡의 대표 녹음을 [recordingId]로 교체한다(기존 대표는 해제). */
    @Transaction
    suspend fun setRepresentativeForSheet(sheetId: Long, recordingId: Long) {
        clearRepresentative(sheetId)
        setRepresentative(recordingId)
    }
}
