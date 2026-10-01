package com.example.littlestar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.littlestar.data.local.entity.SheetMusicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SheetMusicDao {
    @Query("SELECT * FROM sheet_music ORDER BY directoryId, czernyNumber ASC, title ASC")
    fun observeAll(): Flow<List<SheetMusicEntity>>

    @Query("SELECT * FROM sheet_music WHERE directoryId = :directoryId ORDER BY czernyNumber ASC, title ASC")
    fun observeByDirectory(directoryId: Long): Flow<List<SheetMusicEntity>>

    @Query("SELECT * FROM sheet_music WHERE directoryId = :directoryId")
    suspend fun getByDirectory(directoryId: Long): List<SheetMusicEntity>

    @Query("UPDATE sheet_music SET directoryId = :directoryId WHERE id = :id")
    suspend fun moveToDirectory(id: Long, directoryId: Long)

    @Query("SELECT * FROM sheet_music WHERE id = :id")
    fun observeById(id: Long): Flow<SheetMusicEntity?>

    @Query("SELECT * FROM sheet_music WHERE id = :id")
    suspend fun getById(id: Long): SheetMusicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sheets: List<SheetMusicEntity>)

    @Insert
    suspend fun insert(sheet: SheetMusicEntity): Long

    @Query("UPDATE sheet_music SET lessonNote = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateLessonNote(id: Long, note: String?, updatedAt: Long)

    @Query("DELETE FROM sheet_music WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM sheet_music")
    suspend fun count(): Int
}
