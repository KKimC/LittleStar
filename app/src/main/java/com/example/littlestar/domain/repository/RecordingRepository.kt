package com.example.littlestar.domain.repository

import com.example.littlestar.domain.model.Recording
import kotlinx.coroutines.flow.Flow

/**
 * 녹음(Recording) 제공 계약.
 */
interface RecordingRepository {
    /** 특정 곡의 녹음 목록을 최신순으로 관찰한다. */
    fun observeRecordings(sheetId: Long): Flow<List<Recording>>

    /** 녹음 하나를 저장하고 id를 반환한다. */
    suspend fun insertRecording(recording: Recording): Long

    /** 녹음 하나를 삭제한다. */
    suspend fun deleteRecording(recordingId: Long)

    /** 녹음 하나를 조회한다. */
    suspend fun getRecording(recordingId: Long): Recording?

    /** 해당 곡의 대표 녹음을 지정한다. */
    suspend fun setRepresentativeRecording(sheetId: Long, recordingId: Long)

    /** 해당 곡의 대표 녹음 지정을 해제한다. */
    suspend fun clearRepresentativeRecording(sheetId: Long)
}
