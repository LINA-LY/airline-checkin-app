package com.airline.checkin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String,
    val phone: String
)

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val status: String
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val reference: String,
    val flightId: String,
    val passengerId: String,
    val checkInStatus: Boolean
)

@Entity(tableName = "passengers")
data class PassengerEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val fullName: String,
    val passportNumber: String,
    val dateOfBirth: String,
    val nationality: String
)

@Entity(tableName = "seats")
data class SeatEntity(
    @PrimaryKey val id: String,
    val flightId: String,
    val seatNumber: String,
    val type: String,
    val isOccupied: Boolean
)

@Entity(tableName = "boarding_passes")
data class BoardingPassEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val passengerId: String,
    val flightNumber: String,
    val seatNumber: String,
    val gate: String,
    val boardingTime: String,
    val qrCode: String,
    val isDownloaded: Boolean
)

@Entity(tableName = "baggage_declarations")
data class BaggageDeclarationEntity(
    @PrimaryKey val id: String,
    val bookingId: String,
    val cabinBags: Int,
    val checkedBags: Int,
    val specialItems: String
)
