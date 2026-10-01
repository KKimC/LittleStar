package com.example.littlestar.feature.detail

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.littlestar.core.design.components.PianoCard
import com.example.littlestar.core.design.components.PianoTopBar
import com.example.littlestar.core.design.components.SectionTitle
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.core.util.formatDate
import com.example.littlestar.core.util.formatDuration
import com.example.littlestar.core.util.formatMinutes
import com.example.littlestar.core.util.formatTime
import com.example.littlestar.core.util.formatTimeRange
import com.example.littlestar.data.audio.AudioPlayer
import com.example.littlestar.data.audio.AudioRecorder
import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.di.LocalAppContainer
import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.Recording
import com.example.littlestar.domain.model.SheetDetail
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.model.SheetPage
import kotlinx.coroutines.launch

/**
 * 악보 상세 화면.
 * 곡 정보, 악보 페이지, 레슨 노트, 연습 기록을 보여주고 연습을 시작할 수 있다.
 */
@Composable
fun SheetDetailScreen(
    sheetId: Long,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    onRescanPages: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: SheetDetailViewModel = viewModel {
        SheetDetailViewModel(
            sheetId,
            container.getSheetDetailUseCase,
            container.sheetRepository,
            container.practiceRepository,
            container.deleteSheetUseCase,
            container.recordingRepository,
            container.recordingFileStorage,
            AudioRecorder(context.applicationContext),
            AudioPlayer(),
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val directories by viewModel.directories.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val recordingUiState by viewModel.recordingUiState.collectAsState()
    val detail = uiState.detail

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PianoTopBar(title = detail?.sheet?.title ?: "악보 상세", onBack = onBack)
        },
    ) { innerPadding ->
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(Dimens.ScreenPadding),
            ) {
                Text("악보 정보를 찾을 수 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            DetailContent(
                detail = detail,
                directories = directories,
                recordings = recordings,
                recordingUiState = recordingUiState,
                onBack = onBack,
                onStartPractice = onStartPractice,
                onRescanPages = onRescanPages,
                onSaveNote = viewModel::saveLessonNote,
                onMoveSheet = viewModel::moveSheet,
                onDeleteSheet = viewModel::deleteSheet,
                onDeleteSession = viewModel::deleteSession,
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecording,
                onTogglePlayback = viewModel::togglePlayback,
                onDeleteRecording = viewModel::deleteRecording,
                onSetRepresentative = viewModel::toggleRepresentativeRecording,
                onExportRecording = viewModel::exportRecording,
                onImportRecording = viewModel::importRecording,
                sheetFileStorage = container.sheetFileStorage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: SheetDetail,
    directories: List<SheetDirectory>,
    recordings: List<Recording>,
    recordingUiState: RecordingUiState,
    onBack: () -> Unit,
    onStartPractice: () -> Unit,
    onRescanPages: () -> Unit,
    onSaveNote: (String) -> Unit,
    onMoveSheet: (Long) -> Unit,
    onDeleteSheet: suspend () -> Unit,
    onDeleteSession: (Long) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayback: (Recording) -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onSetRepresentative: (Recording) -> Unit,
    onExportRecording: (Recording, Uri) -> Unit,
    onImportRecording: (Uri) -> Unit,
    sheetFileStorage: SheetFileStorage,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var showMoveDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        SheetHeader(detail.sheet, pageCount = detail.pages.size, onRescanPages = onRescanPages)
        SectionPages(detail.pages, sheetFileStorage)
        SectionNote(detail.sheet.lessonNote, onSaveNote)
        SectionRecording(
            recordings = recordings,
            uiState = recordingUiState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onTogglePlayback = onTogglePlayback,
            onDeleteRecording = onDeleteRecording,
            onSetRepresentative = onSetRepresentative,
            onExportRecording = onExportRecording,
            onImportRecording = onImportRecording,
        )
        SectionRecords(detail.practiceSessions, onDeleteSession)

        Button(
            onClick = onStartPractice,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("연습 시작", style = MaterialTheme.typography.titleSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
        ) {
            OutlinedButton(
                onClick = { showMoveDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("디렉토리 이동")
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("악보 삭제")
            }
        }
    }

    if (showMoveDialog) {
        MoveSheetDialog(
            directories = directories,
            currentDirectoryId = detail.sheet.directoryId,
            onDismiss = { showMoveDialog = false },
            onSelect = { directoryId ->
                onMoveSheet(directoryId)
                showMoveDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("악보 삭제") },
            text = { Text("악보와 연습 기록, 페이지 이미지가 모두 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        onDeleteSheet()
                        onBack()
                    }
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun MoveSheetDialog(
    directories: List<SheetDirectory>,
    currentDirectoryId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("디렉토리 이동") },
        text = {
            if (directories.isEmpty()) {
                Text(
                    text = "이동할 디렉토리가 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    directories.forEach { directory ->
                        TextButton(
                            onClick = { onSelect(directory.id) },
                            enabled = directory.id != currentDirectoryId,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(directory.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}

@Composable
private fun SheetHeader(
    sheet: SheetMusic,
    pageCount: Int,
    onRescanPages: () -> Unit,
) {
    Column {
        Text(
            text = sheet.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        sheet.composer?.let { composer ->
            Spacer(Modifier.height(Dimens.TinySpacing))
            Text(
                text = composer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))

        PianoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "페이지 수",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (pageCount == 0) "악보 등록 필요" else "${pageCount}쪽",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SmallSpacing))

        OutlinedButton(
            onClick = onRescanPages,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Description, contentDescription = null)
            Spacer(Modifier.width(Dimens.SmallSpacing))
            Text("페이지 다시 스캔")
        }
    }
}

@Composable
private fun SectionPages(
    pages: List<SheetPage>,
    sheetFileStorage: SheetFileStorage,
) {
    if (pages.isEmpty()) return

    Column {
        SectionTitle("악보 페이지")
        Spacer(Modifier.height(Dimens.SmallSpacing))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            items(pages, key = { it.id }) { page ->
                PageThumbnail(page = page, sheetFileStorage = sheetFileStorage)
            }
        }
    }
}

@Composable
private fun PageThumbnail(
    page: SheetPage,
    sheetFileStorage: SheetFileStorage,
) {
    val bitmap = remember(page.filePath) {
        runCatching {
            BitmapFactory.decodeFile(sheetFileStorage.toFile(page.filePath).absolutePath)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "${page.pageOrder}쪽",
            modifier = Modifier
                .width(120.dp)
                .height(160.dp),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(160.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${page.pageOrder}쪽",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionNote(lessonNote: String?, onSaveNote: (String) -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var inputText by rememberSaveable { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("레슨 노트", modifier = Modifier.weight(1f))
            TextButton(onClick = {
                inputText = lessonNote ?: ""
                showDialog = true
            }) {
                Text("수정")
            }
        }
        Spacer(Modifier.height(Dimens.SmallSpacing))
        if (lessonNote.isNullOrBlank()) {
            Text(
                text = "아직 레슨 노트가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PianoCard {
                Text(
                    text = lessonNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("레슨 노트 수정") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveNote(inputText)
                    showDialog = false
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun SectionRecording(
    recordings: List<Recording>,
    uiState: RecordingUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayback: (Recording) -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onSetRepresentative: (Recording) -> Unit,
    onExportRecording: (Recording, Uri) -> Unit,
    onImportRecording: (Uri) -> Unit,
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Recording?>(null) }
    var exportingRecording by remember { mutableStateOf<Recording?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartRecording()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportRecording(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/mpeg")
    ) { uri ->
        val recording = exportingRecording
        exportingRecording = null
        if (uri != null && recording != null) onExportRecording(recording, uri)
    }

    fun requestStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            onStartRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("녹음", modifier = Modifier.weight(1f))
            TextButton(onClick = { importLauncher.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.width(Dimens.TinySpacing))
                Text("가져오기")
            }
        }
        Spacer(Modifier.height(Dimens.SmallSpacing))

        if (uiState.isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PianoCard(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "녹음 중 ${formatDuration(uiState.recordingElapsedSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.width(Dimens.SmallSpacing))
                IconButton(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "녹음 중지",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            Button(
                onClick = { requestStart() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Spacer(Modifier.width(Dimens.SmallSpacing))
                Text("녹음 시작", style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(Modifier.height(Dimens.SmallSpacing))

        if (recordings.isEmpty()) {
            Text(
                text = "아직 녹음이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PianoCard {
                recordings.forEachIndexed { index, recording ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onTogglePlayback(recording) }) {
                            val isPlaying = uiState.playingRecordingId == recording.id
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "재생 중지" else "재생",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.width(Dimens.SmallSpacing))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatDate(recording.createdAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Dimens.TinySpacing))
                            Text(
                                text = formatTime(recording.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = formatDuration(recording.durationSeconds.toLong()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(onClick = { onSetRepresentative(recording) }) {
                            if (recording.isRepresentative) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "대표 녹음 해제",
                                    tint = Color(0xFFF4B400),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = "대표 녹음으로 설정",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = {
                            exportingRecording = recording
                            exportLauncher.launch("recording-${recording.id}.m4a")
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = "녹음 내보내기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { pendingDelete = recording }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "녹음 삭제",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index != recordings.lastIndex) {
                        Spacer(Modifier.height(Dimens.ItemSpacing))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(Dimens.ItemSpacing))
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("이 녹음을 삭제할까요?") },
            text = { Text("삭제한 녹음은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecording(target)
                    pendingDelete = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun SectionRecords(
    sessions: List<PracticeSession>,
    onDeleteSession: (Long) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<PracticeSession?>(null) }

    Column {
        SectionTitle("연습 기록")
        Spacer(Modifier.height(Dimens.SmallSpacing))
        if (sessions.isEmpty()) {
            Text(
                text = "아직 연습 기록이 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PianoCard {
                sessions.forEachIndexed { index, session ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatDate(session.startedAt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Dimens.TinySpacing))
                            Text(
                                text = formatTimeRange(session.startedAt, session.endedAt ?: session.startedAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!session.dailyMemo.isNullOrBlank()) {
                                Spacer(Modifier.height(Dimens.TinySpacing))
                                Text(
                                    text = session.dailyMemo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(Dimens.SmallSpacing))
                        Text(
                            text = formatMinutes((session.durationSeconds ?: 0) / 60),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(onClick = { pendingDelete = session }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "기록 삭제",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index != sessions.lastIndex) {
                        Spacer(Modifier.height(Dimens.ItemSpacing))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(Dimens.ItemSpacing))
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("이 기록을 삭제할까요?") },
            text = { Text("삭제한 연습 기록은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(target.id)
                    pendingDelete = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("취소")
                }
            },
        )
    }
}
