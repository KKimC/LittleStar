package com.example.littlestar.domain.model

/**
 * 악보 목록에서 한 곡을 표시하기 위한 데이터.
 * [totalPracticeMinutes]는 곡별 누적 연습 시간, [pageCount]는 등록된 페이지 수(0이면 "악보 등록 필요").
 */
data class SheetWithPractice(
    val sheet: SheetMusic,
    val totalPracticeMinutes: Int,
    val pageCount: Int,
)
