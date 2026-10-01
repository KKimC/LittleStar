package com.example.littlestar.domain.model

/**
 * 악보(곡) 메타데이터. 악보 이미지 파일은 [SheetPage]로 분리된다.
 * [directoryId]는 소속 디렉토리, [czernyNumber]는 간추린 체르니에 해당할 때만 값을 가진다.
 */
data class SheetMusic(
    val id: Long,
    val title: String,
    val composer: String?,
    val directoryId: Long,
    val czernyNumber: Int?,
    val lessonNote: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
