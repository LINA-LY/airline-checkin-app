package com.airline.checkin.ui.checkin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.airline.checkin.R
import com.airline.checkin.data.repository.BookingRepository
import com.airline.checkin.domain.model.*
import com.airline.checkin.ui.AppColors
import com.airline.checkin.ui.AppDimens
import com.airline.checkin.ui.auth.AppTextField
import com.airline.checkin.ui.auth.ErrorBanner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

// ─── ViewModels ───────────────────────────────────────────────────

data class CheckInUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val flight: com.airline.checkin.domain.model.Flight? = null,
    val error: String? = null,
    val currentStep: Int = 1,
    val passenger: Passenger = Passenger(),
    val selectedSeatId: String = "",
    val selectedSeatNumber: String = "",
    val baggageList: List<BaggageDeclaration> = emptyList(),
    val mealPreference: String = "",
    val needsWheelchair: Boolean = false,
    val travelingWithInfant: Boolean = false,
    val travelingWithPet: Boolean = false
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val flightRepository: com.airline.checkin.data.repository.FlightRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBookingById(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val booking = bookingRepository.getBookingById(bookingId)
                if (booking == null) { _uiState.value = _uiState.value.copy(isLoading = false, error = "Booking not found"); return@launch }
                val flight = flightRepository.getFlight(booking.flightId)
                if (flight == null) { _uiState.value = _uiState.value.copy(isLoading = false, error = "Flight not found"); return@launch }
                _uiState.value = _uiState.value.copy(isLoading = false, booking = booking, flight = flight)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun lookupBooking(reference: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = CheckInUiState(isLoading = true)
            try {
                val booking = bookingRepository.getBooking(reference, lastName)
                if (booking != null) {
                    val flight = flightRepository.getFlight(booking.flightId)
                    _uiState.value = CheckInUiState(booking = booking, flight = flight)
                } else {
                    _uiState.value = CheckInUiState(error = "No booking found with that reference and last name.")
                }
            } catch (e: Exception) {
                _uiState.value = CheckInUiState(error = e.message)
            }
        }
    }

    fun updatePassenger(passenger: Passenger) { _uiState.value = _uiState.value.copy(passenger = passenger) }
    fun updateSeat(seatId: String, seatNumber: String) { _uiState.value = _uiState.value.copy(selectedSeatId = seatId, selectedSeatNumber = seatNumber) }
    fun addBaggage(baggage: BaggageDeclaration) { _uiState.value = _uiState.value.copy(baggageList = _uiState.value.baggageList + baggage) }
    fun removeBaggage(index: Int) { _uiState.value = _uiState.value.copy(baggageList = _uiState.value.baggageList.toMutableList().also { it.removeAt(index) }) }
    fun updateSpecialRequests(meal: String = _uiState.value.mealPreference, wheelchair: Boolean = _uiState.value.needsWheelchair, infant: Boolean = _uiState.value.travelingWithInfant, pet: Boolean = _uiState.value.travelingWithPet) {
        _uiState.value = _uiState.value.copy(mealPreference = meal, needsWheelchair = wheelchair, travelingWithInfant = infant, travelingWithPet = pet)
    }

    fun submitCheckIn(canSendNotification: Boolean, onSuccess: () -> Unit) {
        val state = _uiState.value
        val bookingId = state.booking?.id ?: run { _uiState.value = _uiState.value.copy(error = "Booking not loaded"); return }

        // Validate: passenger must be 18+ (minors not eligible)
        val dobStr = state.passenger.dateOfBirth
        if (dobStr.isNotBlank()) {
            val isMinor = runCatching {
                val parts = dobStr.split("/")
                if (parts.size == 3) {
                    val year = parts[2].toInt(); val month = parts[1].toInt(); val day = parts[0].toInt()
                    val dob = java.time.LocalDate.of(year, month, day)
                    java.time.Period.between(dob, java.time.LocalDate.now()).years < 18
                } else false
            }.getOrDefault(false)
            if (isMinor) {
                _uiState.value = _uiState.value.copy(error = "Minors (under 18) are not eligible for self check-in. Please see an agent at the desk.")
                return
            }
        }

        val booking = state.booking!!
        val resolvedFirstName = state.passenger.fullName.trim().substringBefore(" ", booking.firstName).ifBlank { booking.firstName }
        val resolvedLastName  = state.passenger.fullName.trim().substringAfter(" ", booking.lastName).ifBlank { booking.lastName }

        val payload = booking.copy(
            checkInStatus = true,
            firstName = resolvedFirstName,
            lastName = resolvedLastName,
            seat = SeatData(seatId = state.selectedSeatId, seatNumber = state.selectedSeatNumber),
            passport = PassportData(number = state.passenger.passportNumber, dob = state.passenger.dateOfBirth, nationality = state.passenger.nationality),
            baggage = BaggageData(cabin = state.baggageList.sumOf { it.cabinBags }, checked = state.baggageList.sumOf { it.checkedBags }),
            specialRequests = SpecialRequests(dietary = state.mealPreference, wheelchair = state.needsWheelchair, infant = state.travelingWithInfant, pet = state.travelingWithPet)
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                bookingRepository.submitCheckIn(bookingId, payload)
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (canSendNotification) showCheckInCompleteNotification(appContext)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun nextStep() { _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1) }
    fun prevStep() { if (_uiState.value.currentStep > 1) _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1) }
}

