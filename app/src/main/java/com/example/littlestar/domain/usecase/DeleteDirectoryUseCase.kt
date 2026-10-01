package com.example.littlestar.domain.usecase

import com.example.littlestar.data.storage.RecordingFileStorage
import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.domain.repository.SheetRepository

/**
 * 디렉토리와 소속 악보를 삭제한다.
 * DB cascade로 악보/페이지/세션/녹음이 삭제되지만, 파일은 별도 저장소에 있으므로 먼저 정리한다.
 */
class DeleteDirectoryUseCase(
    private val sheetRepository: SheetRepository,
    private val fileStorage: SheetFileStorage,
    private val recordingFileStorage: RecordingFileStorage,
) {
    suspend operator fun invoke(directoryId: Long) {
        val sheets = sheetRepository.getSheets(directoryId)
        sheets.forEach {
            fileStorage.deleteSheet(it.id)
            recordingFileStorage.deleteSheet(it.id)
        }
        sheetRepository.deleteDirectory(directoryId)
    }
}
