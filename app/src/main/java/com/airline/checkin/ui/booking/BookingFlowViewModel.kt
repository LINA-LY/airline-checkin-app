package com.airline.checkin.ui.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.domain.model.BookingPassenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassengerDraft(
    val index: Int,
    val passengerId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val passengerName: String = "",
    val seatId: String = "",
    val seatNumber: String = "",
    val cabinClass: String = ""
)

data class BookingDraft(
    val flightId: String = "",
    val passengersCount: Int = 1,
    val cabinClass: String = "ECONOMY",
    val pricePerPerson: Int = 0,
    val passengerDrafts: List<PassengerDraft> = emptyList()
)

@HiltViewModel
class BookingFlowViewModel @Inject constructor() : ViewModel() {
    private val _draft = MutableStateFlow(BookingDraft())
    val draft = _draft.asStateFlow()

    fun startFlow(flightId: String, passengers: Int, cabin: String, pricePerPerson: Int) {
        viewModelScope.launch {
            val drafts = (0 until passengers).map { idx ->
                PassengerDraft(index = idx, cabinClass = cabin)
            }
            _draft.value = BookingDraft(
                flightId = flightId,
                passengersCount = passengers,
                cabinClass = cabin,
                pricePerPerson = pricePerPerson,
                passengerDrafts = drafts
            )
        }
    }

    fun addPassenger() {
        val current = _draft.value
        val newCount = current.passengersCount + 1
        val newDraft = PassengerDraft(index = newCount - 1, cabinClass = current.cabinClass)
        _draft.value = current.copy(
            passengersCount = newCount,
            passengerDrafts = current.passengerDrafts + newDraft
        )
    }

    fun removePassenger(index: Int) {
        val current = _draft.value
        if (current.passengersCount <= 1) return
        val updated = current.passengerDrafts.filterNot { it.index == index }
            .mapIndexed { i, p -> p.copy(index = i) }
        _draft.value = current.copy(
            passengersCount = updated.size,
            passengerDrafts = updated
        )
    }

    fun setPassengerDocument(index: Int, document: com.airline.checkin.domain.model.PassengerDocument) {
        val current = _draft.value
        val updated = current.passengerDrafts.map {
            if (it.index == index) {
                it.copy(
                    passengerId = document.id,
                    passengerName = document.fullName.ifBlank {
                        listOf(document.firstName, document.lastName).filter { part -> part.isNotBlank() }.joinToString(" ")
                    },
                    firstName = document.firstName.ifBlank {
                        document.fullName.trim().split(Regex("\\s+"), limit = 2).getOrNull(0).orEmpty()
                    },
                    lastName = document.lastName.ifBlank {
                        document.fullName.trim().split(Regex("\\s+"), limit = 2).getOrNull(1).orEmpty()
                    }
                )
            } else {
                it
            }
        }
        _draft.value = current.copy(passengerDrafts = updated)
    }

    fun setPassengerCabin(index: Int, cabinClass: String) {
        val current = _draft.value
        val updated = current.passengerDrafts.map {
            if (it.index == index) it.copy(cabinClass = cabinClass) else it
        }
        _draft.value = current.copy(passengerDrafts = updated)
    }

    fun setPassengerName(index: Int, firstName: String, lastName: String) {
        val current = _draft.value
        val updated = current.passengerDrafts.map {
            if (it.index == index) {
                val combined = listOf(firstName.trim(), lastName.trim()).filter { part -> part.isNotBlank() }.joinToString(" ")
                it.copy(firstName = firstName, lastName = lastName, passengerName = combined)
            } else {
                it
            }
        }
        _draft.value = current.copy(passengerDrafts = updated)
    }

    fun clearPassenger(index: Int) {
        val current = _draft.value
        val updated = current.passengerDrafts.map {
            if (it.index == index) {
                it.copy(
                    passengerId = "",
                    firstName = "",
                    lastName = "",
                    passengerName = "",
                    seatId = "",
                    seatNumber = ""
                )
            } else {
                it
            }
        }
        _draft.value = current.copy(passengerDrafts = updated)
    }

    fun setSeatForPassenger(index: Int, seatId: String, seatNumber: String) {
        val current = _draft.value
        val updated = current.passengerDrafts.map {
            if (it.index == index) it.copy(seatId = seatId, seatNumber = seatNumber) else it
        }
        _draft.value = current.copy(passengerDrafts = updated)
    }

    fun toBookingPassengers(): List<BookingPassenger> {
        return _draft.value.passengerDrafts.map { d ->
            BookingPassenger(
                id = "",
                bookingId = "",
                passengerId = d.passengerId,
                firstName = d.firstName,
                lastName = d.lastName,
                passengerName = d.passengerName,
                seatId = d.seatId,
                seatNumber = d.seatNumber,
                cabinClass = d.cabinClass
            )
        }
    }
}
