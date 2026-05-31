package com.airline.checkin.data.repository

import com.airline.checkin.data.local.dao.*
import com.airline.checkin.data.remote.firebase.FirebaseService
import com.airline.checkin.domain.model.*
import android.util.Log
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AuthResult(
    val isSuccess: Boolean,
    val requiresProfile: Boolean
)

@Singleton
class AuthRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val userDao: UserDao
) {
    suspend fun signIn(email: String, password: String) {
        firebase.signIn(email, password)
        cacheUserLocally()
    }

    suspend fun registerWithProfile(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String
    ) {
        firebase.registerWithProfile(email, password, firstName, lastName, phone)
        cacheUserLocally()
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        val result = firebase.signInWithGoogle(idToken)
        val userId = result.userId
        if (userId.isNullOrBlank()) {
            return AuthResult(isSuccess = false, requiresProfile = false)
        }
        val needsProfile = result.isNewUser || !firebase.isProfileComplete(userId)
        if (!needsProfile) cacheUserLocally()
        return AuthResult(isSuccess = true, requiresProfile = needsProfile)
    }

    suspend fun saveUserProfile(firstName: String, lastName: String, phone: String) {
        firebase.saveUserProfile(firstName, lastName, phone)
        cacheUserLocally()
    }

    fun signOut() {
        firebase.signOut()
        // Note: we intentionally do NOT clear the Room cache here;
        // the user table is cleared on next login by upsert.
    }

    fun isLoggedIn() = firebase.currentUserId() != null

    /** Returns the display name, trying Firestore first then falling back to Room cache */
    suspend fun getUserDisplayName(): String? {
        // Try remote first
        val remote = try { firebase.getUserDisplayName() } catch (_: Exception) { null }
        if (!remote.isNullOrBlank()) return remote
        // Fallback to local cache
        val uid = firebase.currentUserId() ?: return null
        return userDao.getById(uid)?.fullName?.takeIf { it.isNotBlank() }
    }

    /** Cache the current user's profile into Room for offline access */
    suspend fun cacheUserLocally() {
        val profile = try { firebase.getUserProfile() } catch (_: Exception) { null }
        if (profile != null) {
            userDao.upsert(
                com.airline.checkin.data.local.entity.UserEntity(
                    id = profile.id,
                    fullName = profile.fullName,
                    email = profile.email,
                    phone = profile.phone
                )
            )
        }
    }
}

@Singleton
class BookingRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val bookingDao: BookingDao,
    private val flightDao: FlightDao
) {
    suspend fun getBookingById(bookingId: String): Booking? {
        return firebase.getBookingById(bookingId)
    }

    suspend fun getBooking(reference: String, lastName: String): Booking? {
        return firebase.getBookingByReference(reference, lastName)
    }

    suspend fun submitCheckIn(bookingId: String, baggage: BaggageDeclaration) {
        firebase.submitCheckIn(bookingId, baggage)
        bookingDao.markCheckedIn(bookingId)
    }

    fun observeUserBookings(userId: String): kotlinx.coroutines.flow.Flow<List<Booking>> {
        return bookingDao.getByUserFlow(userId).map { list ->
            list.map { entity ->
                Booking(
                    id = entity.id,
                    reference = entity.reference,
                    flightId = entity.flightId,
                    passengerId = entity.passengerId,
                    checkInStatus = entity.checkInStatus,
                    ticketsCount = 1,
                    totalPrice = 0,
                    currency = "USD",
                    paymentStatus = "PAID"
                )
            }
        }
    }

    suspend fun refreshUserBookings(userId: String) {
        val remote = firebase.getUserBookings(userId)
        remote.forEach { booking ->
            bookingDao.upsert(
                com.airline.checkin.data.local.entity.BookingEntity(
                    id = booking.id,
                    reference = booking.reference,
                    flightId = booking.flightId,
                    passengerId = booking.passengerId,
                    checkInStatus = booking.checkInStatus
                )
            )
        }
    }
}

@Singleton
class FlightRepository @Inject constructor(
    private val firebase: FirebaseService
) {
    suspend fun getFlight(flightId: String): Flight? {
        return firebase.getFlight(flightId)
    }
}

