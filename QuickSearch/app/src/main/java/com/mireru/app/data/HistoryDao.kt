package com.mireru.app.data

import androidx.room.*
import com.mireru.app.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem): Long

    @Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}
