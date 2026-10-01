package com.example.littlestar.feature.scanner

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.littlestar.core.design.components.PianoTopBar
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.di.LocalAppContainer
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 악보 스캔 화면.
 * ML Kit Document Scanner로 페이지를 촬영하고, 제목/작곡가를 입력한 뒤
 * SaveScannedSheetUseCase를 통해 새 악보로 저장한다.
 */
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    replaceSheetId: Long? = null,
) {
    val container = LocalAppContainer.current
    val viewModel: ScannerViewModel = viewModel {
        ScannerViewModel(
            container.saveScannedSheetUseCase,
            container.replaceSheetPagesUseCase,
            container.getSheetsUseCase.directories(),
            replaceSheetId,
        )
    }
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ML Kit 문서 스캐너 클라이언트. 갤러리 가져오기도 허용하고, 최대 20페이지까지 JPEG로 스캔한다.
    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(20)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scanResult?.pages?.map { it.imageUri }
            if (!uris.isNullOrEmpty()) {
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) {
                        uris.mapNotNull { uri ->
                            runCatching {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }.getOrNull()
                        }
                    }
                    viewModel.setPages(bytes)
                }
            }
        }
    }

    fun startScan() {
        val activity = context as? Activity
        if (activity == null) {
            errorMessage = "스캐너를 시작할 수 없습니다."
            return
        }
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                errorMessage = "스캐너를 시작할 수 없습니다: ${e.message}"
            }
    }

    LaunchedEffect(uiState.savedSheetId) {
        uiState.savedSheetId?.let {
            viewModel.consumeSaved()
            onSaved(it)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PianoTopBar(title = if (viewModel.isReplaceMode) "페이지 다시 스캔" else "악보 스캔", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (uiState.pages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SectionSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "카메라로 악보를 스캔해 페이지를 등록하세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Dimens.ItemSpacing))
                    Button(onClick = { startScan() }) {
                        Text("스캔 시작")
                    }
                }
            } else {
                uiState.pages.forEachIndexed { index, bytes ->
                    val bitmap = remember(bytes) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    Text(
                        text = "페이지 ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "페이지 ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            if (!viewModel.isReplaceMode) {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("악보 제목") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = uiState.composer,
                    onValueChange = viewModel::updateComposer,
                    label = { Text("작곡가 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    text = "저장 위치",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing),
                ) {
                    items(uiState.directories, key = { it.id }) { directory ->
                        FilterChip(
                            selected = directory.id == uiState.selectedDirectoryId,
                            onClick = { viewModel.selectDirectory(directory.id) },
                            label = { Text(directory.name) },
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
                enabled = if (viewModel.isReplaceMode) {
                    !uiState.isSaving && uiState.pages.isNotEmpty()
                } else {
                    uiState.selectedDirectoryId != null && !uiState.isSaving && uiState.pages.isNotEmpty()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("저장")
                }
            }
        }
    }
}
