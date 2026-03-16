package com.airline.checkin.ui.checkin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.BookingRepository
import com.airline.checkin.domain.model.Booking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- ViewModel ----

data class CheckInUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val error: String? = null
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState = _uiState.asStateFlow()

    fun lookupBooking(reference: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = CheckInUiState(isLoading = true)
            try {
                val booking = bookingRepository.getBooking(reference, lastName)
                _uiState.value = CheckInUiState(booking = booking)
            } catch (e: Exception) {
                _uiState.value = CheckInUiState(error = e.message)
            }
        }
    }
}

// ---- Booking Lookup Screen ----

@Composable
fun BookingLookupScreen(
    onBookingFound: (bookingId: String) -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    // TODO Member 2: implement booking lookup UI
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Flight Lookup", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onBookingFound("mock_booking_id") }) {
            Text("Find Booking (placeholder)")
        }
    }
}

// ---- Check-In Flow Screen ----

@Composable
fun CheckInScreen(
    bookingId: String,
    onGoToSeat: (flightId: String) -> Unit,
    onDone: () -> Unit
) {
    // TODO Member 2: implement multi-step check-in flow
    // Steps: passport scan -> details review -> baggage -> special requests
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Check-In Flow", style = MaterialTheme.typography.headlineMedium)
        Text("Booking: $bookingId")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onGoToSeat("mock_flight_id") }) { Text("Select Seat") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone) { Text("Complete Check-In") }
    }
}
