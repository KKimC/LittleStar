package com.example.littlestar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.littlestar.data.local.entity.SheetPageEntity
import com.example.littlestar.domain.model.SheetPageCount
import kotlinx.coroutines.flow.Flow

@Dao
interface SheetPageDao {
    @Query("SELECT * FROM sheet_page WHERE sheetMusicId = :sheetId ORDER BY pageOrder ASC")
    fun observePages(sheetId: Long): Flow<List<SheetPageEntity>>

    @Query("SELECT sheetMusicId, COUNT(*) AS count FROM sheet_page GROUP BY sheetMusicId")
    fun observePageCounts(): Flow<List<SheetPageCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<SheetPageEntity>)

    @Query("DELETE FROM sheet_page WHERE sheetMusicId = :sheetId")
    suspend fun deleteForSheet(sheetId: Long)

    /** 기존 페이지를 삭제하고 새 페이지로 교체한다. */
    @Transaction
    suspend fun replaceForSheet(sheetId: Long, pages: List<SheetPageEntity>) {
        deleteForSheet(sheetId)
        insertAll(pages)
    }
}
