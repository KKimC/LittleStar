package com.example.littlestar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sheet_music",
    foreignKeys = [
        ForeignKey(
            entity = SheetDirectoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["directoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index("directoryId"),
        Index(value = ["czernyNumber"], unique = true),
    ],
)
data class SheetMusicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val composer: String?,
    val directoryId: Long,
    val czernyNumber: Int?,
    val lessonNote: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
