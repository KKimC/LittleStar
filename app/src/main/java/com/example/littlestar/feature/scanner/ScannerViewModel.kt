package com.example.littlestar.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.usecase.ReplaceSheetPagesUseCase
import com.example.littlestar.domain.usecase.SaveScannedSheetUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val pages: List<ByteArray> = emptyList(),
    val title: String = "",
    val composer: String = "",
    val isSaving: Boolean = false,
    val savedSheetId: Long? = null,
    val directories: List<SheetDirectory> = emptyList(),
    val selectedDirectoryId: Long? = null,
)

class ScannerViewModel(
    private val saveScannedSheetUseCase: SaveScannedSheetUseCase,
    private val replaceSheetPagesUseCase: ReplaceSheetPagesUseCase,
    directoriesFlow: Flow<List<SheetDirectory>>,
    private val replaceSheetId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val isReplaceMode: Boolean get() = replaceSheetId != null

    init {
        viewModelScope.launch {
            directoriesFlow.collect { dirs ->
                _uiState.update { state ->
                    val selected = state.selectedDirectoryId
                        ?.takeIf { id -> dirs.any { it.id == id } }
                        ?: dirs.firstOrNull()?.id
                    state.copy(directories = dirs, selectedDirectoryId = selected)
                }
            }
        }
    }

    fun setPages(pages: List<ByteArray>) { _uiState.update { it.copy(pages = pages) } }
    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateComposer(composer: String) { _uiState.update { it.copy(composer = composer) } }
    fun selectDirectory(id: Long) { _uiState.update { it.copy(selectedDirectoryId = id) } }

    fun save() {
        if (_uiState.value.isSaving || _uiState.value.pages.isEmpty()) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val targetId = replaceSheetId
            if (targetId == null) {
                val directoryId = _uiState.value.selectedDirectoryId ?: return@launch
                val newId = saveScannedSheetUseCase(
                    _uiState.value.title,
                    _uiState.value.composer.ifBlank { null },
                    directoryId,
                    _uiState.value.pages,
                )
                _uiState.update { it.copy(isSaving = false, savedSheetId = newId) }
            } else {
                replaceSheetPagesUseCase(targetId, _uiState.value.pages)
                _uiState.update { it.copy(isSaving = false, savedSheetId = targetId) }
            }
        }
    }

    fun consumeSaved() { _uiState.update { it.copy(savedSheetId = null) } }
}
