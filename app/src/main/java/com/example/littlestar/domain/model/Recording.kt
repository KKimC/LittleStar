package com.example.littlestar.domain.model

/**
 * 한 곡에 대한 한 번의 녹음.
 * 곡(SheetMusic) 1개에 녹음(Recording) N개가 연결된다(1:N).
 * [fileName]은 앱 내부 저장소 기준 상대 경로를 담는다.
 * (예: recordings/sheet-3/rec-1712345678901.m4a)
 */
data class Recording(
    val id: Long,
    val sheetMusicId: Long,
    val fileName: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val isRepresentative: Boolean,
)
