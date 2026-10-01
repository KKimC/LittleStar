package com.example.littlestar.domain.usecase

import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.domain.model.SheetPage
import com.example.littlestar.domain.repository.SheetRepository

/**
 * 기존 악보의 페이지 이미지를 새로 스캔한 이미지로 교체하는 유스케이스.
 * 악보 메타데이터/연습 기록/메모는 유지된다.
 */
class ReplaceSheetPagesUseCase(
    private val sheetRepository: SheetRepository,
    private val fileStorage: SheetFileStorage,
) {
    suspend operator fun invoke(sheetId: Long, images: List<ByteArray>) {
        val paths = fileStorage.replacePages(sheetId, images)
        val pages = paths.mapIndexed { index, path ->
            SheetPage(id = 0, sheetMusicId = sheetId, pageOrder = index + 1, filePath = path)
        }
        sheetRepository.replacePages(sheetId, pages)
    }
}
