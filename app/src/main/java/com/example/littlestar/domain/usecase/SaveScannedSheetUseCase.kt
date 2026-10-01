package com.example.littlestar.domain.usecase

import com.example.littlestar.data.storage.SheetFileStorage
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.model.SheetPage
import com.example.littlestar.domain.repository.SheetRepository

/**
 * 스캔한 악보(이미지 목록)를 특정 디렉토리에 새 악보로 저장하는 유스케이스.
 *
 * 1. 악보 메타데이터 저장 -> id 획득
 * 2. 이미지를 내부 저장소에 저장 -> 상대 경로 획득
 * 3. SheetPage 저장
 * 중간 실패 시 저장한 악보와 파일을 정리한다.
 */
class SaveScannedSheetUseCase(
    private val sheetRepository: SheetRepository,
    private val fileStorage: SheetFileStorage,
) {
    suspend operator fun invoke(
        title: String,
        composer: String?,
        directoryId: Long,
        images: List<ByteArray>,
    ): Long {
        val now = System.currentTimeMillis()
        val sheet = SheetMusic(
            id = 0,
            title = title.ifBlank { "제목 없음" },
            composer = composer,
            directoryId = directoryId,
            czernyNumber = null,
            lessonNote = null,
            createdAt = now,
            updatedAt = now,
        )
        val sheetId = sheetRepository.insertSheet(sheet)
        return try {
            val paths = fileStorage.savePages(sheetId, images)
            val pages = paths.mapIndexed { index, path ->
                SheetPage(id = 0, sheetMusicId = sheetId, pageOrder = index + 1, filePath = path)
            }
            sheetRepository.replacePages(sheetId, pages)
            sheetId
        } catch (e: Exception) {
            sheetRepository.deleteSheet(sheetId)
            fileStorage.deleteSheet(sheetId)
            throw e
        }
    }
}
