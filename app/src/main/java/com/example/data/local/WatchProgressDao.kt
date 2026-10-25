package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress ORDER BY lastWatchedAt DESC")
    fun getAllProgress(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId LIMIT 1")
    suspend fun getProgress(animeId: Int): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE animeId = :animeId LIMIT 1")
    fun observeProgress(animeId: Int): Flow<WatchProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(entity: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE animeId = :animeId")
    suspend fun deleteProgress(animeId: Int)
}