data class BookingLookupUiState(
    val reference: String = "", val lastName: String = "",
    val isLoading: Boolean = false, val error: String? = null, val result: Booking? = null
)

@HiltViewModel
class BookingLookupViewModel @Inject constructor(private val bookingRepository: BookingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingLookupUiState())
    val uiState = _uiState.asStateFlow()

    fun reset() { _uiState.value = BookingLookupUiState() }
    fun updateReference(value: String) { _uiState.value = _uiState.value.copy(reference = value, error = null, result = null) }
    fun updateLastName(value: String) { _uiState.value = _uiState.value.copy(lastName = value, error = null, result = null) }

    fun lookup() {
        viewModelScope.launch {
            val ref = _uiState.value.reference.trim()
            val last = _uiState.value.lastName.trim()
            if (ref.isBlank()) { _uiState.value = _uiState.value.copy(error = "Please enter a booking reference."); return@launch }
            if (last.isBlank()) { _uiState.value = _uiState.value.copy(error = "Please enter your last name."); return@launch }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            try {
                val booking = bookingRepository.getBooking(ref, last)
                _uiState.value = if (booking != null) _uiState.value.copy(isLoading = false, result = booking)
                else _uiState.value.copy(isLoading = false, error = "No booking found. Check the reference and your last name.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Something went wrong. Please try again.")
            }
        }
    }
}

@HiltViewModel
class MyBookingsViewModel @Inject constructor(private val bookingRepository: BookingRepository) : ViewModel() {
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings = _bookings.asStateFlow()

    init {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotBlank()) {
            viewModelScope.launch { bookingRepository.observeUserBookings(userId).collect { _bookings.value = it } }
            viewModelScope.launch { try { bookingRepository.refreshUserBookings(userId) } catch (_: Exception) {} }
        }
    }
}

// ─── My Bookings / Passes Screen ─────────────────────────────────

@Composable
fun MyBookingsScreen(
    onNavigateToCheckIn: (String) -> Unit,
    onNavigateToBoardingPass: (String) -> Unit,
    viewModel: MyBookingsViewModel = hiltViewModel()
) {
    val allBookings by viewModel.bookings.collectAsState()

    // Filter state
    var filterCheckedIn    by remember { mutableStateOf(false) }
    var filterNotCheckedIn by remember { mutableStateOf(false) }
    var filterPast         by remember { mutableStateOf(false) }
    var filterUpcoming     by remember { mutableStateOf(false) }

    val now = remember { java.time.Instant.now() }

    val displayBookings = remember(allBookings, filterCheckedIn, filterNotCheckedIn, filterPast, filterUpcoming) {
        var list = allBookings
        if (filterCheckedIn && !filterNotCheckedIn) list = list.filter { it.checkInStatus }
        if (filterNotCheckedIn && !filterCheckedIn) list = list.filter { !it.checkInStatus }
        if (filterPast && !filterUpcoming) list = list.filter { parseInstantOrNull(it.departureTime)?.isBefore(now) == true }
        if (filterUpcoming && !filterPast) list = list.filter { parseInstantOrNull(it.departureTime)?.isAfter(now) != false }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Passes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            Text(
                "${displayBookings.size} of ${allBookings.size}",
                fontSize = 13.sp,
                color = AppColors.Gray500
            )
        }

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterPill("Checked in", filterCheckedIn) { filterCheckedIn = !filterCheckedIn }
            FilterPill("Not checked in", filterNotCheckedIn) { filterNotCheckedIn = !filterNotCheckedIn }
            FilterPill("Past", filterPast) { filterPast = !filterPast }
            FilterPill("Upcoming", filterUpcoming) { filterUpcoming = !filterUpcoming }
        }

        HorizontalDivider(color = AppColors.Gray100)

        if (displayBookings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.Gray300
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (allBookings.isEmpty()) "No passes found" else "No passes match the filter",
                        color = AppColors.Gray500,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayBookings.size) { i ->
                    val booking = displayBookings[i]
                    PassCard(
                        booking = booking,
                        onClick = {
                            if (booking.checkInStatus) onNavigateToBoardingPass(booking.id)
                            else onNavigateToCheckIn(booking.id)
                        }
                    )
                }
            }
        }
    }
}

private fun parseInstantOrNull(s: String?): java.time.Instant? {
    if (s.isNullOrBlank()) return null
    return runCatching {
        val normalized = if (!s.endsWith("Z") && !s.contains("+")) "${s}Z" else s
        java.time.Instant.parse(normalized)
    }.getOrNull()
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.radiusFull))
            .background(if (selected) AppColors.Primary else AppColors.Gray100)
            .border(1.dp, if (selected) AppColors.Primary else AppColors.Gray300, RoundedCornerShape(AppDimens.radiusFull))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else AppColors.Gray700
        )
    }
}

