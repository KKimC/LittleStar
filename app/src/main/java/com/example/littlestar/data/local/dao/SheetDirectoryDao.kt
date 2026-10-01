package com.example.littlestar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.littlestar.data.local.entity.SheetDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SheetDirectoryDao {
    @Query("SELECT * FROM sheet_directory ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SheetDirectoryEntity>>

    @Insert
    suspend fun insert(directory: SheetDirectoryEntity): Long

    @Query("UPDATE sheet_directory SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM sheet_directory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT MAX(sortOrder) FROM sheet_directory")
    suspend fun maxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM sheet_directory")
    suspend fun count(): Int
}
