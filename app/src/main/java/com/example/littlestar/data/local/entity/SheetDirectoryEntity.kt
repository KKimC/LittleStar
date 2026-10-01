package com.example.littlestar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 악보 보관함 디렉토리(폴더) */
@Entity(tableName = "sheet_directory")
data class SheetDirectoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)