@Composable
private fun PassCard(booking: Booking, onClick: () -> Unit) {
    val depDisplay = remember(booking.departureTime) {
        parseInstantOrNull(booking.departureTime)?.let {
            val ldt = java.time.LocalDateTime.ofInstant(it, java.time.ZoneId.systemDefault())
            ldt.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
        } ?: "—"
    }
    val isPast = remember(booking.departureTime) {
        parseInstantOrNull(booking.departureTime)?.isBefore(java.time.Instant.now()) == true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusLarge))
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, RoundedCornerShape(AppDimens.radiusLarge))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = booking.departure.take(3).uppercase().ifEmpty { "???" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Gray900
                    )
                    Icon(Icons.Outlined.Flight, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(14.dp))
                    Text(
                        text = booking.destination.take(3).uppercase().ifEmpty { "???" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Gray900
                    )
                }
                // Status chip
                val (chipBg, chipText, chipLabel) = when {
                    booking.checkInStatus -> Triple(AppColors.SuccessLight, AppColors.Success, "Checked in")
                    isPast -> Triple(AppColors.Gray100, AppColors.Gray500, "Past")
                    else -> Triple(AppColors.PrimaryFaint, AppColors.Primary, "Check in")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.radiusFull))
                        .background(chipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(chipLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = chipText)
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = AppColors.Gray100)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LabelValue("Date", depDisplay)
                LabelValue("Flight", booking.flightId.take(8))
                LabelValue("Class", booking.cabinClass.ifBlank { "Economy" })
            }
        }
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = AppColors.Gray500)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Gray900)
    }
}

// ─── Booking Lookup Screen ────────────────────────────────────────

@Composable
fun BookingLookupScreen(
    onBookingFound: (String) -> Unit,
    viewModel: BookingLookupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reset state every time this screen is entered fresh
    LaunchedEffect(Unit) { viewModel.reset() }

    // Only navigate once per successful lookup
    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.result) {
        if (uiState.result != null && !navigated) {
            navigated = true
            onBookingFound(uiState.result!!.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.White)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Find booking", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("Enter your booking reference to check in", fontSize = 14.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(28.dp))

        AppTextField(
            value = uiState.reference,
            onValueChange = viewModel::updateReference,
            label = "Booking reference",
            icon = Icons.Outlined.ConfirmationNumber
        )
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = uiState.lastName,
            onValueChange = viewModel::updateLastName,
            label = "Last name",
            icon = Icons.Outlined.Person
        )

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::lookup,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            shape = RoundedCornerShape(AppDimens.radiusFull),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Find booking", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Booking Found Screen ─────────────────────────────────────────

@Composable
fun BookingFoundScreen(
    bookingId: String,
    onStartCheckIn: (String) -> Unit,
    onViewBoardingPass: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) { viewModel.loadBookingById(bookingId) }

    val booking = uiState.booking
    val flight = uiState.flight
    val departureInstant = remember(flight?.id) { resolveDepartureInstant(flight) }
    val checkInOpen = departureInstant?.let {
        val timeUntilDeparture = java.time.Duration.between(java.time.Instant.now(), it)
        timeUntilDeparture > java.time.Duration.ZERO || true // dev: check-in always open
    } ?: false

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.Primary)
        }
        uiState.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ErrorBanner(uiState.error ?: "Unable to load booking")
        }
        booking == null || flight == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Booking not found", color = AppColors.Gray500)
        }
        else -> Column(
            modifier = Modifier.fillMaxSize().background(AppColors.Gray50)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.White)
                    .padding(start = 8.dp, end = 20.dp, top = 48.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.Gray900)
                }
                Text("Booking details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            }

            Spacer(Modifier.height(12.dp))

            // Flight card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusXL))
                    .background(AppColors.White)
                    .border(1.dp, AppColors.Gray100, RoundedCornerShape(AppDimens.radiusXL))
                    .padding(20.dp)
            ) {
                Column {
                    // Route
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(flight.origin.take(3).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
                            Text(flight.origin, fontSize = 12.sp, color = AppColors.Gray500)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Flight, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
                            Text(flight.flightNumber, fontSize = 12.sp, color = AppColors.Primary, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(flight.destination.take(3).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
                            Text(flight.destination, fontSize = 12.sp, color = AppColors.Gray500, textAlign = TextAlign.End)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = AppColors.Gray100)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        LabelValue("Date", formatDepartureDisplay(flight))
                        LabelValue("Class", booking.cabinClass.ifBlank { "Economy" })
                        LabelValue("Passenger", booking.passengerName.ifBlank { "—" })
                    }
                    Spacer(Modifier.height(16.dp))

                    if (booking.checkInStatus) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppDimens.radiusFull))
                                .background(AppColors.SuccessLight)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Checked in", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Success)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onViewBoardingPass(booking.id) },
                            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                            shape = RoundedCornerShape(AppDimens.radiusFull),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) {
                            Text("View boarding pass", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (checkInOpen) {
                        Button(
                            onClick = { onStartCheckIn(booking.id) },
                            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                            shape = RoundedCornerShape(AppDimens.radiusFull),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) {
                            Text("Start check-in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppDimens.radiusMedium))
                                .background(AppColors.WarningLight)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Check-in opens 24 hours before departure",
                                color = AppColors.Warning,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(AppDimens.buttonHeight),
                shape = RoundedCornerShape(AppDimens.radiusFull),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray700)
            ) { Text("Back") }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Check-In Flow ────────────────────────────────────────────────

@Composable
fun CheckInScreen(
    bookingId: String,
    onGoToSeat: (flightId: String) -> Unit,
    onDone: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) { viewModel.loadBookingById(bookingId) }

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppColors.Primary)
        }
        uiState.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ErrorBanner(uiState.error ?: "Something went wrong")
        }
        else -> Column(Modifier.fillMaxSize().background(AppColors.White)) {
            // Step progress bar
            StepProgressBar(currentStep = uiState.currentStep, totalSteps = 6)

            when (uiState.currentStep) {
                1 -> PassportScanStep(
                    expectedLastName = uiState.booking?.lastName ?: "",
                    expectedFirstName = uiState.booking?.firstName ?: "",
                    onPassportScanned = { passport, last, first, dob, nationality ->
                        viewModel.updatePassenger(uiState.passenger.copy(
                            passportNumber = passport, 
                            fullName = "$first $last".trim(), 
                            dateOfBirth = dob,
                            nationality = nationality.ifBlank { uiState.passenger.nationality }
                        ))
                        viewModel.nextStep()
                    }
                )
                2 -> PassengerDetailsStep(
                    passenger = uiState.passenger,
                    flight = uiState.flight,
                    onUpdate = { viewModel.updatePassenger(it) },
                    onNext = { viewModel.nextStep() },
                    onBack = { viewModel.prevStep() }
                )
                3 -> com.airline.checkin.ui.seat.SeatMapScreen(
                    flightId = uiState.flight?.id ?: "",
                    passengerIndex = 0,
                    cabinClass = uiState.booking?.cabinClass ?: "ECONOMY",
                    onSeatPicked = { seatId, seatNumber -> viewModel.updateSeat(seatId, seatNumber); viewModel.nextStep() }
                )
                4 -> BaggageStep(baggageList = uiState.baggageList, onAdd = { viewModel.addBaggage(it) }, onRemove = { viewModel.removeBaggage(it) }, onNext = { viewModel.nextStep() }, onBack = { viewModel.prevStep() })
                5 -> SpecialRequestsStep(meal = uiState.mealPreference, wheelchair = uiState.needsWheelchair, infant = uiState.travelingWithInfant, pet = uiState.travelingWithPet, onUpdate = { m, w, i, p -> viewModel.updateSpecialRequests(m, w, i, p) }, onNext = { viewModel.nextStep() }, onBack = { viewModel.prevStep() })
                6 -> ConfirmationStep(uiState = uiState, viewModel = viewModel, onConfirm = { onDone() }, onBack = { viewModel.prevStep() })
            }
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: Int, totalSteps: Int) {
    val labels = listOf("Passport", "Details", "Seat", "Baggage", "Extras", "Confirm")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.White)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.take(totalSteps).forEachIndexed { i, label ->
                val step = i + 1
                val done = step < currentStep
                val active = step == currentStep
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when { done -> AppColors.Primary; active -> AppColors.Primary; else -> AppColors.Gray100 }
                            )
                            .border(1.dp, if (active || done) AppColors.Primary else AppColors.Gray300, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        } else {
                            Text("$step", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else AppColors.Gray500)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(label, fontSize = 8.sp, color = if (active) AppColors.Primary else AppColors.Gray500, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(AppDimens.radiusFull)).background(AppColors.Gray100)) {
            Box(Modifier.fillMaxWidth(fraction = (currentStep - 1f) / (totalSteps - 1f)).height(2.dp).background(AppColors.Primary))
        }
    }
}

