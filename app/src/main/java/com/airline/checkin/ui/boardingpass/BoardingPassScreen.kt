package com.airline.checkin.ui.boardingpass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.BoardingPassRepository
import com.airline.checkin.domain.model.BoardingPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---- ViewModel ----

@HiltViewModel
class BoardingPassViewModel @Inject constructor(
    private val repository: BoardingPassRepository
) : ViewModel() {

    private val _boardingPass = MutableStateFlow<BoardingPass?>(null)
    val boardingPass = _boardingPass.asStateFlow()

    fun load(bookingId: String) {
        viewModelScope.launch {
            _boardingPass.value = repository.getBoardingPass(bookingId)
        }
    }
}

// ---- Screen ----

@Composable
fun BoardingPassScreen(
    bookingId: String,
    viewModel: BoardingPassViewModel = hiltViewModel()
) {
    val pass by viewModel.boardingPass.collectAsState()

    LaunchedEffect(bookingId) { viewModel.load(bookingId) }

    // TODO Member 4: implement boarding pass UI with QR code
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Boarding Pass", style = MaterialTheme.typography.headlineMedium)
        pass?.let {
            Text("Flight: ${it.flightNumber}")
            Text("Seat: ${it.seatNumber}")
            Text("Gate: ${it.gate}")
        } ?: Text("Loading...")
    }
}
