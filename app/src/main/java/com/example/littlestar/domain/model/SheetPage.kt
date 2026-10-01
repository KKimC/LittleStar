package com.example.littlestar.domain.model

/**
 * 악보 한 페이지. [filePath]는 앱 내부 저장소 기준 상대 경로를 담는다.
 * (예: sheets/sheet-3/page-001.jpg)
 */
data class SheetPage(
    val id: Long,
    val sheetMusicId: Long,
    val pageOrder: Int,
    val filePath: String,
)
