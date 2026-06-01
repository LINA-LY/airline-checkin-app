package com.airline.checkin.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun seedIfNeeded() {
        val departure1 = Instant.now().plusSeconds(12 * 3600).toString()
        val departure2 = Instant.now().plusSeconds(14 * 3600).toString()

        // Force server writes, bypassing cache
        firestore.disableNetwork().await()
        firestore.enableNetwork().await()

        firestore.collection("flights").document("FL001").set(mapOf(
            "flightNumber"  to "AH1000",
            "origin"        to "ALG",
            "destination"   to "CDG",
            "departure"     to "ALG",
            "departureTime" to departure1,
            "arrivalTime"   to Instant.now().plusSeconds(16 * 3600).toString(),
            "dateKey"       to departure1.take(10)
        )).await()

        firestore.collection("bookings").document("ABCDEF").set(mapOf(
            "pnr"           to "ABCDEF",
            "firstName"     to "Alice",
            "lastName"      to "Smith",
            "passengerId"   to "",
            "flightId"      to "FL001",
            "flightNumber"  to "AH1000",
            "departure"     to "ALG",
            "destination"   to "CDG",
            "departureTime" to departure1,
            "cabinClass"    to "ECONOMY",
            "checkInStatus" to false
        )).await()

        firestore.collection("bookings").document("XYZ123").set(mapOf(
            "pnr"           to "XYZ123",
            "firstName"     to "Bob",
            "lastName"      to "Jones",
            "passengerId"   to "",
            "flightId"      to "FL001",
            "flightNumber"  to "AH1002",
            "departure"     to "ALG",
            "destination"   to "ORY",
            "departureTime" to departure2,
            "cabinClass"    to "BUSINESS",
            "checkInStatus" to false
        )).await()
    }
}