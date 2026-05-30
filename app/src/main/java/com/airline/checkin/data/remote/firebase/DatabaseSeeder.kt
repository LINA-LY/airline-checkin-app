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
        val flightRef = firestore.collection("flights").document("FL001")
        val existing = flightRef.get().await()
        if (existing.exists()) return

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
            firestore.collection("bookings").document("mock_booking_id"),
            mapOf(
                "reference" to "ABCDEF",
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

        batch.set(
            firestore.collection("boarding_passes").document("BP001"),
            mapOf(
                "bookingId" to "mock_booking_id",
                "passengerId" to "mock-user-001",
                "flightNumber" to "IDN16821",
                "seatNumber" to "5B",
                "gate" to "12",
                "boardingTime" to "16:55",
                "qrCode" to "PNR: mock_booking_id | Flight: IDN16821 | Seat: 5B",
                "isDownloaded" to false
            )
        )

        batch.commit().await()
    }
}
