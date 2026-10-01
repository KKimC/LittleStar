package com.example.littlestar.feature.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.littlestar.core.design.components.PianoCard
import com.example.littlestar.core.design.components.PianoTopBar
import com.example.littlestar.core.design.components.SectionTitle
import com.example.littlestar.core.design.components.StatCard
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.core.util.formatHoursMinutes
import com.example.littlestar.core.util.formatMinutes
import com.example.littlestar.core.util.isWideLayout
import com.example.littlestar.di.LocalAppContainer
import com.example.littlestar.domain.model.DashboardStats
import com.example.littlestar.domain.model.DayAttendance
import com.example.littlestar.domain.model.PracticeOnDay
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.feature.library.SheetList
import java.time.LocalDate
import java.time.YearMonth

/**
 * 메인 대시보드 화면.
 * 가로(넓은) 화면에서는 통계와 악보 보관함을 나란히, 세로 화면에서는 위아래로 배치한다.
 */
@Composable
fun DashboardScreen(
    onOpenSettings: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenSheet: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(container.getDashboardStatsUseCase, container.getSheetsUseCase, container.sheetRepository, container.deleteDirectoryUseCase)
    }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PianoTopBar(title = "LittleStar", onSettings = onOpenSettings)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenScanner,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                text = { Text("악보 스캔") },
            )
        },
    ) { innerPadding ->
        DashboardContent(
            uiState = uiState,
            onSelectDirectory = viewModel::selectDirectory,
            onAddDirectory = viewModel::addDirectory,
            onRenameDirectory = viewModel::renameDirectory,
            onDeleteDirectory = viewModel::deleteDirectory,
            onSetSortOrder = viewModel::setSortOrder,
            onOpenSheet = onOpenSheet,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onSelectDirectory: (Long) -> Unit,
    onAddDirectory: (String) -> Unit,
    onRenameDirectory: (Long, String) -> Unit,
    onDeleteDirectory: (Long) -> Unit,
    onSetSortOrder: (SheetSortOrder) -> Unit,
    onOpenSheet: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = isWideLayout()
    val stats = uiState.stats ?: return

    var selectedDateEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedDate = selectedDateEpoch?.let { LocalDate.ofEpochDay(it) }

    if (wide) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                StatsSection(
                    stats = stats,
                    recentSheet = stats.recentSheet,
                    displayedMonth = uiState.displayedMonth,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDateEpoch = it.toEpochDay() },
                    onOpenSheet = onOpenSheet,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                LibrarySection(uiState, onSelectDirectory, onAddDirectory, onRenameDirectory, onDeleteDirectory, onSetSortOrder, onOpenSheet)
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            StatsSection(
                stats = stats,
                recentSheet = stats.recentSheet,
                displayedMonth = uiState.displayedMonth,
                selectedDate = selectedDate,
                onDateSelected = { selectedDateEpoch = it.toEpochDay() },
                onOpenSheet = onOpenSheet,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
            LibrarySection(uiState, onSelectDirectory, onAddDirectory, onRenameDirectory, onDeleteDirectory, onSetSortOrder, onOpenSheet)
        }
    }
}

@Composable
private fun StatsSection(
    stats: DashboardStats,
    recentSheet: SheetMusic?,
    displayedMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onOpenSheet: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    TodayPracticeCard(stats)
    StatCard(
        label = "이번 주 누적 연습 시간",
        value = formatMinutes(stats.weeklyPracticeMinutes),
        icon = Icons.Filled.Timer,
    )
    AttendanceCard(stats.weeklyAttendance)
    MonthlyCalendar(
        month = displayedMonth,
        practiceByDay = stats.practiceByDay,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
    )
    recentSheet?.let { ContinueCard(it, onOpenSheet) }
}

