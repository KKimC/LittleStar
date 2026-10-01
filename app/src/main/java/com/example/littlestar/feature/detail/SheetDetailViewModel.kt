package com.example.littlestar.feature.detail

import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.littlestar.data.audio.AudioPlayer
import com.example.littlestar.data.audio.AudioRecorder
import com.example.littlestar.data.storage.RecordingFileStorage
import com.example.littlestar.domain.model.Recording
import com.example.littlestar.domain.model.SheetDetail
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.RecordingRepository
import com.example.littlestar.domain.repository.SheetRepository
import com.example.littlestar.domain.usecase.DeleteSheetUseCase
import com.example.littlestar.domain.usecase.GetSheetDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SheetDetailUiState(val detail: SheetDetail? = null)

/**
 * 녹음 UI 상태.
 *  - [isRecording]: 녹음 진행 중 여부
 *  - [recordingElapsedSeconds]: 녹음 진행 중 표시용 경과 시간
 *  - [playingRecordingId]: 현재 재생 중인 녹음 id (없으면 null)
 */
data class RecordingUiState(
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Long = 0L,
    val playingRecordingId: Long? = null,
)

class SheetDetailViewModel(
    private val sheetId: Long,
    getSheetDetail: GetSheetDetailUseCase,
    private val sheetRepository: SheetRepository,
    private val practiceRepository: PracticeRepository,
    private val deleteSheetUseCase: DeleteSheetUseCase,
    private val recordingRepository: RecordingRepository,
    private val recordingFileStorage: RecordingFileStorage,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    val uiState: StateFlow<SheetDetailUiState> = getSheetDetail(sheetId)
        .map { SheetDetailUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SheetDetailUiState())

    val directories: StateFlow<List<SheetDirectory>> = sheetRepository.observeDirectories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recordings: StateFlow<List<Recording>> = recordingRepository.observeRecordings(sheetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recordingUiState = MutableStateFlow(RecordingUiState())
    val recordingUiState: StateFlow<RecordingUiState> = _recordingUiState.asStateFlow()

    private var recordingTickerJob: Job? = null
    private var currentRecordingPath: String? = null

    fun saveLessonNote(note: String) {
        viewModelScope.launch { sheetRepository.updateLessonNote(sheetId, note.ifBlank { null }) }
    }

    fun moveSheet(directoryId: Long) {
        viewModelScope.launch { sheetRepository.moveSheet(sheetId, directoryId) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch { practiceRepository.deleteSession(sessionId) }
    }

    suspend fun deleteSheet() {
        deleteSheetUseCase(sheetId)
    }

    fun startRecording() {
        if (_recordingUiState.value.isRecording) return

        val path = recordingFileStorage.newRelativePath(sheetId)
        val file = recordingFileStorage.prepareFile(path)
        runCatching { audioRecorder.start(file) }
            .onSuccess {
                currentRecordingPath = path
                val startRealtimeMs = SystemClock.elapsedRealtime()
                _recordingUiState.update { it.copy(isRecording = true, recordingElapsedSeconds = 0L) }
                recordingTickerJob = viewModelScope.launch {
                    while (isActive) {
                        delay(1_000)
                        val elapsed = (SystemClock.elapsedRealtime() - startRealtimeMs) / 1000
                        _recordingUiState.update { it.copy(recordingElapsedSeconds = elapsed) }
                    }
                }
            }
            .onFailure {
                currentRecordingPath = null
                file.delete()
            }
    }

    fun stopRecording() {
        if (!_recordingUiState.value.isRecording) return
        recordingTickerJob?.cancel()
        recordingTickerJob = null

        val durationSeconds = audioRecorder.stop()
        val path = currentRecordingPath
        currentRecordingPath = null
        _recordingUiState.update { it.copy(isRecording = false, recordingElapsedSeconds = 0L) }

        if (path == null) return
        if (durationSeconds <= 0) {
            viewModelScope.launch { recordingFileStorage.delete(path) }
            return
        }
        viewModelScope.launch {
            recordingRepository.insertRecording(
                Recording(
                    id = 0,
                    sheetMusicId = sheetId,
                    fileName = path,
                    durationSeconds = durationSeconds,
                    createdAt = System.currentTimeMillis(),
                    isRepresentative = false,
                )
            )
        }
    }

    fun togglePlayback(recording: Recording) {
        val current = _recordingUiState.value.playingRecordingId
        if (current == recording.id) {
            audioPlayer.stop()
            _recordingUiState.update { it.copy(playingRecordingId = null) }
            return
        }

        audioPlayer.stop()
        val file = recordingFileStorage.toFile(recording.fileName)
        if (!file.exists()) return
        _recordingUiState.update { it.copy(playingRecordingId = recording.id) }
        audioPlayer.play(file) {
            _recordingUiState.update { it.copy(playingRecordingId = null) }
        }
    }

    fun deleteRecording(recording: Recording) {
        if (_recordingUiState.value.playingRecordingId == recording.id) {
            audioPlayer.stop()
            _recordingUiState.update { it.copy(playingRecordingId = null) }
        }
        viewModelScope.launch {
            recordingRepository.deleteRecording(recording.id)
            recordingFileStorage.delete(recording.fileName)
        }
    }

    fun setRepresentativeRecording(recording: Recording) {
        viewModelScope.launch {
            recordingRepository.setRepresentativeRecording(sheetId, recording.id)
        }
    }

    fun toggleRepresentativeRecording(recording: Recording) {
        viewModelScope.launch {
            if (recording.isRepresentative) {
                recordingRepository.clearRepresentativeRecording(sheetId)
            } else {
                recordingRepository.setRepresentativeRecording(sheetId, recording.id)
            }
        }
    }

    fun exportRecording(recording: Recording, targetUri: Uri) {
        viewModelScope.launch {
            recordingFileStorage.exportRecording(recording.fileName, targetUri)
        }
    }

    fun importRecording(sourceUri: Uri) {
        viewModelScope.launch {
            val imported = recordingFileStorage.importRecording(sheetId, sourceUri) ?: return@launch
            recordingRepository.insertRecording(
                Recording(
                    id = 0,
                    sheetMusicId = sheetId,
                    fileName = imported.fileName,
                    durationSeconds = imported.durationSeconds,
                    createdAt = System.currentTimeMillis(),
                    isRepresentative = false,
                )
            )
        }
    }

    override fun onCleared() {
        if (_recordingUiState.value.isRecording) {
            audioRecorder.cancel()
            currentRecordingPath?.let { recordingFileStorage.toFile(it).delete() }
        }
        audioPlayer.stop()
        super.onCleared()
    }
}
