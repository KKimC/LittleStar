package com.example.littlestar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 녹음 메타데이터. 실제 오디오 파일은 내부 저장소에 저장되고
 * [fileName]은 그 상대 경로를 담는다. 곡(SheetMusicEntity) 1:N 관계.
 */
@Entity(
    tableName = "recording",
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
        Index("createdAt"),
    ],
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetMusicId: Long,
    val fileName: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val isRepresentative: Boolean,
)
