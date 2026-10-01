package com.example.littlestar.domain.usecase

import com.example.littlestar.domain.model.SheetDetail
import com.example.littlestar.domain.repository.PracticeRepository
import com.example.littlestar.domain.repository.SheetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 악보 상세 조회 유스케이스 (곡 + 페이지 + 연습 기록) */
class GetSheetDetailUseCase(
    private val sheetRepository: SheetRepository,
    private val practiceRepository: PracticeRepository,
) {
    operator fun invoke(sheetId: Long): Flow<SheetDetail?> = combine(
        sheetRepository.observeSheet(sheetId),
        sheetRepository.observePages(sheetId),
        practiceRepository.observeSessions(sheetId),
    ) { sheet, pages, sessions ->
        sheet?.let { SheetDetail(sheet = it, pages = pages, practiceSessions = sessions) }
    }
}
