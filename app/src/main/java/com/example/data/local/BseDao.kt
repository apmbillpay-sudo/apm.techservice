package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BseDao {
    @Query("SELECT * FROM watchlist_stocks ORDER BY addedAtMillis DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist_stocks WHERE scripCode = :scripCode LIMIT 1")
    suspend fun getWatchlistItem(scripCode: String): WatchlistEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_stocks WHERE scripCode = :scripCode)")
    fun isStockWatched(scripCode: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Update
    suspend fun updateWatchlist(item: WatchlistEntity)

    @Delete
    suspend fun removeFromWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist_stocks WHERE scripCode = :scripCode")
    suspend fun removeFromWatchlistByCode(scripCode: String)

    // Saved Projections
    @Query("SELECT * FROM saved_projections ORDER BY createdAtMillis DESC")
    fun getAllSavedProjections(): Flow<List<SavedProjectionScenario>>

    @Query("SELECT * FROM saved_projections WHERE scripCode = :scripCode ORDER BY createdAtMillis DESC")
    fun getProjectionsForStock(scripCode: String): Flow<List<SavedProjectionScenario>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectionScenario(scenario: SavedProjectionScenario): Long

    @Delete
    suspend fun deleteProjectionScenario(scenario: SavedProjectionScenario)
}
