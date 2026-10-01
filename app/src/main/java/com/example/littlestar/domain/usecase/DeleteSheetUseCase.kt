package com.example.littlestar.domain.usecase

import com.example.littlestar.data.storage.RecordingFileStorage
import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.domain.repository.SheetRepository

/**
 * 악보와 연관 데이터(페이지/연습 기록/녹음) 및 이미지/오디오 파일을 삭제한다.
 */
class DeleteSheetUseCase(
    private val sheetRepository: SheetRepository,
    private val fileStorage: SheetFileStorage,
    private val recordingFileStorage: RecordingFileStorage,
) {
    suspend operator fun invoke(sheetId: Long) {
        sheetRepository.deleteSheet(sheetId)
        fileStorage.deleteSheet(sheetId)
        recordingFileStorage.deleteSheet(sheetId)
    }
}
