package com.airline.checkin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.airline.checkin.data.local.dao.*
import com.airline.checkin.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        BookingEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookingDao(): BookingDao
}
