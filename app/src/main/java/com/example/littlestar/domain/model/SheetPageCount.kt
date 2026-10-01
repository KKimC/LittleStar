package com.example.littlestar.domain.model

/** 악보별 등록된 페이지 수(SQL COUNT 집계 결과) */
data class SheetPageCount(
    val sheetMusicId: Long,
    val count: Int,
)
