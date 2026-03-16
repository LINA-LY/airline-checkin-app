package com.airline.checkin.data.remote.firebase

import com.airline.checkin.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    // ---- Auth ----
    suspend fun signIn(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun register(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    fun signOut() = auth.signOut()

    fun currentUserId() = auth.currentUser?.uid

    // ---- Bookings ----
    suspend fun getBookingByReference(ref: String, lastName: String): Booking? {
        val snapshot = firestore.collection("bookings")
            .whereEqualTo("reference", ref)
            .get().await()
        return snapshot.documents.firstOrNull()?.toObject(Booking::class.java)
    }

    // ---- Flights ----
    suspend fun getFlight(flightId: String): Flight? =
        firestore.collection("flights")
            .document(flightId).get().await()
            .toObject(Flight::class.java)

    // ---- Seats ----
    suspend fun getSeats(flightId: String): List<Seat> {
        val snapshot = firestore.collection("seats")
            .whereEqualTo("flightId", flightId).get().await()
        return snapshot.toObjects(Seat::class.java)
    }

    suspend fun selectSeat(seatId: String) {
        firestore.collection("seats")
            .document(seatId)
            .update("isOccupied", true).await()
    }

    // ---- Boarding Pass ----
    suspend fun getBoardingPass(bookingId: String): BoardingPass? {
        val snapshot = firestore.collection("boarding_passes")
            .whereEqualTo("bookingId", bookingId).get().await()
        return snapshot.documents.firstOrNull()?.toObject(BoardingPass::class.java)
    }

    // ---- Check-in ----
    suspend fun submitCheckIn(bookingId: String, baggage: BaggageDeclaration) {
        firestore.collection("bookings")
            .document(bookingId)
            .update("checkInStatus", true).await()

        firestore.collection("baggage_declarations")
            .add(baggage).await()
    }
}
