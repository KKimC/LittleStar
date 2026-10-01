package com.example.littlestar.domain.model

/** 악보별 누적 연습 초(SQL GROUP BY 집계 결과) */
data class SheetPracticeTotal(
    val sheetMusicId: Long,
    val totalSeconds: Long?,
)
