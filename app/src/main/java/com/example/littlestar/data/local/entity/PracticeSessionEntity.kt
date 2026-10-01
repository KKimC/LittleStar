package com.example.littlestar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.littlestar.domain.model.PracticeSessionStatus

@Entity(
    tableName = "practice_session",
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
        Index("startedAt"),
        Index("status"),
    ],
)
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetMusicId: Long,
    val startedAt: Long,
    val startedRealtimeMs: Long,
    val endedAt: Long?,
    val durationSeconds: Int?,
    val dailyMemo: String?,
    val status: PracticeSessionStatus,
)