@Singleton
class DocumentRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val savedPassengerDao: SavedPassengerDao
) {
    suspend fun getPassengerDocuments(userId: String): List<PassengerDocument> {
        return firebase.getPassengerDocuments(userId)
    }

    suspend fun createPassengerDocument(doc: PassengerDocument): PassengerDocument {
        return firebase.createPassengerDocument(doc)
    }

    fun observeSavedPassengers(userId: String): kotlinx.coroutines.flow.Flow<List<PassengerDocument>> {
        return savedPassengerDao.getAll(userId).map { list ->
            list.map { entity ->
                PassengerDocument(
                    id = entity.id,
                    userId = entity.userId,
                    firstName = entity.firstName,
                    lastName = entity.lastName,
                    fullName = entity.fullName,
                    docType = entity.docType,
                    docNumber = entity.docNumber,
                    nationality = entity.nationality,
                    dateOfBirth = entity.dateOfBirth
                )
            }
        }
    }

    suspend fun saveTravelerLocally(doc: PassengerDocument) {
        savedPassengerDao.upsert(
            com.airline.checkin.data.local.entity.SavedPassengerEntity(
                id = doc.id,
                userId = doc.userId,
                firstName = doc.firstName,
                lastName = doc.lastName,
                fullName = doc.fullName,
                docType = doc.docType,
                docNumber = doc.docNumber,
                nationality = doc.nationality,
                dateOfBirth = doc.dateOfBirth,
                syncedToRemote = false
            )
        )
    }

    suspend fun syncUnsyncedToFirestore(userId: String) {
        val unsynced = savedPassengerDao.getUnsynced(userId)
        for (entity in unsynced) {
            try {
                val doc = PassengerDocument(
                    id = entity.id,
                    userId = entity.userId,
                    firstName = entity.firstName,
                    lastName = entity.lastName,
                    fullName = entity.fullName,
                    docType = entity.docType,
                    docNumber = entity.docNumber,
                    nationality = entity.nationality,
                    dateOfBirth = entity.dateOfBirth
                )
                firebase.createPassengerDocument(doc)
                savedPassengerDao.upsert(entity.copy(syncedToRemote = true))
            } catch (e: Exception) {
                Log.e("DocumentRepository", "Failed to sync traveler \${entity.id}", e)
            }
        }
    }
}

@Singleton
class SeatRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val seatDao: SeatDao
) {
    suspend fun getSeats(flightId: String): List<Seat> {
        val remote = firebase.getSeats(flightId)
        // cache locally
        seatDao.upsertAll(remote.map {
            com.airline.checkin.data.local.entity.SeatEntity(
                id = it.id,
                flightId = it.flightId,
                seatNumber = it.seatNumber,
                type = it.type.name,
                isOccupied = it.isOccupied
            )
        })
        return remote
    }

    suspend fun selectSeat(seatId: String) {
        firebase.selectSeat(seatId)
        seatDao.markOccupied(seatId)
    }
}

@Singleton
class BoardingPassRepository @Inject constructor(
    private val firebase: FirebaseService,
    private val boardingPassDao: BoardingPassDao
) {
    suspend fun getBoardingPass(bookingId: String): BoardingPass? {
        // Try local cache first
        val cached = boardingPassDao.getByBooking(bookingId)
        if (cached != null) return BoardingPass(
            id = cached.id,
            bookingId = cached.bookingId,
            passengerId = cached.passengerId,
            passengerName = cached.passengerName,
            flightNumber = cached.flightNumber,
            seatNumber = cached.seatNumber,
            gate = cached.gate,
            boardingTime = cached.boardingTime,
            qrCode = cached.qrCode,
            isDownloaded = cached.isDownloaded,
            origin = cached.origin,
            destination = cached.destination
        )
        // Fetch from Firebase
        val remote = firebase.getBoardingPass(bookingId)
        if (remote != null) {
            boardingPassDao.upsert(
                com.airline.checkin.data.local.entity.BoardingPassEntity(
                    id = remote.id,
                    bookingId = remote.bookingId,
                    passengerId = remote.passengerId,
                    passengerName = remote.passengerName,
                    flightNumber = remote.flightNumber,
                    seatNumber = remote.seatNumber,
                    gate = remote.gate,
                    boardingTime = remote.boardingTime,
                    qrCode = remote.qrCode,
                    isDownloaded = remote.isDownloaded,
                    origin = remote.origin,
                    destination = remote.destination
                )
            )
        }
        return remote
    }
}
