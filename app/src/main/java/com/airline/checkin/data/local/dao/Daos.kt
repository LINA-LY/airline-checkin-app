package com.airline.checkin.data.local.dao

import androidx.room.*
import com.airline.checkin.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: String): BookingEntity?

    @Upsert
    suspend fun upsert(booking: BookingEntity)

    @Query("UPDATE bookings SET checkInStatus = 1 WHERE id = :id")
    suspend fun markCheckedIn(id: String)

    @Query("SELECT * FROM bookings WHERE userId = :userId")
    fun getByUserFlow(userId: String): Flow<List<BookingEntity>>
}