// ─── Step 1: Passport Scan ────────────────────────────────────────

enum class ScanState { WAITING, DETECTING, MISMATCH, SUCCESS, TIMEOUT, ERROR }

@Composable
fun PassportScanStep(
    expectedLastName: String,
    expectedFirstName: String,
    onPassportScanned: (passport: String, lastName: String, firstName: String, dob: String, nationality: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCameraPermission = granted }
    LaunchedEffect(Unit) { if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var scanState by remember { mutableStateOf(ScanState.WAITING) }
    var mismatchMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(true) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            delay(20_000L)
            if (isProcessing) { isProcessing = false; scanState = ScanState.TIMEOUT }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppColors.White)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scan passport", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("Hold passport MRZ strip towards camera", fontSize = 13.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(20.dp))

        if (hasCameraPermission) {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusXL))
                    .border(2.dp, AppColors.Primary, RoundedCornerShape(AppDimens.radiusXL))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        ProcessCameraProvider.getInstance(ctx).addListener({
                            val provider = ProcessCameraProvider.getInstance(ctx).get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isProcessing) { imageProxy.close(); return@setAnalyzer }
                                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val text = visionText.text.uppercase()
                                                if (text.isBlank()) return@addOnSuccessListener
                                                if (scanState == ScanState.WAITING) scanState = ScanState.DETECTING
                                                
                                                var parsedLast = ""
                                                var parsedFirst = ""
                                                var parsedPassport = ""
                                                var parsedDob = ""
                                                var parsedNationality = ""

                                                val cleanText = text.replace(" ", "").replace("\n", "")

                                                // 1. Precise MRZ Line 1 Parse
                                                val mrz1Regex = Regex("P[A-Z<]([A-Z]{3})([A-Z0-9<]+)")
                                                val mrz1Match = mrz1Regex.find(cleanText)
                                                if (mrz1Match != null) {
                                                    val nat = mrz1Match.groupValues[1]
                                                    if (nat == "DZA" || nat == "ALG") parsedNationality = "Algerian"

                                                    val namesPart = mrz1Match.groupValues[2]
                                                    val nameSplit = namesPart.split("<<")
                                                    if (nameSplit.isNotEmpty()) { 
                                                        parsedLast = nameSplit[0].replace("<", "").trim()
                                                        if (nameSplit.size > 1) parsedFirst = nameSplit[1].replace("<", " ").trim() 
                                                    }
                                                }

                                                // 2. Precise MRZ Line 2 Parse
                                                val mrz2Regex = Regex("([A-Z0-9<]{9})\\d([A-Z<]{3})(\\d{6})\\d[MF<]")
                                                val mrz2Match = mrz2Regex.find(cleanText)
                                                if (mrz2Match != null) {
                                                    parsedPassport = mrz2Match.groupValues[1].replace("<", "")
                                                    val nat = mrz2Match.groupValues[2]
                                                    if (nat == "DZA" || nat == "ALG") parsedNationality = "Algerian"
                                                    
                                                    val dobRaw = mrz2Match.groupValues[3]
                                                    val y = dobRaw.substring(0, 2).toIntOrNull() ?: 0
                                                    parsedDob = "${dobRaw.substring(4, 6)}/${dobRaw.substring(2, 4)}/${if (y > 30) "19$y" else "20$y"}"
                                                }

                                                // 3. Robust Fallbacks for VIZ (Visual Inspection Zone)
                                                if (parsedPassport.length < 6) {
                                                    Regex("\\b[A-Z0-9]{9}\\b").find(text)?.let { parsedPassport = it.value }
                                                }

                                                // Nationality fallback matching English, Arabic or Country Code
                                                if (parsedNationality.isEmpty() && (text.contains("ALGERIAN") || text.contains("جزائرية") || text.contains("DZA"))) {
                                                    parsedNationality = "Algerian"
                                                }

                                                // Complex DOB Fallback to ignore Arabic characters (e.g. "14 جوان / JUINE 2002")
                                                if (parsedDob.isEmpty()) {
                                                    Regex("(\\d{2})\\s*[^0-9A-Za-z<]*\\s*([A-Z]+|\\d{2})\\s*(\\d{4})").find(text)?.let { match ->
                                                        val d = match.groupValues[1]
                                                        val mStr = match.groupValues[2]
                                                        val y = match.groupValues[3]
                                                        
                                                        val m = when {
                                                            mStr.contains("JAN") -> "01"
                                                            mStr.contains("FEB") || mStr.contains("FEV") -> "02"
                                                            mStr.contains("MAR") -> "03"
                                                            mStr.contains("APR") || mStr.contains("AVR") -> "04"
                                                            mStr.contains("MAY") || mStr.contains("MAI") -> "05"
                                                            mStr.contains("JUN") || mStr.contains("JUIN") -> "06"
                                                            mStr.contains("JUL") || mStr.contains("JUIL") -> "07"
                                                            mStr.contains("AUG") || mStr.contains("AOU") -> "08"
                                                            mStr.contains("SEP") -> "09"
                                                            mStr.contains("OCT") -> "10"
                                                            mStr.contains("NOV") -> "11"
                                                            mStr.contains("DEC") -> "12"
                                                            mStr.toIntOrNull() != null -> mStr.padStart(2, '0')
                                                            else -> "01"
                                                        }
                                                        parsedDob = "$d/$m/$y"
                                                    }
                                                }

                                                // Auto-correct last name if OCR failed reading it perfectly but it exists in block
                                                if (parsedLast.isEmpty() || !parsedLast.equals(expectedLastName, ignoreCase = true)) {
                                                    if (expectedLastName.isNotBlank() && text.contains(expectedLastName.uppercase())) {
                                                        parsedLast = expectedLastName.uppercase()
                                                    }
                                                }

                                                if (parsedPassport.isNotEmpty() && parsedLast.isNotEmpty()) {
                                                    if (parsedLast.equals(expectedLastName, ignoreCase = true)) {
                                                        isProcessing = false
                                                        scanState = ScanState.SUCCESS
                                                        mismatchMsg = null
                                                        onPassportScanned(parsedPassport, parsedLast, parsedFirst.ifBlank { expectedFirstName }, parsedDob, parsedNationality)
                                                    } else {
                                                        scanState = ScanState.MISMATCH
                                                        mismatchMsg = "Passport name (${parsedLast}) does not match booking name (${expectedLastName}). Check document and try again."
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { if (isProcessing) scanState = ScanState.ERROR }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else imageProxy.close()
                                }
                            }
                            try { provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis) }
                            catch (e: Exception) { scanState = ScanState.ERROR }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusXL))
                    .background(AppColors.Gray100)
                    .border(2.dp, AppColors.Gray300, RoundedCornerShape(AppDimens.radiusXL)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = AppColors.Gray500, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Camera permission required", color = AppColors.Gray500)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status feedback
        when (scanState) {
            ScanState.WAITING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.Primary)
                Text("Position passport MRZ in frame", color = AppColors.Gray700, fontSize = 13.sp)
            }
            ScanState.DETECTING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.Primary)
                Text("Reading passport data...", color = AppColors.Gray700, fontSize = 13.sp)
            }
            ScanState.MISMATCH -> {
                mismatchMsg?.let { ErrorBanner(it) }
            }
            ScanState.SUCCESS -> Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.radiusMedium)).background(AppColors.SuccessLight).padding(12.dp)
            ) { Text("Passport verified. Proceeding...", color = AppColors.Success, fontWeight = FontWeight.SemiBold) }
            ScanState.TIMEOUT -> ErrorBanner("Scan timed out. Try better lighting or use manual entry below.")
            ScanState.ERROR -> ErrorBanner("Could not read passport. Retry or use manual entry.")
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { onPassportScanned("MANUAL000", expectedLastName, expectedFirstName, "", "") },
            modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
            shape = RoundedCornerShape(AppDimens.radiusFull),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Primary)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Enter details manually")
        }
    }
}

