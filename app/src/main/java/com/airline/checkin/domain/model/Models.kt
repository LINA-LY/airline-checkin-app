package com.airline.checkin.domain.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = ""
)

data class Flight(
    val id: String = "",
    val flightNumber: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val status: String = ""
)

data class Booking(
    val id: String = "",
    val reference: String = "",
    val flightId: String = "",
    val passengerId: String = "",
    val checkInStatus: Boolean = false
)

data class Passenger(
    val id: String = "",
    val bookingId: String = "",
    val fullName: String = "",
    val passportNumber: String = "",
    val dateOfBirth: String = "",
    val nationality: String = ""
)

data class Seat(
    val id: String = "",
    val flightId: String = "",
    val seatNumber: String = "",
    val type: SeatType = SeatType.ECONOMY,
    val isOccupied: Boolean = false
)

enum class SeatType { ECONOMY, PREMIUM, BUSINESS }

data class BoardingPass(
    val id: String = "",
    val bookingId: String = "",
    val passengerId: String = "",
    val flightNumber: String = "",
    val seatNumber: String = "",
    val gate: String = "",
    val boardingTime: String = "",
    val qrCode: String = "",
    val isDownloaded: Boolean = false
)

data class BaggageDeclaration(
    val id: String = "",
    val bookingId: String = "",
    val cabinBags: Int = 0,
    val checkedBags: Int = 0,
    val specialItems: String = ""
)
