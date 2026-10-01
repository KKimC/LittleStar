package com.example.littlestar.di

import android.content.Context
import com.example.littlestar.data.local.PianoFlowDatabase
import com.example.littlestar.data.repository.RoomPracticeRepository
import com.example.littlestar.data.repository.RoomRecordingRepository
import com.example.littlestar.data.repository.RoomSheetRepository
import com.example.littlestar.data.repository.SharedPrefsSettingsRepository
import com.example.littlestar.data.storage.BackupManager
import com.example.littlestar.data.storage.InkStorage
import com.example.littlestar.data.storage.RecordingFileStorage
import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.RecordingRepository
import com.example.littlestar.domain.repository.SettingsRepository
import com.example.littlestar.domain.repository.SheetRepository
import com.example.littlestar.domain.usecase.DeleteDirectoryUseCase
import com.example.littlestar.domain.usecase.DeleteSheetUseCase
import com.example.littlestar.domain.usecase.GetDashboardStatsUseCase
import com.example.littlestar.domain.usecase.GetSheetDetailUseCase
import com.example.littlestar.domain.usecase.GetSheetsUseCase
import com.example.littlestar.domain.usecase.ReplaceSheetPagesUseCase
import com.example.littlestar.domain.usecase.SaveScannedSheetUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 수동 의존성 주입 컨테이너.
 * 앱 전체에서 공유되는 인스턴스(DB/리포지토리/유스케이스)를 생성/제공한다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = PianoFlowDatabase.getInstance(appContext)

    val sheetRepository: SheetRepository =
        RoomSheetRepository(database.sheetDirectoryDao(), database.sheetMusicDao(), database.sheetPageDao())
    val practiceRepository: PracticeRepository =
        RoomPracticeRepository(database.practiceSessionDao())
    val recordingRepository: RecordingRepository =
        RoomRecordingRepository(database.recordingDao())
    val settingsRepository: SettingsRepository = SharedPrefsSettingsRepository(appContext)
    val sheetFileStorage = SheetFileStorage(appContext)
    val inkStorage = InkStorage(appContext)
    val recordingFileStorage = RecordingFileStorage(appContext)
    val backupManager = BackupManager(appContext)

    val getSheetsUseCase = GetSheetsUseCase(sheetRepository, practiceRepository)
    val getDashboardStatsUseCase = GetDashboardStatsUseCase(sheetRepository, practiceRepository, settingsRepository)
    val getSheetDetailUseCase = GetSheetDetailUseCase(sheetRepository, practiceRepository)
    val saveScannedSheetUseCase = SaveScannedSheetUseCase(sheetRepository, sheetFileStorage)
    val replaceSheetPagesUseCase = ReplaceSheetPagesUseCase(sheetRepository, sheetFileStorage)
    val deleteSheetUseCase = DeleteSheetUseCase(sheetRepository, sheetFileStorage, recordingFileStorage)
    val deleteDirectoryUseCase = DeleteDirectoryUseCase(sheetRepository, sheetFileStorage, recordingFileStorage)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val appScope: CoroutineScope get() = scope

    /** 앱 시작 시 이전 프로세스에서 남은 RUNNING 세션을 INTERRUPTED로 정리한다. */
    fun recoverRunningSessions() {
        scope.launch { practiceRepository.interruptRunningSessions() }
    }
}
