package com.airline.checkin.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun seedIfNeeded() {
        val existingBooking = firestore.collection("bookings")
            .whereEqualTo("reference", "ABCDEF")
            .get()
            .await()
        if (!existingBooking.isEmpty) return

        val flightRef = firestore.collection("flights").document("FL001")

        val batch = firestore.batch()

        batch.set(
            flightRef,
            mapOf(
                "flight_number" to "IDN16821",
                "origin" to "CGK",
                "destination" to "DPS",
                "departureTime" to "16:55",
                "arrivalTime" to "18:45",
                "status" to "On Time",
                "dateKey" to "2026-06-01",
                "routeKey" to "CGK_DPS",
                "aircraftType" to "A320",
                "aircraftId" to "A320",
                "airline" to "Airline Asia",
                "stops" to 0,
                "checkedBagsIncluded" to 1,
                "carryOnIncluded" to 1,
                "emissionsKg" to 150,
                "price" to 120,
                "currency" to "USD",
                "durationMinutes" to 110,
                "pricingSummary" to mapOf(
                    "ECONOMY" to mapOf("price" to 120, "seatsAvailable" to 120),
                    "PREMIUM_ECONOMY" to mapOf("price" to 168, "seatsAvailable" to 24),
                    "BUSINESS" to mapOf("price" to 300, "seatsAvailable" to 12),
                    "meta" to mapOf("currency" to "USD", "carryOnIncluded" to 1)
                )
            )
        )

        batch.set(
            firestore.collection("bookings").document("booking_abc123"),
            mapOf(
                "reference" to "ABCDEF",
                "lastName" to "Smith",
                "passengerName" to "John Smith",
                "flightId" to "FL001",
                "passengerId" to "mock-user-001",
                "checkInStatus" to false,
                "ticketsCount" to 1,
                "totalPrice" to 120,
                "currency" to "USD",
                "paymentStatus" to "PAID",
                "cabinClass" to "ECONOMY"
            )
        )

        batch.set(
            firestore.collection("bookings").document("booking_xyz789"),
            mapOf(
                "reference" to "XYZABC",
                "lastName" to "Doe",
                "passengerName" to "Jane Doe",
                "flightId" to "FL001",
                "passengerId" to "mock-user-002",
                "checkInStatus" to false,
                "ticketsCount" to 1,
                "totalPrice" to 120,
                "currency" to "USD",
                "paymentStatus" to "PAID",
                "cabinClass" to "ECONOMY"
            )
        )

        val seatDocs = listOf(
            "S1" to mapOf(
                "flightId" to "FL001",
                "seatNumber" to "5A",
                "type" to "ECONOMY",
                "isOccupied" to false
            ),
            "S2" to mapOf(
                "flightId" to "FL001",
                "seatNumber" to "5B",
                "type" to "ECONOMY",
                "isOccupied" to true
            ),
            "S3" to mapOf(
                "flightId" to "FL001",
                "seatNumber" to "5C",
                "type" to "ECONOMY",
                "isOccupied" to false
            )
        )

        seatDocs.forEach { (id, data) ->
            batch.set(firestore.collection("seats").document(id), data)
        }

        batch.commit().await()
    }
}
