package com.example.littlestar.feature.viewer

import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.littlestar.core.design.components.PianoCard
import com.example.littlestar.core.design.components.PianoTopBar
import com.example.littlestar.core.design.components.ZoomableBox
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.core.util.formatDuration
import com.example.littlestar.data.audio.AudioRecorder
import com.example.littlestar.data.storage.InkStorage
import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.di.LocalAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 연습 뷰어 화면.
 * 악보를 한 페이지씩 좌우로 넘기며, 시작 버튼을 누르면 연습 시간을 측정한다.
 * 연습이 끝나면 실제 연습 세션으로 저장된다.
 * 악보 이미지는 아직 없으므로 플레이스홀더를 표시한다.
 */
@Composable
fun PracticeViewerScreen(
    sheetId: Long,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: PracticeViewerViewModel = viewModel {
        PracticeViewerViewModel(
            sheetId,
            container.getSheetDetailUseCase,
            container.practiceRepository,
            container.recordingRepository,
            container.recordingFileStorage,
            AudioRecorder(context.applicationContext),
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val recordingUiState by viewModel.recordingUiState.collectAsState()

    // 가로 화면에서는 두 장을 나란히 표시한다(필기 비활성).
    val twoPageMode = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val spreadCount = if (twoPageMode) (uiState.pageCount + 1) / 2 else uiState.pageCount
    val effectiveInkEnabled = uiState.inkEnabled && !twoPageMode

    val pagerState = rememberPagerState(pageCount = { spreadCount })
    val scope = rememberCoroutineScope()
    var showMemoDialog by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showLessonNote by rememberSaveable { mutableStateOf(false) }
    var inkClearSignal by rememberSaveable { mutableIntStateOf(0) }
    var inkUndoSignal by rememberSaveable { mutableIntStateOf(0) }
    var controlsVisible by rememberSaveable { mutableStateOf(false) }

    // 컨트롤이 보이는 상태에서 연습/녹음/필기 중이 아니라면 일정 시간 뒤 자동으로 숨긴다.
    LaunchedEffect(controlsVisible, uiState.isRunning, recordingUiState.isRecording, effectiveInkEnabled) {
        if (controlsVisible && !uiState.isRunning && !recordingUiState.isRecording && !effectiveInkEnabled) {
            delay(4_000)
            controlsVisible = false
        }
    }

    fun goToSpread(index: Int) {
        if (index in 0 until spreadCount) {
            scope.launch { pagerState.animateScrollToPage(index) }
        }
    }

    // 시스템 바는 MainActivity에서 앱 전체에 대해 숨김 처리한다.
    // 여기서는 화면을 벗어날 때 복원만 담당한다.

    fun requestExit() {
        if (uiState.isRunning) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(onBack = ::requestExit)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedVisibility(visible = controlsVisible) {
                PianoTopBar(title = uiState.title.ifBlank { "연습" }, onBack = ::requestExit)
            }
        },
    ) { innerPadding ->
        val immersive = !controlsVisible
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (immersive) 0.dp else Dimens.ScreenPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { spread ->
                if (twoPageMode) {
                    val leftPage = spread * 2
                    val rightPage = spread * 2 + 1
                    Row(modifier = Modifier.fillMaxSize()) {
                        SheetPageContent(
                            sheetId = sheetId,
                            filePath = uiState.pages.getOrNull(leftPage)?.filePath,
                            pageNumber = leftPage + 1,
                            storage = container.sheetFileStorage,
                            inkStorage = container.inkStorage,
                            inkEnabled = false,
                            inkBrushType = uiState.inkBrushType,
                            inkColor = uiState.inkColor,
                            inkSize = uiState.inkSize,
                            inkClearSignal = inkClearSignal,
                            inkUndoSignal = inkUndoSignal,
                            saveScope = container.appScope,
                            modifier = Modifier.weight(1f),
                            fullWidth = true,
                            immersive = immersive,
                            onPrevious = { goToSpread(spread - 1) },
                            onNext = { goToSpread(spread + 1) },
                            onToggleControls = { controlsVisible = !controlsVisible },
                        )
                        SheetPageContent(
                            sheetId = sheetId,
                            filePath = uiState.pages.getOrNull(rightPage)?.filePath,
                            pageNumber = rightPage + 1,
                            storage = container.sheetFileStorage,
                            inkStorage = container.inkStorage,
                            inkEnabled = false,
                            inkBrushType = uiState.inkBrushType,
                            inkColor = uiState.inkColor,
                            inkSize = uiState.inkSize,
                            inkClearSignal = inkClearSignal,
                            inkUndoSignal = inkUndoSignal,
                            saveScope = container.appScope,
                            modifier = Modifier.weight(1f),
                            fullWidth = true,
                            immersive = immersive,
                            onPrevious = { goToSpread(spread - 1) },
                            onNext = { goToSpread(spread + 1) },
                            onToggleControls = { controlsVisible = !controlsVisible },
                        )
                    }
                } else {
                    SheetPageContent(
                        sheetId = sheetId,
                        filePath = uiState.pages.getOrNull(spread)?.filePath,
                        pageNumber = spread + 1,
                        storage = container.sheetFileStorage,
                        inkStorage = container.inkStorage,
                        inkEnabled = effectiveInkEnabled,
                        inkBrushType = uiState.inkBrushType,
                        inkColor = uiState.inkColor,
                        inkSize = uiState.inkSize,
                        inkClearSignal = inkClearSignal,
                        inkUndoSignal = inkUndoSignal,
                        saveScope = container.appScope,
                        immersive = immersive,
                        onPrevious = { goToSpread(spread - 1) },
                        onNext = { goToSpread(spread + 1) },
                        onToggleControls = { controlsVisible = !controlsVisible },
                    )
                }
            }

            Spacer(Modifier.height(Dimens.SmallSpacing))

            AnimatedVisibility(visible = controlsVisible) {
                PageNavigator(
                    currentPage = pagerState.currentPage,
                    pageCount = spreadCount,
                    totalPages = uiState.pageCount,
                    twoPageMode = twoPageMode,
                    onPrevious = { goToSpread(pagerState.currentPage - 1) },
                    onNext = { goToSpread(pagerState.currentPage + 1) },
                )
            }

            AnimatedVisibility(visible = controlsVisible) {
                Column {
                    Spacer(Modifier.height(Dimens.ItemSpacing))

                    ViewerToolbar(
                        isRecording = recordingUiState.isRecording,
                        recordingElapsedSeconds = recordingUiState.recordingElapsedSeconds,
                        inkEnabled = effectiveInkEnabled,
                        showInk = !twoPageMode,
                        onToggleRecording = {
                            if (recordingUiState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                viewModel.startRecording()
                            }
                        },
                        onToggleInk = viewModel::toggleInk,
                        onUndo = { inkUndoSignal++ },
                        onClear = { inkClearSignal++ },
                        onShowLessonNote = { showLessonNote = true },
                    )

                    if (effectiveInkEnabled) {
                        Spacer(Modifier.height(Dimens.ItemSpacing))
                        InkPalette(
                            brushType = uiState.inkBrushType,
                            color = uiState.inkColor,
                            size = uiState.inkSize,
                            onBrushTypeChange = viewModel::setInkBrushType,
                            onColorChange = viewModel::setInkColor,
                            onSizeChange = viewModel::setInkSize,
                        )
                    }

                    Spacer(Modifier.height(Dimens.ItemSpacing))

                    PracticeControls(
                        isRunning = uiState.isRunning,
                        elapsedSeconds = uiState.elapsedSeconds,
                        onStart = viewModel::startPractice,
                        onStop = { scope.launch { viewModel.stopPractice() } },
                        onMemo = { showMemoDialog = true },
                    )
                }
            }

            Spacer(Modifier.height(Dimens.ScreenPadding))
        }
    }

    if (showMemoDialog) {
        MemoDialog(
            text = uiState.memoText,
            onTextChange = viewModel::updateMemo,
            onDismiss = { showMemoDialog = false },
        )
    }

    if (showLessonNote) {
        LessonNoteDialog(
            note = uiState.lessonNote,
            onDismiss = { showLessonNote = false },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("연습이 진행 중입니다") },
            text = { Text("연습을 종료하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.stopPractice()
                            showExitDialog = false
                            onBack()
                        }
                    },
                ) {
                    Text("연습 종료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("계속 연습")
                }
            },
        )
    }
}

