// app/src/main/java/com/airline/checkin/data/repository/Repositories.kt
package com.airline.checkin.data.repository

import com.airline.checkin.data.local.dao.*
import com.airline.checkin.data.remote.firebase.FirebaseService
import com.airline.checkin.domain.model.*
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
    }

    fun isLoggedIn() = firebase.currentUserId() != null

    fun getCurrentUserId(): String? = firebase.currentUserId()

    suspend fun getUserDisplayName(): String? {
        val remote = try { firebase.getUserDisplayName() } catch (_: Exception) { null }
        if (!remote.isNullOrBlank()) return remote
        val uid = firebase.currentUserId() ?: return null
        return userDao.getById(uid)?.fullName?.takeIf { it.isNotBlank() }
    }

    // FIXED: Expose user profile details to view models, loading locally first
    suspend fun getUserProfile(): User? {
        val uid = firebase.currentUserId() ?: return null
        val local = userDao.getById(uid)
        if (local != null) {
            return User(
                id = local.id,
                fullName = local.fullName,
                email = local.email,
                phone = local.phone
            )
        }
        val remote = try { firebase.getUserProfile() } catch (_: Exception) { null }
        if (remote != null) {
            cacheUserLocally()
            return remote
        }
        return null
    }

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
    private val bookingDao: BookingDao
) {
    suspend fun getBookingById(bookingId: String): Booking? {
        val local = bookingDao.getById(bookingId)?.toDomain()
        return try {
            val remote = firebase.getBookingById(bookingId)
            if (remote != null) {
                bookingDao.upsert(remote.toEntity())
                remote
            } else {
                local
            }
        } catch (_: Exception) {
            local
        }
    }

    suspend fun getBooking(reference: String, lastName: String): Booking? {
        val remote = firebase.getBookingByReference(reference, lastName)
        if (remote != null) {
            bookingDao.upsert(remote.toEntity())
        }
        return remote
    }

    suspend fun createMockBooking(userId: String, firstName: String, lastName: String) {
        firebase.createMockBooking(userId, firstName, lastName)
    }

    suspend fun getUserBookings(userId: String): List<Booking> {
        val remote = firebase.getUserBookings(userId)
        remote.forEach { booking -> bookingDao.upsert(booking.toEntity(userId)) }
        return remote
    }

    suspend fun submitCheckIn(bookingId: String, booking: Booking) {
        // 1. Save to Room DB FIRST! So the local app instantly knows we are checked in.
        bookingDao.upsert(booking.copy(id = bookingId, checkInStatus = true).toEntity())

        // 2. Tell Firebase (it will sync in the background when the internet returns)
        firebase.submitCheckIn(bookingId, booking)
    }

    fun observeUserBookings(userId: String): kotlinx.coroutines.flow.Flow<List<Booking>> {
        return bookingDao.getByUserFlow(userId).map { list ->
            list.map { entity -> entity.toDomain() }
        }
    }

    suspend fun refreshUserBookings(userId: String) {
        val remote = firebase.getUserBookings(userId)
        remote.forEach { booking ->
            bookingDao.upsert(booking.toEntity(userId))
        }
    }
}

private fun com.airline.checkin.data.local.entity.BookingEntity.toDomain(): Booking {
    return Booking(
        id = id,
        userId = userId,
        lastName = lastName,
        firstName = firstName,
        flightNumber = flightNumber,
        departure = departure,
        destination = destination,
        departureTime = departureTime,
        checkInStatus = checkInStatus,
        seat = seat,
        passport = passport,
        baggage = baggage,
        specialRequests = specialRequests
    )
}

private fun Booking.toEntity(fallbackUserId: String = userId): com.airline.checkin.data.local.entity.BookingEntity {
    return com.airline.checkin.data.local.entity.BookingEntity(
        id = id,
        userId = userId.ifBlank { fallbackUserId },
        lastName = lastName,
        firstName = firstName,
        flightNumber = flightNumber,
        departure = departure,
        destination = destination,
        departureTime = departureTime,
        checkInStatus = checkInStatus,
        seat = seat,
        passport = passport,
        baggage = baggage,
        specialRequests = specialRequests
    )
}

@Singleton
class FlightRepository @Inject constructor() {
    suspend fun getFlight(flightId: String): Flight? {
        return Flight(
            id = flightId,
            flightNumber = "IDN16821",
            origin = "CGK",
            destination = "DPS",
            departureTime = "2026-06-05T10:00:00Z",
            arrivalTime = "2026-06-05T12:00:00Z",
            dateKey = "2026-06-05",
            aircraftId = "A320",
            currency = "USD"
        )
    }
}

@Singleton
class SeatRepository @Inject constructor() {
    suspend fun getSeats(flightId: String): List<Seat> {
        // Return some dummy occupied seats
        return listOf(
            Seat(id = "1", seatNumber = "1A", isOccupied = true, flightId = flightId),
            Seat(id = "2", seatNumber = "2C", isOccupied = true, flightId = flightId)
        )
    }
}