// ─── Step 2: Passenger Details ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsStep(
    passenger: Passenger,
    flight: com.airline.checkin.domain.model.Flight?,
    onUpdate: (Passenger) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var givenName  by remember { mutableStateOf(passenger.fullName.substringBefore(" ")) }
    var lastName   by remember { mutableStateOf(passenger.fullName.substringAfter(" ", "")) }
    var nationality by remember { mutableStateOf(passenger.nationality) }
    var dob        by remember { mutableStateOf(passenger.dateOfBirth) }
    var passport   by remember { mutableStateOf(passenger.passportNumber) }
    var gender     by remember { mutableStateOf("") }
    var idType     by remember { mutableStateOf("Passport") }
    var submitted  by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var nationalityExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var idTypeExpanded by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    val idTypeOptions = listOf("Passport", "National ID", "Driver License")

    // Common nationalities
    val nationalityOptions = listOf(
        "Afghan", "Albanian", "Algerian", "American", "Argentinian", "Australian",
        "Austrian", "Bahraini", "Belgian", "Brazilian", "British", "Canadian",
        "Chilean", "Chinese", "Colombian", "Croatian", "Czech", "Danish", "Dutch",
        "Egyptian", "Finnish", "French", "German", "Greek", "Hungarian", "Indian",
        "Indonesian", "Iranian", "Iraqi", "Irish", "Israeli", "Italian", "Japanese",
        "Jordanian", "Kenyan", "Korean", "Kuwaiti", "Lebanese", "Libyan",
        "Malaysian", "Moroccan", "Mexican", "Nigerian", "Norwegian", "Pakistani",
        "Peruvian", "Polish", "Portuguese", "Qatari", "Romanian", "Russian",
        "Saudi", "Serbian", "Singaporean", "South African", "Spanish", "Swedish",
        "Swiss", "Syrian", "Thai", "Tunisian", "Turkish", "Ukrainian",
        "Emirati", "Venezuelan", "Vietnamese"
    )

    // Validation
    val dobIsMinor: Boolean = remember(dob) {
        if (dob.isBlank()) false
        else runCatching {
            val parts = dob.split("/")
            if (parts.size == 3) {
                val year = parts[2].toInt(); val month = parts[1].toInt(); val day = parts[0].toInt()
                val dobDate = java.time.LocalDate.of(year, month, day)
                java.time.Period.between(dobDate, java.time.LocalDate.now()).years < 18
            } else false
        }.getOrDefault(false)
    }

    val isPassportValid = passport.matches(Regex("[A-Z0-9]{6,9}"))

    Column(modifier = Modifier.fillMaxSize().background(AppColors.White)) {
        // Flight info strip
        if (flight != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(AppColors.Primary)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${flight.origin} — ${flight.destination}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("·", color = Color(0xFFCC99FF))
                Text(formatDepartureDisplay(flight), fontSize = 12.sp, color = Color(0xFFEDD9FF))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text("Passenger details", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            Spacer(Modifier.height(4.dp))
            Text("Must match travel document exactly", fontSize = 13.sp, color = AppColors.Gray500)
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = givenName, onValueChange = { givenName = it }, label = "Given names",
                    modifier = Modifier.weight(1f),
                    error = if (submitted && givenName.isBlank()) "Required" else null
                )
                AppTextField(
                    value = lastName, onValueChange = { lastName = it }, label = "Surname",
                    modifier = Modifier.weight(1f),
                    error = if (submitted && lastName.isBlank()) "Required" else null
                )
            }
            Spacer(Modifier.height(12.dp))

            // Nationality — searchable dropdown
            ExposedDropdownMenuBox(
                expanded = nationalityExpanded,
                onExpandedChange = { nationalityExpanded = !nationalityExpanded }
            ) {
                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it; nationalityExpanded = true },
                    label = { Text("Nationality", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = RoundedCornerShape(AppDimens.radiusLarge),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nationalityExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Gray300, focusedLabelColor = AppColors.Primary
                    ),
                    isError = submitted && nationality.isBlank()
                )
                ExposedDropdownMenu(
                    expanded = nationalityExpanded && nationalityOptions.any { it.contains(nationality, ignoreCase = true) },
                    onDismissRequest = { nationalityExpanded = false }
                ) {
                    nationalityOptions.filter { it.contains(nationality, ignoreCase = true) }.take(6).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { nationality = option; nationalityExpanded = false }
                        )
                    }
                }
            }
            if (submitted && nationality.isBlank()) {
                Text("Required", color = AppColors.Error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))

            // Date of birth — tap to open date picker
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of birth", fontSize = 14.sp) },
                placeholder = { Text("DD/MM/YYYY", color = AppColors.Gray300) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                singleLine = true,
                readOnly = true,
                shape = RoundedCornerShape(AppDimens.radiusLarge),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date", tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Gray300, focusedLabelColor = AppColors.Primary,
                    errorBorderColor = AppColors.Error
                ),
                isError = (submitted && dob.isBlank()) || dobIsMinor
            )
            when {
                submitted && dob.isBlank() -> Text("Required", color = AppColors.Error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                dobIsMinor -> Text("Passengers under 18 are not eligible for self check-in", color = AppColors.Error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
            Spacer(Modifier.height(12.dp))

            // Gender dropdown
            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                OutlinedTextField(
                    value = gender, onValueChange = {}, readOnly = true, label = { Text("Gender", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true,
                    shape = RoundedCornerShape(AppDimens.radiusLarge),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Gray300, focusedLabelColor = AppColors.Primary)
                )
                ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    genderOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { gender = it; genderExpanded = false }) }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ID type
            ExposedDropdownMenuBox(expanded = idTypeExpanded, onExpandedChange = { idTypeExpanded = !idTypeExpanded }) {
                OutlinedTextField(
                    value = idType, onValueChange = {}, readOnly = true, label = { Text("ID type", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true,
                    shape = RoundedCornerShape(AppDimens.radiusLarge),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = idTypeExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Gray300, focusedLabelColor = AppColors.Primary)
                )
                ExposedDropdownMenu(expanded = idTypeExpanded, onDismissRequest = { idTypeExpanded = false }) {
                    idTypeOptions.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { idType = it; idTypeExpanded = false }) }
                }
            }
            Spacer(Modifier.height(12.dp))

            AppTextField(
                value = passport, onValueChange = { passport = it.uppercase() }, label = "Passport / ID number",
                icon = Icons.Outlined.Badge,
                error = when {
                    submitted && passport.isBlank() -> "Required"
                    submitted && passport.isNotBlank() && !isPassportValid -> "Invalid passport number (6–9 alphanumeric characters)"
                    else -> null
                }
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    submitted = true
                    if (givenName.isBlank() || lastName.isBlank() || passport.isBlank() || nationality.isBlank() || dob.isBlank()) return@Button
                    if (dobIsMinor) return@Button
                    if (!isPassportValid) return@Button
                    onUpdate(Passenger(fullName = "$givenName $lastName".trim(), nationality = nationality, dateOfBirth = dob, passportNumber = passport))
                    onNext()
                },
                modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                shape = RoundedCornerShape(AppDimens.radiusFull),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                shape = RoundedCornerShape(AppDimens.radiusFull),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray700)
            ) { Text("Back") }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { day, month, year ->
                dob = "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}/$year"
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (day: Int, month: Int, year: Int) -> Unit
) {
    val state = rememberDatePickerState()
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) {
                    val ld = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    onDateSelected(ld.dayOfMonth, ld.monthValue, ld.year)
                } else onDismissRequest()
            }) { Text("OK", color = AppColors.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel", color = AppColors.Gray500) }
        }
    ) {
        DatePicker(state = state, colors = DatePickerDefaults.colors(selectedDayContainerColor = AppColors.Primary, todayDateBorderColor = AppColors.Primary))
    }
}