/** 악보 한 페이지. 실제 이미지가 있으면 표시하고, 없으면 플레이스홀더를 보여준다. */
@Composable
private fun SheetPageContent(
    sheetId: Long,
    filePath: String?,
    pageNumber: Int,
    storage: SheetFileStorage,
    inkStorage: InkStorage,
    inkEnabled: Boolean,
    inkBrushType: InkBrushType,
    inkColor: androidx.compose.ui.graphics.Color,
    inkSize: Float,
    inkClearSignal: Int,
    inkUndoSignal: Int,
    saveScope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    immersive: Boolean = false,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleControls: () -> Unit,
) {
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(filePath) {
        bitmap = withContext(Dispatchers.IO) {
            filePath?.let { runCatching { BitmapFactory.decodeFile(storage.toFile(it).absolutePath) }.getOrNull() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(if (immersive) 0.dp else Dimens.SmallSpacing),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = if (immersive) 0.dp else Dimens.SmallSpacing)
                .then(if (fullWidth || immersive) Modifier else Modifier.width(600.dp))
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(if (immersive) 0.dp else 12.dp),
                )
                .border(
                    BorderStroke(
                        width = if (immersive) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    RoundedCornerShape(if (immersive) 0.dp else 12.dp),
                )
                // 필기 모드에서는 탭이 그리기로 동작해야 하므로, 필기 꺼짐 상태에서만
                // 왼쪽/오른쪽 탭으로 페이지를 넘기고 가운데 탭으로 컨트롤을 토글한다.
                .pointerInput(inkEnabled) {
                    if (!inkEnabled) {
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            when {
                                offset.x < width / 3f -> onPrevious()
                                offset.x > width * 2f / 3f -> onNext()
                                else -> onToggleControls()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val image = bitmap
            if (image != null) {
                ZoomableBox {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "악보 ${pageNumber}쪽",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (immersive) 0.dp else Dimens.SmallSpacing),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(Dimens.ItemSpacing))
                    Text(
                        text = "악보 플레이스홀더",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Dimens.TinySpacing))
                    Text(
                        text = "${pageNumber}쪽 · 악보 이미지가 없습니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (inkEnabled) {
                InkOverlay(
                    sheetId = sheetId,
                    pageOrder = pageNumber,
                    inkStorage = inkStorage,
                    brushType = inkBrushType,
                    color = inkColor,
                    size = inkSize,
                    clearSignal = inkClearSignal,
                    undoSignal = inkUndoSignal,
                    saveScope = saveScope,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun PageNavigator(
    currentPage: Int,
    pageCount: Int,
    totalPages: Int,
    twoPageMode: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val label = if (twoPageMode) {
        val start = currentPage * 2 + 1
        val end = (currentPage * 2 + 2).coerceAtMost(totalPages)
        if (start == end) "$start / $totalPages" else "$start-$end / $totalPages"
    } else {
        "${currentPage + 1} / $pageCount"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = currentPage > 0) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "이전 페이지",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onNext, enabled = currentPage < pageCount - 1) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "다음 페이지",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ViewerToolbar(
    isRecording: Boolean,
    recordingElapsedSeconds: Long,
    inkEnabled: Boolean,
    showInk: Boolean = true,
    onToggleRecording: () -> Unit,
    onToggleInk: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onShowLessonNote: () -> Unit,
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onToggleRecording()
    }

    fun requestRecord() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            onToggleRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
    ) {
        // 녹음 토글
        if (isRecording) {
            Text(
                text = formatDuration(recordingElapsedSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { requestRecord() }) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isRecording) "녹음 중지" else "녹음 시작",
                tint = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 레슨 노트 보기
        IconButton(onClick = onShowLessonNote) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = "레슨 노트 보기",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        // 필기 토글
        if (showInk) {
            IconButton(onClick = onToggleInk) {
                Icon(
                    imageVector = Icons.Filled.Brush,
                    contentDescription = "필기 켜기/끄기",
                    tint = if (inkEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (inkEnabled) {
                IconButton(onClick = onUndo) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "실행 취소",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "전체 지우기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeControls(
    isRunning: Boolean,
    elapsedSeconds: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onMemo: () -> Unit,
) {
    PianoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRunning) "진행 중" else "연습 시간",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.TinySpacing))
                Text(
                    text = formatDuration(elapsedSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (isRunning) {
                OutlinedButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(Dimens.SmallSpacing))
                    Text("종료")
                }
            } else {
                Button(onClick = onStart) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Dimens.SmallSpacing))
                    Text("시작")
                }
            }

            Spacer(Modifier.width(Dimens.SmallSpacing))

            IconButton(onClick = onMemo) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "메모",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InkPalette(
    brushType: InkBrushType,
    color: Color,
    size: Float,
    onBrushTypeChange: (InkBrushType) -> Unit,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkBrushType.entries.forEach { type ->
                FilterChip(
                    selected = brushType == type,
                    onClick = { onBrushTypeChange(type) },
                    label = { Text(type.label) },
                )
            }
        }

        Spacer(Modifier.height(Dimens.SmallSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InkPaletteColors.forEach { c ->
                val selected = c == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(c) },
                )
            }
        }

        Spacer(Modifier.height(Dimens.SmallSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "굵기",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = size,
                onValueChange = onSizeChange,
                valueRange = INK_MIN_SIZE..INK_MAX_SIZE,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MemoDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("연습 메모") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("메모를 입력하세요") },
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Composable
private fun LessonNoteDialog(
    note: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("레슨 노트") },
        text = {
            if (note.isNullOrBlank()) {
                Text(
                    text = "레슨 노트가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("확인") }
        },
    )
}
