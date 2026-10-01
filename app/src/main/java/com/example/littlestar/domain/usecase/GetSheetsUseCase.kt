package com.example.littlestar.domain.usecase

import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetWithPractice
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.SheetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 악보 보관함 조회 유스케이스.
 * 디렉토리 목록과, 곡 정보 + 누적 연습 시간 + 등록 페이지 수를 합친 목록을 제공한다.
 */
class GetSheetsUseCase(
    private val sheetRepository: SheetRepository,
    private val practiceRepository: PracticeRepository,
) {
    fun directories(): Flow<List<SheetDirectory>> = sheetRepository.observeDirectories()

    suspend fun addDirectory(name: String): Long = sheetRepository.insertDirectory(name)

    fun allSheets(): Flow<List<SheetWithPractice>> = combine(
        sheetRepository.observeAllSheets(),
        practiceRepository.observeTotalsBySheet(),
        sheetRepository.observePageCounts(),
    ) { sheets, totals, pageCounts ->
        val totalMap = totals.associate { it.sheetMusicId to (it.totalSeconds ?: 0L) }
        val countMap = pageCounts.associate { it.sheetMusicId to it.count }
        sheets.map { sheet ->
            SheetWithPractice(
                sheet = sheet,
                totalPracticeMinutes = ((totalMap[sheet.id] ?: 0L) / 60L).toInt(),
                pageCount = countMap[sheet.id] ?: 0,
            )
        }
    }
}
