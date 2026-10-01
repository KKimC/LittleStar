package com.example.littlestar.domain.model

/**
 * 악보 상세 화면에 필요한 전체 정보.
 * 레슨 메모는 [SheetMusic.lessonNote], 연습 기록은 [practiceSessions]에 담긴다.
 */
data class SheetDetail(
    val sheet: SheetMusic,
    val pages: List<SheetPage>,
    val practiceSessions: List<PracticeSession>,
)
