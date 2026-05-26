package com.airline.checkin.data.remote.firebase

import com.airline.checkin.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    data class GoogleAuthResult(
        val userId: String?,
        val email: String?,
        val isNewUser: Boolean
    )

    private val bookings = firestore.collection("bookings")
    private val flights = firestore.collection("flights")
    private val seats = firestore.collection("seats")
    private val boardingPasses = firestore.collection("boarding_passes")
    private val baggageDeclarations = firestore.collection("baggage_declarations")
    private val users = firestore.collection("users")

    // ---- Auth ----
    suspend fun signIn(email: String, password: String): Boolean {
        auth.signInWithEmailAndPassword(email, password).await()
        return auth.currentUser != null
    }

    suspend fun register(email: String, password: String): Boolean {
        auth.createUserWithEmailAndPassword(email, password).await()
        return auth.currentUser != null
    }

    suspend fun registerWithProfile(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String
    ): Boolean {
        auth.createUserWithEmailAndPassword(email, password).await()
        val saved = saveUserProfile(firstName, lastName, phone)
        return auth.currentUser != null && saved
    }

    suspend fun signInWithGoogle(idToken: String): GoogleAuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return GoogleAuthResult(
            userId = result.user?.uid,
            email = result.user?.email,
            isNewUser = result.additionalUserInfo?.isNewUser == true
        )
    }

    suspend fun saveUserProfile(
        firstName: String,
        lastName: String,
        phone: String
    ): Boolean {
        val user = auth.currentUser ?: return false
        val fullName = listOf(firstName.trim(), lastName.trim()).joinToString(" ").trim()
        val payload = mapOf(
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim(),
            "fullName" to fullName,
            "phone" to phone.trim(),
            "email" to (user.email ?: "")
        )
        users.document(user.uid).set(payload, SetOptions.merge()).await()
        return true
    }

    suspend fun isProfileComplete(userId: String): Boolean {
        val doc = users.document(userId).get().await()
        if (!doc.exists()) return false
        val firstName = doc.getString("firstName") ?: ""
        val lastName = doc.getString("lastName") ?: ""
        val phone = doc.getString("phone") ?: ""
        return firstName.isNotBlank() && lastName.isNotBlank() && phone.isNotBlank()
    }

    fun signOut() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    // ---- Bookings ----
    suspend fun getBookingByReference(ref: String, lastName: String): Booking? {
        if (ref.isBlank()) return null
        val baseQuery = bookings.whereEqualTo("reference", ref)
        val snapshot = if (lastName.isNotBlank()) {
            val withLastName = baseQuery.whereEqualTo("lastName", lastName).get().await()
            if (withLastName.isEmpty) baseQuery.get().await() else withLastName
        } else {
            baseQuery.get().await()
        }
        val doc = snapshot.documents.firstOrNull() ?: return null
        return doc.toBooking()
    }

    // ---- Flights ----
    suspend fun getFlight(flightId: String): Flight? {
        if (flightId.isBlank()) return null
        val doc = flights.document(flightId).get().await()
        if (!doc.exists()) return null
        return doc.toFlight()
    }

    // ---- Seats ----
    suspend fun getSeats(flightId: String): List<Seat> {
        if (flightId.isBlank()) return emptyList()
        val snapshot = seats.whereEqualTo("flightId", flightId).get().await()
        return snapshot.documents.mapNotNull { it.toSeat() }
    }

    suspend fun selectSeat(seatId: String) {
        if (seatId.isBlank()) return
        seats.document(seatId).update("isOccupied", true).await()
    }

    // ---- Boarding Pass ----
    suspend fun getBoardingPass(bookingId: String): BoardingPass? {
        if (bookingId.isBlank()) return null
        val snapshot = boardingPasses.whereEqualTo("bookingId", bookingId)
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return doc.toBoardingPass()
    }

    // ---- Check-in ----
    suspend fun submitCheckIn(bookingId: String, baggage: BaggageDeclaration) {
        if (bookingId.isBlank()) return
        bookings.document(bookingId)
            .set(mapOf("checkInStatus" to true), SetOptions.merge())
            .await()

        val baggageId = if (baggage.id.isNotBlank()) baggage.id else bookingId
        baggageDeclarations.document(baggageId)
            .set(
                mapOf(
                    "bookingId" to bookingId,
                    "cabinBags" to baggage.cabinBags,
                    "checkedBags" to baggage.checkedBags,
                    "specialItems" to baggage.specialItems
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun DocumentSnapshot.toBooking(): Booking = Booking(
        id = id,
        reference = getString("reference") ?: "",
        flightId = getString("flightId") ?: "",
        passengerId = getString("passengerId") ?: "",
        checkInStatus = getBoolean("checkInStatus") ?: false
    )

    private fun DocumentSnapshot.toFlight(): Flight = Flight(
        id = id,
        flightNumber = getString("flight_number") ?: getString("flightNumber") ?: "",
        origin = getString("origin") ?: "",
        destination = getString("destination") ?: "",
        departureTime = getString("departureTime") ?: "",
        arrivalTime = getString("arrivalTime") ?: "",
        status = getString("status") ?: ""
    )

    private fun DocumentSnapshot.toSeat(): Seat? {
        val flightId = getString("flightId") ?: return null
        val seatNumber = getString("seatNumber") ?: return null
        val typeRaw = getString("type") ?: SeatType.ECONOMY.name
        val seatType = runCatching { SeatType.valueOf(typeRaw.uppercase()) }
            .getOrDefault(SeatType.ECONOMY)
        return Seat(
            id = id,
            flightId = flightId,
            seatNumber = seatNumber,
            type = seatType,
            isOccupied = getBoolean("isOccupied") ?: false
        )
    }

    private fun DocumentSnapshot.toBoardingPass(): BoardingPass = BoardingPass(
        id = id,
        bookingId = getString("bookingId") ?: "",
        passengerId = getString("passengerId") ?: "",
        flightNumber = getString("flightNumber") ?: "",
        seatNumber = getString("seatNumber") ?: "",
        gate = getString("gate") ?: "",
        boardingTime = getString("boardingTime") ?: "",
        qrCode = getString("qrCode") ?: "",
        isDownloaded = getBoolean("isDownloaded") ?: false
    )
}