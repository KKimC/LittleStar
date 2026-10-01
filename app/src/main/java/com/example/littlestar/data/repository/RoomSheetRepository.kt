package com.example.littlestar.data.repository

import com.example.littlestar.data.local.dao.SheetDirectoryDao
import com.example.littlestar.data.local.dao.SheetMusicDao
import com.example.littlestar.data.local.dao.SheetPageDao
import com.example.littlestar.data.local.entity.SheetDirectoryEntity
import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.model.SheetPage
import com.example.littlestar.domain.model.SheetPageCount
import com.example.littlestar.domain.repository.SheetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSheetRepository(
    private val sheetDirectoryDao: SheetDirectoryDao,
    private val sheetMusicDao: SheetMusicDao,
    private val sheetPageDao: SheetPageDao,
) : SheetRepository {

    override fun observeDirectories(): Flow<List<SheetDirectory>> =
        sheetDirectoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAllSheets(): Flow<List<SheetMusic>> =
        sheetMusicDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSheets(directoryId: Long): Flow<List<SheetMusic>> =
        sheetMusicDao.observeByDirectory(directoryId).map { list -> list.map { it.toDomain() } }

    override fun observeSheet(sheetId: Long): Flow<SheetMusic?> =
        sheetMusicDao.observeById(sheetId).map { it?.toDomain() }

    override fun observePages(sheetId: Long): Flow<List<SheetPage>> =
        sheetPageDao.observePages(sheetId).map { list -> list.map { it.toDomain() } }

    override fun observePageCounts(): Flow<List<SheetPageCount>> =
        sheetPageDao.observePageCounts()

    override suspend fun getSheet(sheetId: Long): SheetMusic? =
        sheetMusicDao.getById(sheetId)?.toDomain()

    override suspend fun insertDirectory(name: String): Long {
        val nextOrder = (sheetDirectoryDao.maxSortOrder() ?: 0) + 1
        val entity = SheetDirectoryEntity(name = name, sortOrder = nextOrder)
        return sheetDirectoryDao.insert(entity)
    }

    override suspend fun renameDirectory(id: Long, name: String) {
        sheetDirectoryDao.rename(id, name)
    }

    override suspend fun deleteDirectory(id: Long) {
        sheetDirectoryDao.deleteById(id)
    }

    override suspend fun getSheets(directoryId: Long): List<SheetMusic> =
        sheetMusicDao.getByDirectory(directoryId).map { it.toDomain() }

    override suspend fun moveSheet(sheetId: Long, directoryId: Long) {
        sheetMusicDao.moveToDirectory(sheetId, directoryId)
    }

    override suspend fun insertSheet(sheet: SheetMusic): Long =
        sheetMusicDao.insert(sheet.toEntity())

    override suspend fun updateLessonNote(sheetId: Long, note: String?) {
        sheetMusicDao.updateLessonNote(sheetId, note, System.currentTimeMillis())
    }

    override suspend fun replacePages(sheetId: Long, pages: List<SheetPage>) {
        sheetPageDao.replaceForSheet(sheetId, pages.map { it.toEntity() })
    }

    override suspend fun deleteSheet(sheetId: Long) {
        sheetMusicDao.deleteById(sheetId)
    }
}
