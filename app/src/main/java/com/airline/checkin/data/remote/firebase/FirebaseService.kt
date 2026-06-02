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
    private val users = firestore.collection("users")
    private val passengerDocs = firestore.collection("passenger_documents")

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

    /** Fetch display name: Firestore fullName → Firebase Auth displayName → null */
    suspend fun getUserDisplayName(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = users.document(uid).get().await()
            val fullName = doc.getString("fullName")
            if (!fullName.isNullOrBlank()) fullName
            else auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
        }
    }

    /** Fetch the full user profile from Firestore */
    suspend fun getUserProfile(): com.airline.checkin.domain.model.User? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val doc = users.document(uid).get().await()
            if (!doc.exists()) return null
            com.airline.checkin.domain.model.User(
                id = uid,
                fullName = doc.getString("fullName")
                    ?: auth.currentUser?.displayName ?: "",
                email = doc.getString("email")
                    ?: auth.currentUser?.email ?: "",
                phone = doc.getString("phone") ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }

    // ---- Bookings ----
    suspend fun getBookingById(pnr: String): Booking? {
        if (pnr.isBlank()) return null
        val doc = bookings.document(pnr).get().await()
        if (!doc.exists()) return null
        return doc.toBooking()
    }

    suspend fun getBookingByReference(ref: String, lastName: String): Booking? {
        if (ref.isBlank()) return null
        // First try direct document lookup (if PNR is used as doc ID)
        val directDoc = bookings.document(ref.uppercase()).get().await()
        if (directDoc.exists()) {
            val booking = directDoc.toBooking()
            if (lastName.isBlank() || booking.lastName.equals(lastName, ignoreCase = true)) return booking
        }
        // Fallback: query by pnr field
        val snapshot = bookings
            .whereEqualTo("pnr", ref.uppercase())
            .get().await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        val booking = doc.toBooking()
        if (lastName.isNotBlank() && !booking.lastName.equals(lastName, ignoreCase = true)) return null
        return booking
    }
    suspend fun getUserBookings(userId: String): List<Booking> {
        if (userId.isBlank()) return emptyList()
        val snapshot = bookings.whereEqualTo("passengerId", userId).get().await()
        return snapshot.documents.map { it.toBooking() }
    }

    // ---- Check-in ----
    suspend fun submitCheckIn(pnr: String, booking: Booking) {
        if (pnr.isBlank()) return
        val map = mutableMapOf<String, Any>(
            "checkInStatus" to true
        )
        booking.seat?.let {
            map["seat"] = mapOf(
                "seatId" to it.seatId,
                "seatNumber" to it.seatNumber
            )
        }
        booking.passport?.let {
            map["passport"] = mapOf(
                "number" to it.number,
                "dob" to it.dob,
                "nationality" to it.nationality
            )
        }
        booking.baggage?.let {
            map["baggage"] = mapOf(
                "cabin" to it.cabin,
                "checked" to it.checked
            )
        }
        booking.specialRequests?.let {
            map["specialRequests"] = mapOf(
                "dietary" to it.dietary,
                "wheelchair" to it.wheelchair,
                "infant" to it.infant,
                "pet" to it.pet
            )
        }
        
        bookings.document(pnr).update(map)
    }

    private fun DocumentSnapshot.toBooking(): Booking {
        val seatMap = get("seat") as? Map<String, Any>
        val seatData = seatMap?.let {
            SeatData(
                seatId = it["seatId"] as? String ?: "",
                seatNumber = it["seatNumber"] as? String ?: ""
            )
        }

        val passportMap = get("passport") as? Map<String, Any>
        val passportData = passportMap?.let {
            PassportData(
                number = it["number"] as? String ?: "",
                dob = it["dob"] as? String ?: "",
                nationality = it["nationality"] as? String ?: ""
            )
        }
        
        val baggageMap = get("baggage") as? Map<String, Any>
        val baggageData = baggageMap?.let {
            BaggageData(
                cabin = (it["cabin"] as? Number)?.toInt() ?: 0,
                checked = (it["checked"] as? Number)?.toInt() ?: 0
            )
        }
        
        val specialMap = get("specialRequests") as? Map<String, Any>
        val specialRequests = specialMap?.let {
            SpecialRequests(
                dietary = it["dietary"] as? String ?: "",
                wheelchair = it["wheelchair"] as? Boolean ?: false,
                infant = it["infant"] as? Boolean ?: false,
                pet = it["pet"] as? Boolean ?: false
            )
        }

        return Booking(
            id = id,
            lastName = getString("lastName") ?: "",
            firstName = getString("firstName") ?: "",
            flightNumber = getString("flightNumber") ?: "",
            departure = getString("departure") ?: getString("origin") ?: "",
            destination = getString("destination") ?: getString("destination") ?: "",

// And add these two missing fields:
            flightId = getString("flightId") ?: getString("flight_id") ?: id,
            cabinClass = getString("cabinClass") ?: getString("cabin_class") ?: "",
            passengerName = run {
                val fn = getString("firstName") ?: ""
                val ln = getString("lastName") ?: ""
                "$fn $ln".trim()
            },
            departureTime = getString("departureTime") ?: "",
            checkInStatus = getBoolean("checkInStatus") ?: false,
            seat = seatData,
            userId = getString("passengerId") ?: "",
            passport = passportData,
            baggage = baggageData,
            specialRequests = specialRequests
        )
    }

    suspend fun createMockBooking(userId: String, firstName: String, lastName: String) {
        val pnr = generateBookingReference()
        val flightDoc = firestore.collection("flights").document("FL001").get().await()
        
        val flightNumber = flightDoc.getString("flightNumber") ?: "IDN16821"
        val departure = flightDoc.getString("departure") ?: "CGK"
        val destination = flightDoc.getString("destination") ?: "DPS"
        val departureTime = java.time.Instant.now().plusSeconds(43200).toString()
        
        val bookingData = mapOf(
            "lastName" to lastName,
            "firstName" to firstName,
            "passengerId" to userId,
            "flightNumber" to flightNumber,
            "departure" to departure,
            "destination" to destination,
            "departureTime" to departureTime,
            "checkInStatus" to false
        )
        
        bookings.document(pnr).set(bookingData).await()
    }

    private fun generateBookingReference(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}