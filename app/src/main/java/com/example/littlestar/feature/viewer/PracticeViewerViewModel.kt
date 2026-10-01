package com.example.littlestar.feature.viewer

import android.os.SystemClock
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.littlestar.data.audio.AudioRecorder
import com.example.littlestar.data.storage.RecordingFileStorage
import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.PracticeSessionStatus
import com.example.littlestar.domain.model.Recording
import com.example.littlestar.domain.model.SheetPage
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.RecordingRepository
import com.example.littlestar.domain.usecase.GetSheetDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PracticeViewerUiState(
    val title: String = "",
    val pageCount: Int = 1,
    val pages: List<SheetPage> = emptyList(),
    val lessonNote: String? = null,
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val memoText: String = "",
    val inkEnabled: Boolean = false,
    val inkBrushType: InkBrushType = InkBrushType.PEN,
    val inkColor: Color = Color(0xFF303B32),
    val inkSize: Float = 4f,
)

/**
 * 연습 뷰어의 녹음 UI 상태.
 */
data class ViewerRecordingUiState(
    val isRecording: Boolean = false,
    val recordingElapsedSeconds: Long = 0L,
)

class PracticeViewerViewModel(
    private val sheetId: Long,
    getSheetDetail: GetSheetDetailUseCase,
    private val practiceRepository: PracticeRepository,
    private val recordingRepository: RecordingRepository,
    private val recordingFileStorage: RecordingFileStorage,
    private val audioRecorder: AudioRecorder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeViewerUiState())
    val uiState: StateFlow<PracticeViewerUiState> = _uiState.asStateFlow()

    private val _recordingUiState = MutableStateFlow(ViewerRecordingUiState())
    val recordingUiState: StateFlow<ViewerRecordingUiState> = _recordingUiState.asStateFlow()

    private var sessionId: Long? = null
    private var startedAt: Long = 0L
    private var startedRealtimeMs: Long = 0L
    private var tickerJob: Job? = null
    private var recordingTickerJob: Job? = null
    private var currentRecordingPath: String? = null

    init {
        viewModelScope.launch {
            getSheetDetail(sheetId).collect { detail ->
                _uiState.update {
                    it.copy(
                        title = detail?.sheet?.title ?: "",
                        pageCount = (detail?.pages?.size ?: 0).coerceAtLeast(1),
                        pages = detail?.pages ?: emptyList(),
                        lessonNote = detail?.sheet?.lessonNote,
                    )
                }
            }
        }
    }

    fun startPractice() {
        if (_uiState.value.isRunning) return
        startedRealtimeMs = SystemClock.elapsedRealtime()
        startedAt = System.currentTimeMillis()
        _uiState.update { it.copy(isRunning = true, elapsedSeconds = 0L) }
        viewModelScope.launch {
            sessionId = practiceRepository.insertSession(
                PracticeSession(
                    id = 0,
                    sheetMusicId = sheetId,
                    startedAt = startedAt,
                    startedRealtimeMs = startedRealtimeMs,
                    endedAt = null,
                    durationSeconds = null,
                    dailyMemo = null,
                    status = PracticeSessionStatus.RUNNING,
                )
            )
        }
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (SystemClock.elapsedRealtime() - startedRealtimeMs) / 1000
                _uiState.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
    }

    suspend fun stopPractice() {
        if (!_uiState.value.isRunning) return
        tickerJob?.cancel()
        tickerJob = null
        val durationSeconds = ((SystemClock.elapsedRealtime() - startedRealtimeMs) / 1000).toInt()
        val memo = _uiState.value.memoText
        _uiState.update { it.copy(isRunning = false) }
        val id = sessionId ?: return
        practiceRepository.updateSession(
            PracticeSession(
                id = id,
                sheetMusicId = sheetId,
                startedAt = startedAt,
                startedRealtimeMs = startedRealtimeMs,
                endedAt = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                dailyMemo = memo.ifBlank { null },
                status = PracticeSessionStatus.COMPLETED,
            )
        )
    }

    fun updateMemo(text: String) {
        _uiState.update { it.copy(memoText = text) }
    }

    fun toggleInk() {
        _uiState.update { it.copy(inkEnabled = !it.inkEnabled) }
    }

    fun setInkBrushType(type: InkBrushType) {
        _uiState.update { it.copy(inkBrushType = type) }
    }

    fun setInkColor(color: Color) {
        _uiState.update { it.copy(inkColor = color) }
    }

    fun setInkSize(size: Float) {
        _uiState.update { it.copy(inkSize = size.coerceIn(INK_MIN_SIZE, INK_MAX_SIZE)) }
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

    override fun onCleared() {
        if (_recordingUiState.value.isRecording) {
            audioRecorder.cancel()
            currentRecordingPath?.let { recordingFileStorage.toFile(it).delete() }
        }
        super.onCleared()
    }
}
