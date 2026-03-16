package com.airline.checkin.data.repository

import com.airline.checkin.data.local.dao.*
import com.airline.checkin.data.remote.firebase.FirebaseService
import com.airline.checkin.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebase: FirebaseService
) {
    suspend fun signIn(email: String, password: String) =
        firebase.signIn(email, password)

    suspend fun register(email: String, password: String) =
        firebase.register(email, password)

    fun signOut() = firebase.signOut()

    fun isLoggedIn() = firebase.currentUserId() != null
}

@Singleton
class BookingRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val bookingDao: BookingDao,
    private val flightDao: FlightDao
) {
    suspend fun getBooking(reference: String, lastName: String): Booking? {
        return firebase.getBookingByReference(reference, lastName)
    }
}

@Singleton
class SeatRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val seatDao: SeatDao
) {
    suspend fun getSeats(flightId: String): List<Seat> {
        val remote = firebase.getSeats(flightId)
        // cache locally
        seatDao.upsertAll(remote.map {
            com.airline.checkin.data.local.entity.SeatEntity(
                id = it.id,
                flightId = it.flightId,
                seatNumber = it.seatNumber,
                type = it.type.name,
                isOccupied = it.isOccupied
            )
        })
        return remote
    }

    suspend fun selectSeat(seatId: String) {
        firebase.selectSeat(seatId)
        seatDao.markOccupied(seatId)
    }
}

@Singleton
class BoardingPassRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val boardingPassDao: BoardingPassDao
) {
    suspend fun getBoardingPass(bookingId: String): BoardingPass? {
        // Try local cache first
        val cached = boardingPassDao.getByBooking(bookingId)
        if (cached != null) return BoardingPass(
            id = cached.id,
            bookingId = cached.bookingId,
            passengerId = cached.passengerId,
            flightNumber = cached.flightNumber,
            seatNumber = cached.seatNumber,
            gate = cached.gate,
            boardingTime = cached.boardingTime,
            qrCode = cached.qrCode
        )
        // Fetch from Firebase
        return firebase.getBoardingPass(bookingId)
    }
}
