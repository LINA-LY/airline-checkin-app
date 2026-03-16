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
interface FlightDao {
    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun getById(id: String): FlightEntity?

    @Upsert
    suspend fun upsert(flight: FlightEntity)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE reference = :ref")
    suspend fun getByReference(ref: String): BookingEntity?

    @Upsert
    suspend fun upsert(booking: BookingEntity)

    @Query("UPDATE bookings SET checkInStatus = 1 WHERE id = :id")
    suspend fun markCheckedIn(id: String)
}

@Dao
interface PassengerDao {
    @Query("SELECT * FROM passengers WHERE bookingId = :bookingId")
    suspend fun getByBooking(bookingId: String): PassengerEntity?

    @Upsert
    suspend fun upsert(passenger: PassengerEntity)
}

@Dao
interface SeatDao {
    @Query("SELECT * FROM seats WHERE flightId = :flightId")
    fun getByFlight(flightId: String): Flow<List<SeatEntity>>

    @Upsert
    suspend fun upsertAll(seats: List<SeatEntity>)

    @Query("UPDATE seats SET isOccupied = 1 WHERE id = :seatId")
    suspend fun markOccupied(seatId: String)
}

@Dao
interface BoardingPassDao {
    @Query("SELECT * FROM boarding_passes WHERE bookingId = :bookingId")
    suspend fun getByBooking(bookingId: String): BoardingPassEntity?

    @Upsert
    suspend fun upsert(boardingPass: BoardingPassEntity)
}

@Dao
interface BaggageDeclarationDao {
    @Query("SELECT * FROM baggage_declarations WHERE bookingId = :bookingId")
    suspend fun getByBooking(bookingId: String): BaggageDeclarationEntity?

    @Upsert
    suspend fun upsert(baggage: BaggageDeclarationEntity)
}
