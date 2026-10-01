package com.example.littlestar.data.repository

import com.example.littlestar.data.local.entity.PracticeSessionEntity
import com.example.littlestar.data.local.entity.RecordingEntity
import com.example.littlestar.data.local.entity.SheetDirectoryEntity
import com.example.littlestar.data.local.entity.SheetMusicEntity
import com.example.littlestar.data.local.entity.SheetPageEntity
import com.example.littlestar.domain.model.PracticeSession
import com.example.littlestar.domain.model.Recording
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.model.SheetPage

fun SheetDirectoryEntity.toDomain() = SheetDirectory(id, name, sortOrder)
fun SheetMusicEntity.toDomain() = SheetMusic(id, title, composer, directoryId, czernyNumber, lessonNote, createdAt, updatedAt)
fun SheetPageEntity.toDomain() = SheetPage(id, sheetMusicId, pageOrder, filePath)
fun PracticeSessionEntity.toDomain() = PracticeSession(id, sheetMusicId, startedAt, startedRealtimeMs, endedAt, durationSeconds, dailyMemo, status)
fun RecordingEntity.toDomain() = Recording(id, sheetMusicId, fileName, durationSeconds, createdAt, isRepresentative)

fun SheetMusic.toEntity() = SheetMusicEntity(id, title, composer, directoryId, czernyNumber, lessonNote, createdAt, updatedAt)
fun SheetPage.toEntity() = SheetPageEntity(id, sheetMusicId, pageOrder, filePath)
fun PracticeSession.toEntity() = PracticeSessionEntity(id, sheetMusicId, startedAt, startedRealtimeMs, endedAt, durationSeconds, dailyMemo, status)
fun Recording.toEntity() = RecordingEntity(id, sheetMusicId, fileName, durationSeconds, createdAt, isRepresentative)
