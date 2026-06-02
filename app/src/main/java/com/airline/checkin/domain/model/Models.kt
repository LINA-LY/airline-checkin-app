package com.airline.checkin.domain.model

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = ""
)

data class Airport(
    val code: String = "",
    val name: String = "",
    val city: String = "",
    val country: String = ""
)

data class PassengerDocument(
    val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val docType: String = "",
    val docNumber: String = "",
    val nationality: String = "",
    val dateOfBirth: String = "",
    val gender: String = ""
)

data class PassportData(
    val number: String = "",
    val dob: String = "",
    val nationality: String = ""
)

data class BaggageData(
    val cabin: Int = 0,
    val checked: Int = 0
)

data class SpecialRequests(
    val dietary: String = "",
    val wheelchair: Boolean = false,
    val infant: Boolean = false,
    val pet: Boolean = false
)

data class SeatData(
    val seatId: String = "",
    val seatNumber: String = ""
)

data class Booking(
    val id: String = "",
    val lastName: String = "",
    val firstName: String = "",
    val flightNumber: String = "",
    val departure: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val checkInStatus: Boolean = false,
    val seat: SeatData? = null,
    val passport: PassportData? = null,
    val baggage: BaggageData? = null,
    val specialRequests: SpecialRequests? = null,
    val userId: String = "",
    val flightId: String = "",
    val passengerName: String = "",
    val cabinClass: String = ""
)

data class Flight(
    val id: String = "",
    val flightNumber: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val dateKey: String = "",
    val aircraftId: String = "",
    val currency: String = "USD"
)

data class Passenger(
    val id: String = "",
    val passportNumber: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val nationality: String = "",
    val gender: String = ""
)

data class BaggageDeclaration(
    val id: String = "",
    val cabinBags: Int = 0,
    val checkedBags: Int = 0
)

data class Seat(
    val id: String = "",
    val seatNumber: String = "",
    val isOccupied: Boolean = false,
    val flightId: String = ""
)
