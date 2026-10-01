package com.example.littlestar.domain.model

/**
 * 악보 보관함의 디렉토리(폴더).
 * 사용자가 자유롭게 추가/관리할 수 있다.
 */
data class SheetDirectory(
    val id: Long,
    val name: String,
    val sortOrder: Int,
)