// ─── Step 4: Baggage ──────────────────────────────────────────────

@Composable
fun BaggageStep(
    baggageList: List<BaggageDeclaration>,
    onAdd: (BaggageDeclaration) -> Unit,
    onRemove: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.White).padding(24.dp)) {
        Text("Baggage", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("Declare your luggage for this flight", fontSize = 13.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BaggageAddButton(
                label = "Cabin bag", sublabel = "7 kg",
                icon = Icons.Outlined.Backpack,
                modifier = Modifier.weight(1f),
                onClick = { onAdd(BaggageDeclaration(id = "BAG-${baggageList.size + 1}", cabinBags = 1, checkedBags = 0)) }
            )
            BaggageAddButton(
                label = "Checked bag", sublabel = "20 kg",
                icon = Icons.Outlined.Luggage,
                modifier = Modifier.weight(1f),
                onClick = { onAdd(BaggageDeclaration(id = "BAG-${baggageList.size + 1}", cabinBags = 0, checkedBags = 1)) }
            )
        }

        Spacer(Modifier.height(20.dp))

        if (baggageList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.radiusLarge))
                    .background(AppColors.Gray50).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No bags added yet", color = AppColors.Gray500, fontSize = 14.sp)
            }
        } else {
            baggageList.forEachIndexed { index, bag ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(AppDimens.radiusMedium))
                        .background(AppColors.Gray50)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (bag.cabinBags > 0) Icons.Outlined.Backpack else Icons.Outlined.Luggage,
                            contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(if (bag.cabinBags > 0) "Cabin bag" else "Checked bag", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppColors.Gray900)
                            Text(if (bag.cabinBags > 0) "7 kg" else "20 kg", fontSize = 12.sp, color = AppColors.Gray500)
                        }
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Remove", tint = AppColors.Error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(20.dp))

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray700)) { Text("Back") }
    }
}

