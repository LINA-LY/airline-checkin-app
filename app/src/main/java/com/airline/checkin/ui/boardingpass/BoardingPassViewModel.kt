// FILE: app/src/main/java/com/airline/checkin/ui/boardingpass/BoardingPassViewModel.kt
package com.airline.checkin.ui.boardingpass

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.BookingRepository
import com.airline.checkin.data.repository.FlightRepository
import com.airline.checkin.domain.model.Booking
import com.airline.checkin.domain.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardingPassUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val flight: Flight? = null,
    val qrCodeBitmap: Bitmap? = null,
    val qrCodePayload: String = "",
    val error: String? = null
)

@HiltViewModel
class BoardingPassViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val flightRepository: FlightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardingPassUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBoardingPass(bookingId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val booking = bookingRepository.getBookingById(bookingId)

                if (booking != null) {
                    val flight = flightRepository.getFlight(booking.flightId)

                    val fullName = listOf(booking.firstName, booking.lastName)
                        .joinToString(" ")
                        .trim()
                    val seatNumber = booking.seat?.seatNumber.orEmpty()
                    val qrPayload = "${booking.id}|$fullName|$seatNumber"
                    val qrBitmap = QrCodeGenerator.generateQrCodeBitmap(qrPayload)

                    _uiState.value = _uiState.value.copy(
                        booking = booking,
                        flight = flight,
                        qrCodePayload = qrPayload,
                        qrCodeBitmap = qrBitmap
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Booking not found for reference: $bookingId"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An unexpected error occurred"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}