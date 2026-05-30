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
    val dateOfBirth: String = ""
)

data class BookingPassenger(
    val id: String = "",
    val bookingId: String = "",
    val passengerId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val passengerName: String = "",
    val seatId: String = "",
    val seatNumber: String = "",
    val cabinClass: String = "",
    val hasCheckedBags: Boolean = false,
    val hasCarryOn: Boolean = false,
    val hasPet: Boolean = false,
    val needsWheelchair: Boolean = false
)

data class CabinPricing(
    val price: Int = 0,
    val seatsAvailable: Int = 0
)

data class Flight(
    val id: String = "",
    val flightNumber: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val status: String = "",
    val airline: String = "",
    val stops: Int = 0,
    val checkedBagsIncluded: Int = 0,
    val carryOnIncluded: Int = 0,
    val emissionsKg: Int = 0,
    val price: Int = 0,
    val currency: String = "USD",
    val durationMinutes: Int = 0,
    val aircraftId: String = "",
    val pricingSummary: Map<String, CabinPricing> = emptyMap()
)

data class Booking(
    val id: String = "",
    val reference: String = "",
    val flightId: String = "",
    val passengerId: String = "",
    val checkInStatus: Boolean = false,
    val ticketsCount: Int = 1,
    val totalPrice: Int = 0,
    val currency: String = "USD",
    val paymentStatus: String = ""
)

data class BookingConfirmation(
    val id: String = "",
    val reference: String = ""
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

enum class SeatType { ECONOMY, PREMIUM, BUSINESS, FIRST }

data class BoardingPass(
    val id: String = "",
    val bookingId: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val flightNumber: String = "",
    val seatNumber: String = "",
    val gate: String = "",
    val boardingTime: String = "",
    val qrCode: String = "",
    val isDownloaded: Boolean = false,
    val origin: String = "",
    val destination: String = "",
    val cabinClass: String = ""
)

data class BaggageDeclaration(
    val id: String = "",
    val bookingId: String = "",
    val carryOnIncluded: Int = 0,
    val cabinBags: Int = 0,
    val checkedBags: Int = 0,
    val specialItems: String = ""
)

fun Flight.economyPrice(): Int {
    val summaryPrice = pricingSummary["ECONOMY"]?.price ?: pricingSummary["economy"]?.price ?: 0
    return when {
        summaryPrice > 0 -> summaryPrice
        price > 0 -> price
        else -> 120
    }
}

fun Flight.cabinPrice(cabinClass: String): Int {
    val summaryPrice = pricingSummary[cabinClass.uppercase()]?.price ?: pricingSummary[cabinClass.lowercase()]?.price ?: 0
    if (summaryPrice > 0) return summaryPrice
    
    val basePrice = economyPrice()
    val multiplier = when (cabinClass.uppercase()) {
        "PREMIUM" -> 1.2f
        "BUSINESS" -> 1.5f
        "FIRST" -> 2.0f
        else -> 1.0f
    }
    return (basePrice * multiplier).toInt()
}