@Composable
private fun BaggageAddButton(label: String, sublabel: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(AppDimens.radiusLarge),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Primary)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(sublabel, fontSize = 10.sp, color = AppColors.Gray500)
        }
    }
}

// ─── Step 5: Special Requests ─────────────────────────────────────

@Composable
fun SpecialRequestsStep(
    meal: String, wheelchair: Boolean, infant: Boolean, pet: Boolean,
    onUpdate: (String, Boolean, Boolean, Boolean) -> Unit,
    onNext: () -> Unit, onBack: () -> Unit
) {
    var mealText  by remember { mutableStateOf(meal) }
    var wcChecked by remember { mutableStateOf(wheelchair) }
    var infChecked by remember { mutableStateOf(infant) }
    var petChecked by remember { mutableStateOf(pet) }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.White).padding(24.dp)) {
        Text("Special requests", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("All optional", fontSize = 13.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(20.dp))

        AppTextField(
            value = mealText, onValueChange = { mealText = it },
            label = "Meal preferences",
            icon = Icons.Outlined.Restaurant,
            singleLine = false,
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        Spacer(Modifier.height(16.dp))

        listOf(
            Triple(Icons.Outlined.Accessible, "Wheelchair assistance", wcChecked) to { v: Boolean -> wcChecked = v },
            Triple(Icons.Outlined.ChildCare, "Traveling with infant", infChecked) to { v: Boolean -> infChecked = v },
            Triple(Icons.Outlined.Pets, "Traveling with pet", petChecked) to { v: Boolean -> petChecked = v }
        ).forEach { (triple, setter) ->
            val (icon, label, checked) = triple
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.radiusMedium))
                    .clickable { setter(!checked) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) AppColors.Primary else AppColors.Gray500, modifier = Modifier.size(20.dp))
                Text(label, fontSize = 14.sp, color = AppColors.Gray900, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = checked, onCheckedChange = { setter(it) },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary)
                )
            }
            HorizontalDivider(color = AppColors.Gray100)
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(20.dp))

        Button(onClick = { onUpdate(mealText, wcChecked, infChecked, petChecked); onNext() }, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray700)) { Text("Back") }
    }
}

