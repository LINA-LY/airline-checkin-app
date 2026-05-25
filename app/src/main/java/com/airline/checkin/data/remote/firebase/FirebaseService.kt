package com.airline.checkin.data.remote.firebase

import com.airline.checkin.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor() {

    // ---- Auth ----
    suspend fun signIn(email: String, password: String): Boolean {
        return email == "test@test.com" && password == "123456"
    }

    suspend fun register(email: String, password: String): Boolean {
        return true
    }

    fun signOut() { /* no-op */ }

    fun currentUserId(): String? = "mock-user-001"

    // ---- Bookings ----
    suspend fun getBookingByReference(ref: String, lastName: String): Booking? {
        if (ref.isBlank() || lastName.isBlank()) return null
        return Booking(
            id = "B001",
            reference = ref,
            flightId = "FL001",
            passengerId = "P001",
            checkInStatus = false
        )
    }

    // ---- Flights ----
    suspend fun getFlight(flightId: String): Flight? {
        return Flight(
            id = "FL001",
            flightNumber = "IDN16821",
            origin = "CGK",
            destination = "DPS",
            departureTime = "16:55",
            arrivalTime = "20:30",
            status = "On Time"
        )
    }

    // ---- Seats ----
    suspend fun getSeats(flightId: String): List<Seat> {
        return listOf(
            Seat(id = "S1", flightId = flightId, seatNumber = "5B",
                type = SeatType.ECONOMY, isOccupied = false),
            Seat(id = "S2", flightId = flightId, seatNumber = "5C",
                type = SeatType.ECONOMY, isOccupied = true),
            Seat(id = "S3", flightId = flightId, seatNumber = "5D",
                type = SeatType.ECONOMY, isOccupied = false)
        )
    }

    suspend fun selectSeat(seatId: String) { /* no-op */ }

    // ---- Boarding Pass ----
    suspend fun getBoardingPass(bookingId: String): BoardingPass? {
        return BoardingPass(
            id = "BP001",
            bookingId = bookingId,
            passengerId = "P001",
            flightNumber = "IDN16821",
            seatNumber = "5B",
            gate = "12",
            boardingTime = "16:55",
            qrCode = "MOCK_QR_${bookingId}",
            isDownloaded = false
        )
    }

    // ---- Check-in ----
    suspend fun submitCheckIn(bookingId: String, baggage: BaggageDeclaration) {
        // Mock: pretend it worked
    }
}