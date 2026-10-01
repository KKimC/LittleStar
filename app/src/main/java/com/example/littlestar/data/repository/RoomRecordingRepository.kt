package com.example.littlestar.data.repository

import com.example.littlestar.data.local.dao.RecordingDao
import com.example.littlestar.domain.model.Recording
import com.example.littlestar.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRecordingRepository(
    private val recordingDao: RecordingDao,
) : RecordingRepository {

    override fun observeRecordings(sheetId: Long): Flow<List<Recording>> =
        recordingDao.observeBySheet(sheetId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertRecording(recording: Recording): Long =
        recordingDao.insert(recording.toEntity())

    override suspend fun deleteRecording(recordingId: Long) {
        recordingDao.deleteById(recordingId)
    }

    override suspend fun getRecording(recordingId: Long): Recording? =
        recordingDao.getById(recordingId)?.toDomain()

    override suspend fun setRepresentativeRecording(sheetId: Long, recordingId: Long) {
        recordingDao.setRepresentativeForSheet(sheetId, recordingId)
    }

    override suspend fun clearRepresentativeRecording(sheetId: Long) {
        recordingDao.clearRepresentative(sheetId)
    }
}