// ─── Step 6: Confirmation ─────────────────────────────────────────

@Composable
fun ConfirmationStep(
    uiState: CheckInUiState,
    viewModel: CheckInViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.submitCheckIn(canSendNotification = granted) { showDialog = false; onConfirm() }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.White).verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Review & confirm", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        Spacer(Modifier.height(4.dp))
        Text("Check your details before submitting", fontSize = 13.sp, color = AppColors.Gray500)
        Spacer(Modifier.height(20.dp))

        // Summary card
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.radiusXL))
                .border(1.dp, AppColors.Gray100, RoundedCornerShape(AppDimens.radiusXL)).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val rows = listOf(
                    "Passenger" to uiState.passenger.fullName.ifBlank { "—" },
                    "Flight" to (uiState.flight?.flightNumber ?: uiState.booking?.flightId ?: "—"),
                    "Route" to "${uiState.flight?.origin ?: "?"} — ${uiState.flight?.destination ?: "?"}",
                    "Seat" to uiState.selectedSeatNumber.ifBlank { "Not selected" },
                    "Passport" to uiState.passenger.passportNumber.ifBlank { "—" },
                    "Bags" to "${uiState.baggageList.sumOf { it.cabinBags }} cabin, ${uiState.baggageList.sumOf { it.checkedBags }} checked"
                )
                rows.forEach { (label, value) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, fontSize = 13.sp, color = AppColors.Gray500)
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.Gray900)
                    }
                }
            }
        }

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary), enabled = !uiState.isLoading) {
            if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else Text("Confirm check-in", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), shape = RoundedCornerShape(AppDimens.radiusFull), border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Gray300)), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Gray700)) { Text("Back") }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm check-in?", fontWeight = FontWeight.Bold) },
            text = { Text("Once submitted, your boarding pass will be generated.") },
            confirmButton = {
                Button(
                    onClick = {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.submitCheckIn(canSendNotification = true) { showDialog = false; onConfirm() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) { Text("Check in now") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = AppColors.Gray500) } },
            shape = RoundedCornerShape(AppDimens.radiusXL)
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────

private fun resolveDepartureInstant(flight: com.airline.checkin.domain.model.Flight?): java.time.Instant? {
    val dt = flight?.departureTime?.trim().orEmpty()
    if (dt.isBlank()) return null
    runCatching { java.time.Instant.parse(if (!dt.endsWith("Z") && !dt.contains("+")) "${dt}Z" else dt) }.getOrNull()?.let { return it }
    val dateKey = flight?.dateKey?.trim().orEmpty()
    if (dateKey.isBlank()) return null
    val date = runCatching { java.time.LocalDate.parse(dateKey) }.getOrNull() ?: return null
    val time = runCatching { java.time.LocalTime.parse(dt) }.getOrNull() ?: java.time.LocalTime.MIDNIGHT
    return date.atTime(time).atZone(java.time.ZoneId.systemDefault()).toInstant()
}

private fun formatDepartureDisplay(flight: com.airline.checkin.domain.model.Flight?): String {
    val instant = resolveDepartureInstant(flight) ?: return flight?.departureTime?.ifBlank { "—" } ?: "—"
    val ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    return ldt.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm"))
}

private const val CHECK_IN_CHANNEL_ID = "check_in_updates"
private fun showCheckInCompleteNotification(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        nm.createNotificationChannel(NotificationChannel(CHECK_IN_CHANNEL_ID, "Check-In Updates", NotificationManager.IMPORTANCE_DEFAULT))
    }
    val notification = NotificationCompat.Builder(context, CHECK_IN_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Check-in complete")
        .setContentText("Your boarding pass is ready.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
}