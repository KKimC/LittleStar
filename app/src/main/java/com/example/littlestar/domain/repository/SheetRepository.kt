package com.example.littlestar.domain.repository

import com.example.littlestar.domain.model.SheetDirectory
import com.example.littlestar.domain.model.SheetMusic
import com.example.littlestar.domain.model.SheetPage
import com.example.littlestar.domain.model.SheetPageCount
import kotlinx.coroutines.flow.Flow

/**
 * 악보(SheetMusic), 페이지(SheetPage), 디렉토리(SheetDirectory) 제공 계약.
 */
interface SheetRepository {
    fun observeDirectories(): Flow<List<SheetDirectory>>
    fun observeAllSheets(): Flow<List<SheetMusic>>
    fun observeSheets(directoryId: Long): Flow<List<SheetMusic>>
    fun observeSheet(sheetId: Long): Flow<SheetMusic?>
    fun observePages(sheetId: Long): Flow<List<SheetPage>>
    fun observePageCounts(): Flow<List<SheetPageCount>>
    suspend fun getSheet(sheetId: Long): SheetMusic?

    /** 새 디렉토리를 추가하고 id를 반환한다. */
    suspend fun insertDirectory(name: String): Long

    /** 디렉토리 이름을 변경한다. */
    suspend fun renameDirectory(id: Long, name: String)

    /** 디렉토리를 삭제한다(소속 악보는 cascade 삭제). */
    suspend fun deleteDirectory(id: Long)

    /** 디렉토리에 속한 악보 목록을 한 번 조회한다. */
    suspend fun getSheets(directoryId: Long): List<SheetMusic>

    /** 악보를 다른 디렉토리로 이동한다. */
    suspend fun moveSheet(sheetId: Long, directoryId: Long)

    /** 새 악보를 저장하고 id를 반환한다. */
    suspend fun insertSheet(sheet: SheetMusic): Long

    /** 레슨 메모를 갱신한다. */
    suspend fun updateLessonNote(sheetId: Long, note: String?)

    /** 악보 페이지 전체를 교체한다(기존 페이지 삭제 후 새로 삽입). */
    suspend fun replacePages(sheetId: Long, pages: List<SheetPage>)

    /** 악보와 연관 데이터를 삭제한다. */
    suspend fun deleteSheet(sheetId: Long)
}
