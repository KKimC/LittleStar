package com.example.littlestar.feature.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.littlestar.core.design.components.PianoCard
import com.example.littlestar.core.design.components.PianoTopBar
import com.example.littlestar.core.design.components.SectionTitle
import com.example.littlestar.core.design.theme.Dimens
import com.example.littlestar.core.util.formatDate
import com.example.littlestar.core.util.formatMinutes
import com.example.littlestar.core.util.formatTime
import com.example.littlestar.di.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * 설정 화면.
 * 일일 연습 목표, 기본 설정, 데이터 백업/복원을 제공한다.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(container.settingsRepository) }
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var lastBackup by remember { mutableStateOf(container.backupManager.lastBackupTimeMillis()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PianoTopBar(title = "설정", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            PianoCard {
                SectionTitle("일일 연습 목표")
                Spacer(Modifier.height(Dimens.ItemSpacing))
                Text(
                    text = formatMinutes(uiState.dailyGoalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = uiState.dailyGoalMinutes.toFloat(),
                    onValueChange = { viewModel.setDailyGoal(it.toInt()) },
                    valueRange = 5f..300f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "매일 이 시간만큼 연습하는 것을 목표로 합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PianoCard {
                SectionTitle("기본 설정")
                Spacer(Modifier.height(Dimens.SmallSpacing))
                SettingRow(label = "다크 모드", enabled = false)
                SettingRow(label = "연습 알림", enabled = false)
                SettingRow(label = "연습 종료 시 알림", enabled = false)
            }

            PianoCard {
                SectionTitle("데이터 백업")
                Spacer(Modifier.height(Dimens.SmallSpacing))
                Text(
                    text = "앱 데이터(악보·연습 기록·이미지)를 최신 백업 하나로 보관합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                lastBackup?.let {
                    Spacer(Modifier.height(Dimens.TinySpacing))
                    Text(
                        text = "마지막 백업: ${formatDate(it)} ${formatTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Dimens.SmallSpacing))
                Button(onClick = {
                    scope.launch {
                        val ok = container.backupManager.backup()
                        backupMessage = if (ok) "백업이 완료되었습니다." else "백업에 실패했습니다."
                        if (ok) lastBackup = container.backupManager.lastBackupTimeMillis()
                    }
                }) {
                    Text("지금 백업")
                }
                Spacer(Modifier.height(Dimens.SmallSpacing))
                OutlinedButton(onClick = { showRestoreConfirm = true }) {
                    Text("백업 복원")
                }
                backupMessage?.let {
                    Spacer(Modifier.height(Dimens.SmallSpacing))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("백업 복원") },
            text = { Text("현재 데이터가 최신 백업의 내용으로 교체됩니다. 복원 후 앱이 재시작됩니다. 계속할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    scope.launch {
                        val ok = container.backupManager.restore()
                        if (ok) {
                            restartApp(context)
                        } else {
                            backupMessage = "복원할 백업이 없거나 실패했습니다."
                        }
                    }
                }) { Text("복원") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("취소") }
            },
        )
    }
}

/** 준비 중인 설정 항목 */
@Composable
private fun SettingRow(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SmallSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = false, onCheckedChange = null, enabled = enabled)
    }
}

/** 복원 완료 후 앱을 재시작하여 복원된 데이터를 로드한다. */
private fun restartApp(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(launchIntent)
    Runtime.getRuntime().exit(0)
}
