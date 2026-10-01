package com.example.littlestar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sheet_page",
    foreignKeys = [
        ForeignKey(
            entity = SheetMusicEntity::class,
            parentColumns = ["id"],
            childColumns = ["sheetMusicId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("sheetMusicId"),
        Index(value = ["sheetMusicId", "pageOrder"], unique = true),
    ],
)
data class SheetPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetMusicId: Long,
    val pageOrder: Int,
    val filePath: String,
)
