package com.airline.checkin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.airline.checkin.data.local.dao.*
import com.airline.checkin.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        FlightEntity::class,
        BookingEntity::class,
        PassengerEntity::class,
        SeatEntity::class,
        BoardingPassEntity::class,
        BaggageDeclarationEntity::class,
        SavedPassengerEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun flightDao(): FlightDao
    abstract fun bookingDao(): BookingDao
    abstract fun passengerDao(): PassengerDao
    abstract fun seatDao(): SeatDao
    abstract fun boardingPassDao(): BoardingPassDao
    abstract fun baggageDeclarationDao(): BaggageDeclarationDao
    abstract fun savedPassengerDao(): SavedPassengerDao
}
