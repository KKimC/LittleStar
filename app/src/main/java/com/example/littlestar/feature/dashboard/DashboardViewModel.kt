package com.example.littlestar.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.littlestar.domain.model.DashboardStats
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetWithPractice
import com.example.littlestar.domain.repository.SheetRepository
import com.example.littlestar.domain.usecase.DeleteDirectoryUseCase
import com.example.littlestar.domain.usecase.GetDashboardStatsUseCase
import com.example.littlestar.domain.usecase.GetSheetsUseCase
import com.example.littlestar.core.util.naturalCompare
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/** 악보 목록 정렬 기준 */
enum class SheetSortOrder { NAME, LATEST }

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val directories: List<SheetDirectory> = emptyList(),
    val selectedDirectoryId: Long? = null,
    val sheets: List<SheetWithPractice> = emptyList(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val sortOrder: SheetSortOrder = SheetSortOrder.NAME,
)

class DashboardViewModel(
    getDashboardStats: GetDashboardStatsUseCase,
    private val getSheets: GetSheetsUseCase,
    private val sheetRepository: SheetRepository,
    private val deleteDirectoryUseCase: DeleteDirectoryUseCase,
) : ViewModel() {

    private data class Selection(
        val directoryId: Long? = null,
        val month: YearMonth = YearMonth.now(),
        val sortOrder: SheetSortOrder = SheetSortOrder.NAME,
    )

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<DashboardUiState> = combine(
        getDashboardStats(),
        getSheets.directories(),
        getSheets.allSheets(),
        selection,
    ) { stats, directories, allSheets, sel ->
        val effectiveId = sel.directoryId ?: directories.firstOrNull()?.id
        val filtered = allSheets.filter { it.sheet.directoryId == effectiveId }
        val sheets = when (sel.sortOrder) {
            SheetSortOrder.NAME -> filtered.sortedWith(Comparator { a, b ->
                naturalCompare(a.sheet.title, b.sheet.title)
            })
            SheetSortOrder.LATEST -> filtered.sortedByDescending { it.sheet.createdAt }
        }
        DashboardUiState(
            stats = stats,
            directories = directories,
            selectedDirectoryId = effectiveId,
            sheets = sheets,
            displayedMonth = sel.month,
            sortOrder = sel.sortOrder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun selectDirectory(id: Long) {
        selection.update { it.copy(directoryId = id) }
    }

    fun addDirectory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = getSheets.addDirectory(trimmed)
            selection.update { it.copy(directoryId = id) }
        }
    }

    fun renameDirectory(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { sheetRepository.renameDirectory(id, trimmed) }
    }

    fun deleteDirectory(id: Long) {
        viewModelScope.launch {
            deleteDirectoryUseCase(id)
            if (selection.value.directoryId == id) {
                selection.update { it.copy(directoryId = null) }
            }
        }
    }

    fun setSortOrder(order: SheetSortOrder) {
        selection.update { it.copy(sortOrder = order) }
    }

    fun previousMonth() { selection.update { it.copy(month = it.month.minusMonths(1)) } }

    fun nextMonth() { selection.update { it.copy(month = it.month.plusMonths(1)) } }
}
