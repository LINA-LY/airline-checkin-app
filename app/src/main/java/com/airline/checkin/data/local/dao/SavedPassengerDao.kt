package com.airline.checkin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.airline.checkin.data.local.entity.SavedPassengerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPassengerDao {
    @Query("SELECT * FROM saved_passengers WHERE userId = :userId")
    fun getAll(userId: String): Flow<List<SavedPassengerEntity>>

    @Query("SELECT * FROM saved_passengers WHERE userId = :userId AND syncedToRemote = 0")
    suspend fun getUnsynced(userId: String): List<SavedPassengerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedPassengerEntity)

    @Query("DELETE FROM saved_passengers WHERE id = :id")
    suspend fun deleteById(id: String)
}