@Composable
private fun TodayPracticeCard(stats: DashboardStats) {
    PianoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "오늘의 연습 시간",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.SmallSpacing))
                Text(
                    text = formatMinutes(stats.todayPracticeMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))

        LinearProgressIndicator(
            progress = { stats.goalProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )

        Spacer(Modifier.height(Dimens.SmallSpacing))

        Text(
            text = "목표 달성률 ${(stats.goalProgress * 100).toInt()}% · 목표 ${formatMinutes(stats.goalMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AttendanceCard(attendance: List<DayAttendance>) {
    PianoCard {
        SectionTitle("이번 주 출석")
        Spacer(Modifier.height(Dimens.ItemSpacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            attendance.forEach { day ->
                DayMark(day)
            }
        }
    }
}

@Composable
private fun DayMark(day: DayAttendance) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (day.practiced) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .then(
                    if (day.isToday) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (day.practiced) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(Dimens.TinySpacing))
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (day.isToday) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthlyCalendar(
    month: YearMonth,
    practiceByDay: Map<LocalDate, List<PracticeOnDay>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    PianoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("월간 출석", modifier = Modifier.weight(1f))
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${month.year}년 ${month.monthValue}월",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))

        val weekdays = listOf("월", "화", "수", "목", "금", "토", "일")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SmallSpacing))

        val today = LocalDate.now()
        val firstDay = month.atDay(1)
        // 월요일 시작 기준 빈 칸 수 (Monday=1 → 0)
        val leadingBlanks = firstDay.dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val total = leadingBlanks + daysInMonth
        val paddedTotal = ((total + 6) / 7) * 7
        val cells: List<Int?> =
            List(leadingBlanks) { null } + (1..daysInMonth).map { it } + List(paddedTotal - total) { null }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).height(CalendarCellHeight))
                    } else {
                        val date = month.atDay(day)
                        val items = practiceByDay[date] ?: emptyList()
                        CalendarDayCell(
                            day = day,
                            practiced = items.isNotEmpty(),
                            durationSeconds = items.sumOf { it.totalSeconds },
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (selectedDate != null) {
            Spacer(Modifier.height(Dimens.ItemSpacing))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Dimens.ItemSpacing))

            Text(
                text = "${selectedDate.monthValue}월 ${selectedDate.dayOfMonth}일 연습",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.SmallSpacing))

            val items = practiceByDay[selectedDate] ?: emptyList()
            if (items.isEmpty()) {
                Text(
                    text = "이 날은 연습 기록이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(Dimens.SmallSpacing))
                        Text(
                            text = formatHoursMinutes(item.totalSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(Dimens.TinySpacing))
                }
            }
        }
    }
}

private val CalendarCellHeight = 46.dp

@Composable
private fun CalendarDayCell(
    day: Int,
    practiced: Boolean,
    durationSeconds: Long,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(CalendarCellHeight)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (practiced) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
                )
                .then(
                    when {
                        isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.labelMedium,
                color = if (practiced) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (practiced) formatHoursMinutes(durationSeconds) else " ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ContinueCard(sheet: SheetMusic, onOpenSheet: (Long) -> Unit) {
    PianoCard(
        modifier = Modifier.clickable { onOpenSheet(sheet.id) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(Dimens.ItemSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "이어서 연습하기",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sheet.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibrarySection(
    uiState: DashboardUiState,
    onSelectDirectory: (Long) -> Unit,
    onAddDirectory: (String) -> Unit,
    onRenameDirectory: (Long, String) -> Unit,
    onDeleteDirectory: (Long) -> Unit,
    onSetSortOrder: (SheetSortOrder) -> Unit,
    onOpenSheet: (Long) -> Unit,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var manageTarget by remember { mutableStateOf<SheetDirectory?>(null) }
    var renameTarget by remember { mutableStateOf<SheetDirectory?>(null) }
    var deleteTarget by remember { mutableStateOf<SheetDirectory?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle("악보 보관함", modifier = Modifier.weight(1f))
        TextButton(onClick = { onSetSortOrder(SheetSortOrder.NAME) }) {
            Text(
                text = "이름순",
                color = if (uiState.sortOrder == SheetSortOrder.NAME) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        TextButton(onClick = { onSetSortOrder(SheetSortOrder.LATEST) }) {
            Text(
                text = "최신순",
                color = if (uiState.sortOrder == SheetSortOrder.LATEST) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    Spacer(Modifier.height(Dimens.SmallSpacing))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        items(uiState.directories, key = { it.id }) { directory ->
            FilterChip(
                selected = directory.id == uiState.selectedDirectoryId,
                onClick = { onSelectDirectory(directory.id) },
                label = { Text(directory.name) },
                modifier = Modifier.combinedClickable(
                    onClick = { onSelectDirectory(directory.id) },
                    onLongClick = { manageTarget = directory },
                ),
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = { showAddDialog = true },
                label = { Text("+ 디렉토리") },
            )
        }
    }

    Spacer(Modifier.height(Dimens.SmallSpacing))

    SheetList(sheets = uiState.sheets, onSheetClick = onOpenSheet)

    if (showAddDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 디렉토리") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("디렉토리 이름") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onAddDirectory(name)
                        showAddDialog = false
                    }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            },
        )
    }

    manageTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { manageTarget = null },
            title = { Text(target.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        renameTarget = target
                        manageTarget = null
                    }) { Text("이름 변경") }
                    TextButton(onClick = {
                        deleteTarget = target
                        manageTarget = null
                    }) { Text("삭제") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { manageTarget = null }) { Text("닫기") }
            },
        )
    }

    renameTarget?.let { target ->
        var name by rememberSaveable { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("이름 변경") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("디렉토리 이름") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onRenameDirectory(target.id, name)
                        renameTarget = null
                    }
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("취소") }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("디렉토리 삭제") },
            text = { Text("이 디렉토리의 모든 악보와 기록이 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDirectory(target.id)
                    deleteTarget = null
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("취소") }
            },
        )
    }
}
