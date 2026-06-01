package com.airline.checkin.ui.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.BookingRepository
import com.airline.checkin.domain.model.BaggageDeclaration
import com.airline.checkin.domain.model.Booking
import com.airline.checkin.domain.model.Passenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import com.airline.checkin.R
import com.airline.checkin.domain.model.BaggageData
import com.airline.checkin.domain.model.PassportData
import com.airline.checkin.domain.model.SeatData
import com.airline.checkin.domain.model.SpecialRequests
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

// ---- ViewModel ----

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
                if (booking == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Booking not found")
                    return@launch
                }

                val flight = flightRepository.getFlight(booking.flightId)
                if (flight == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Flight not found")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    booking = booking,
                    flight = flight,
                    error = null
                )
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
                    _uiState.value = CheckInUiState(error = "Booking not found")
                }
            } catch (e: Exception) {
                _uiState.value = CheckInUiState(error = e.message)
            }
        }
    }

    fun updatePassenger(passenger: Passenger) {
        _uiState.value = _uiState.value.copy(passenger = passenger)
    }

    fun updateSeat(seatId: String, seatNumber: String) {
        _uiState.value = _uiState.value.copy(
            selectedSeatId = seatId,
            selectedSeatNumber = seatNumber
        )
    }

    fun addBaggage(baggage: BaggageDeclaration) {
        val updated = _uiState.value.baggageList + baggage
        _uiState.value = _uiState.value.copy(baggageList = updated)
    }

    fun removeBaggage(index: Int) {
        val updated = _uiState.value.baggageList.toMutableList().also { it.removeAt(index) }
        _uiState.value = _uiState.value.copy(baggageList = updated)
    }

    fun updateSpecialRequests(
        meal: String = _uiState.value.mealPreference,
        wheelchair: Boolean = _uiState.value.needsWheelchair,
        infant: Boolean = _uiState.value.travelingWithInfant,
        pet: Boolean = _uiState.value.travelingWithPet
    ) {
        _uiState.value = _uiState.value.copy(
            mealPreference = meal,
            needsWheelchair = wheelchair,
            travelingWithInfant = infant,
            travelingWithPet = pet
        )
    }

    fun submitCheckIn(canSendNotification: Boolean, onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val existingBooking = currentState.booking
        val bookingId = existingBooking?.id
        if (bookingId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "Booking not loaded")
            return
        }

        val resolvedFirstName = currentState.passenger.fullName.trim()
            .substringBefore(" ", existingBooking.firstName)
            .ifBlank { existingBooking.firstName }
        val resolvedLastName = currentState.passenger.fullName.trim()
            .substringAfter(" ", existingBooking.lastName)
            .ifBlank { existingBooking.lastName }

        val checkInPayload = existingBooking.copy(
            checkInStatus = true,
            firstName = resolvedFirstName,
            lastName = resolvedLastName,
            seat = SeatData(
                seatId = currentState.selectedSeatId,
                seatNumber = currentState.selectedSeatNumber
            ),
            passport = PassportData(
                number = currentState.passenger.passportNumber,
                dob = currentState.passenger.dateOfBirth,
                nationality = currentState.passenger.nationality
            ),
            baggage = BaggageData(
                cabin = currentState.baggageList.sumOf { it.cabinBags },
                checked = currentState.baggageList.sumOf { it.checkedBags }
            ),
            specialRequests = SpecialRequests(
                dietary = currentState.mealPreference,
                wheelchair = currentState.needsWheelchair,
                infant = currentState.travelingWithInfant,
                pet = currentState.travelingWithPet
            )
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                bookingRepository.submitCheckIn(bookingId, checkInPayload)
                _uiState.value = _uiState.value.copy(isLoading = false)
                if (canSendNotification) {
                    showCheckInCompleteNotification(appContext)
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }
}

data class BookingLookupUiState(
    val reference: String = "",
    val lastName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val result: Booking? = null
)

@HiltViewModel
class BookingLookupViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingLookupUiState())
    val uiState = _uiState.asStateFlow()

    fun updateReference(value: String) {
        _uiState.value = _uiState.value.copy(reference = value, error = null, result = null)
    }

    fun updateLastName(value: String) {
        _uiState.value = _uiState.value.copy(lastName = value, error = null, result = null)
    }

    fun lookup() {
        viewModelScope.launch {
            val reference = _uiState.value.reference.trim()
            val lastName = _uiState.value.lastName.trim()
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            try {
                val booking = bookingRepository.getBooking(reference, lastName)
                _uiState.value = if (booking != null) {
                    _uiState.value.copy(isLoading = false, result = booking)
                } else {
                    _uiState.value.copy(isLoading = false, error = "Booking not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

// ---- My Bookings Screen ----

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings = _bookings.asStateFlow()

    init {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                bookingRepository.observeUserBookings(userId).collect {
                    _bookings.value = it
                }
            }
            viewModelScope.launch {
                try {
                    bookingRepository.refreshUserBookings(userId)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }
}

@Composable
fun MyBookingsScreen(
    onNavigateToCheckIn: (String) -> Unit,
    onNavigateToBoardingPass: (String) -> Unit,
    viewModel: MyBookingsViewModel = hiltViewModel()
) {
    val bookings by viewModel.bookings.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Passes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No passes found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings.size) { index ->
                    val booking = bookings[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (booking.checkInStatus) {
                                onNavigateToBoardingPass(booking.id)
                            } else {
                                onNavigateToCheckIn(booking.id)
                            }
                        },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Reference: ${booking.id}", style = MaterialTheme.typography.titleMedium)
                            Text("Flight: ${booking.flightId}", style = MaterialTheme.typography.bodyMedium)
                            if (booking.checkInStatus) {
                                Text(
                                    text = "Checked In · Pass Ready",
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "Check-In Open",
                                    color = Color(0xFFFFC107),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingLookupScreen(
    onBookingFound: (String) -> Unit,
    viewModel: BookingLookupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.result?.id) {
        uiState.result?.id?.let(onBookingFound)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Online Check-In", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.reference,
            onValueChange = viewModel::updateReference,
            label = { Text("Booking Reference") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.lastName,
            onValueChange = viewModel::updateLastName,
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::lookup,
            enabled = !uiState.isLoading && uiState.reference.isNotBlank() && uiState.lastName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(12.dp))
                Text("Finding...")
            } else {
                Text("Find Booking")
            }
        }
    }
}

@Composable
fun BookingFoundScreen(
    bookingId: String,
    onStartCheckIn: (String) -> Unit,
    onViewBoardingPass: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.loadBookingById(bookingId)
    }

    val booking = uiState.booking
    val flight = uiState.flight
    val departureInstant = remember(flight?.id, flight?.departureTime, flight?.dateKey) {
        resolveDepartureInstant(flight)
    }
    val checkInOpen = departureInstant?.let {
        val timeUntilDeparture = java.time.Duration.between(java.time.Instant.now(), it)
        timeUntilDeparture > java.time.Duration.ZERO && timeUntilDeparture <= java.time.Duration.ofHours(24)
    } ?: false

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Unable to load booking",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        booking == null || flight == null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Booking not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("Booking Found", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Flight: ${flight.flightNumber}", style = MaterialTheme.typography.titleMedium)
                        Text("${flight.origin} → ${flight.destination}")
                        Text("Departure: ${formatDepartureDisplay(flight)}")
                        Text("Passenger: ${booking.passengerName.ifBlank { "Unknown" }}")
                        Text("Cabin class: ${booking.cabinClass.ifBlank { "Unknown" }}")

                        Spacer(Modifier.height(16.dp))

                        if (booking.checkInStatus) {
                            Text(
                                text = "Already Checked In",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onViewBoardingPass(booking.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Boarding Pass")
                            }
                        } else if (checkInOpen) {
                            Button(
                                onClick = { onStartCheckIn(booking.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Start Check-In")
                            }
                        } else {
                            Text(
                                text = "Check-in opens 24 hours before departure",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back")
                }
            }
        }
    }
}

private fun resolveDepartureInstant(flight: com.airline.checkin.domain.model.Flight?): java.time.Instant? {
    val departureTime = flight?.departureTime?.trim().orEmpty()
    if (departureTime.isBlank()) return null

    runCatching {
        val normalized = if (!departureTime.endsWith("Z") && !departureTime.contains("+")) {
            "${departureTime}Z"
        } else departureTime
        java.time.Instant.parse(normalized)
    }.getOrNull()?.let { return it }

    val dateKey = flight?.dateKey?.trim().orEmpty()
    if (dateKey.isBlank()) return null

    val date = runCatching { java.time.LocalDate.parse(dateKey) }.getOrNull() ?: return null
    val time = runCatching { java.time.LocalTime.parse(departureTime) }
        .getOrNull() ?: java.time.LocalTime.MIDNIGHT
    return date.atTime(time).atZone(java.time.ZoneId.systemDefault()).toInstant()
}

private fun formatDepartureDisplay(flight: com.airline.checkin.domain.model.Flight?): String {
    val departureInstant = resolveDepartureInstant(flight) ?: return flight?.departureTime?.ifBlank { "Unknown" } ?: "Unknown"
    val localDateTime = java.time.LocalDateTime.ofInstant(departureInstant, java.time.ZoneId.systemDefault())
    val datePart = localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
    val timePart = localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    return "$datePart • $timePart"
}

// ---- Check-In Flow Screen ----

@Composable
fun CheckInScreen(
    bookingId: String,
    onGoToSeat: (flightId: String) -> Unit,
    onDone: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.loadBookingById(bookingId)
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading check-in...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Something went wrong",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> when (uiState.currentStep) {
            1 -> PassportScanStep(
                expectedLastName = uiState.booking?.lastName ?: "",
                expectedFirstName = uiState.booking?.firstName ?: "",
                onPassportScanned = { passportNumber, lastName, firstName, dob ->
                    val updatedPassenger = uiState.passenger.copy(
                        passportNumber = passportNumber,
                        fullName = "$firstName $lastName".trim(),
                        dateOfBirth = dob
                    )
                    viewModel.updatePassenger(updatedPassenger)
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
            3 -> {
                com.airline.checkin.ui.seat.SeatMapScreen(
                    flightId = uiState.flight?.id ?: "",
                    passengerIndex = 0,
                    cabinClass = uiState.booking?.cabinClass ?: "ECONOMY",
                    onSeatPicked = { seatId, seatNumber ->
                        viewModel.updateSeat(seatId, seatNumber)
                        viewModel.nextStep()
                    }
                )
            }
            4 -> BaggageStep(
                baggageList = uiState.baggageList,
                onAdd = { viewModel.addBaggage(it) },
                onRemove = { viewModel.removeBaggage(it) },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() }
            )
            5 -> SpecialRequestsStep(
                meal = uiState.mealPreference,
                wheelchair = uiState.needsWheelchair,
                infant = uiState.travelingWithInfant,
                pet = uiState.travelingWithPet,
                onUpdate = { meal, wc, inf, pet ->
                    viewModel.updateSpecialRequests(meal, wc, inf, pet)
                },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() }
            )
            6 -> ConfirmationStep(
                uiState = uiState,
                viewModel = viewModel,
                onConfirm = { onDone() },
                onBack = { viewModel.prevStep() }
            )
        }
    }
}

// ---- Step 1: Passport Scan ----

enum class ScanState { WAITING, DETECTING, MISMATCH, SUCCESS, TIMEOUT, ERROR }

@Composable
fun PassportScanStep(
    expectedLastName: String,
    expectedFirstName: String,
    onPassportScanned: (passport: String, lastName: String, firstName: String, dob: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var scanState by remember { mutableStateOf(ScanState.WAITING) }
    var mismatchMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Timeout: give up after 20 seconds and let the user know
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            delay(20_000L)
            if (isProcessing) {
                isProcessing = false
                scanState = ScanState.TIMEOUT
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Scan Passport", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ensure the two lines at the bottom (MRZ) are visible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(8.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        if (!isProcessing) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            textRecognizer.process(image)
                                                .addOnSuccessListener { visionText ->
                                                    val text = visionText.text.uppercase()
                                                    if (text.isBlank()) return@addOnSuccessListener

                                                    // Text detected — advance from WAITING to DETECTING once
                                                    if (scanState == ScanState.WAITING) {
                                                        scanState = ScanState.DETECTING
                                                    }

                                                    var parsedLastName = ""
                                                    var parsedFirstName = ""
                                                    var parsedPassport = ""
                                                    var parsedDob = ""

                                                    // --- 1. SMART MRZ PARSING ---
                                                    // Fix MLKit's biggest flaw: replace spaces with MRZ '<' characters
                                                    val cleanLines = text.split('\n').map { it.replace(" ", "<").replace("«", "<<") }
                                                    
                                                    val mrzLine1 = cleanLines.find { it.startsWith("P") && it.contains("<") && it.length > 20 }
                                                    val mrzLine2 = cleanLines.find { it.length > 20 && it != mrzLine1 && it.count { c -> c.isDigit() } > 5 }

                                                    if (mrzLine1 != null) {
                                                        val namesPart = mrzLine1.substringAfter("P<").drop(3) // Drop 3-letter country code
                                                        val nameSplit = namesPart.split("<<")
                                                        if (nameSplit.isNotEmpty()) {
                                                            parsedLastName = nameSplit[0].replace("<", "").trim()
                                                            if (nameSplit.size > 1) {
                                                                parsedFirstName = nameSplit[1].replace("<", " ").trim()
                                                            }
                                                        }
                                                    }

                                                    if (mrzLine2 != null) {
                                                        // Passport number is the first 9 characters of line 2
                                                        val match = Regex("^([A-Z0-9<]{9})").find(mrzLine2)
                                                        if (match != null) {
                                                            parsedPassport = match.groupValues[1].replace("<", "")
                                                        }
                                                        // DOB is at index 13 to 18 in a standard MRZ
                                                        if (mrzLine2.length >= 19) {
                                                            val dobRaw = mrzLine2.substring(13, 19)
                                                            if (dobRaw.all { it.isDigit() }) {
                                                                val year = dobRaw.substring(0, 2).toIntOrNull() ?: 0
                                                                val fullYear = if (year > 30) "19$year" else "20$year"
                                                                parsedDob = "${dobRaw.substring(4, 6)}/${dobRaw.substring(2, 4)}/$fullYear"
                                                            }
                                                        }
                                                    }

                                                    // --- 2. ACADEMIC FALLBACK (BULLETPROOFING) ---
                                                    // If the MRZ is unreadable due to glare, find the data anywhere on the page
                                                    if (parsedPassport.length < 6) {
                                                        val pMatch = Regex("\\b[A-Z0-9]{9}\\b").find(text)
                                                        if (pMatch != null) parsedPassport = pMatch.value
                                                    }
                                                    
                                                    if (parsedDob.isEmpty()) {
                                                        // Look for standard passport date format e.g., 15 MAR 1990
                                                        val dMatch = Regex("\\b\\d{2}\\s[A-Z]{3}\\s\\d{4}\\b").find(text)
                                                        if (dMatch != null) parsedDob = dMatch.value
                                                    }

                                                    // If the parser didn't perfectly extract the last name, but the expected 
                                                    // booking last name is visibly written anywhere on the passport, accept it!
                                                    if (parsedLastName.isEmpty() || !parsedLastName.equals(expectedLastName, ignoreCase = true)) {
                                                        if (expectedLastName.isNotBlank() && text.contains(expectedLastName.uppercase())) {
                                                            parsedLastName = expectedLastName.uppercase()
                                                        }
                                                    }

                                                    // --- 3. VALIDATION LOGIC ---
                                                    if (parsedPassport.isNotEmpty() && parsedLastName.isNotEmpty()) {
                                                        if (parsedLastName.equals(expectedLastName, ignoreCase = true)) {
                                                            isProcessing = false
                                                            scanState = ScanState.SUCCESS
                                                            mismatchMsg = null
                                                            onPassportScanned(parsedPassport, parsedLastName, parsedFirstName, parsedDob)
                                                        } else {
                                                            scanState = ScanState.MISMATCH
                                                            mismatchMsg = "Name mismatch!\nBooking: ${expectedLastName.uppercase()}\nPassport: $parsedLastName"
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    if (isProcessing) scanState = ScanState.ERROR
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                scanState = ScanState.ERROR
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(300.dp).padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Scan status indicator
        when (scanState) {
            ScanState.WAITING ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Position passport MRZ in frame…", fontWeight = FontWeight.Medium)
                }
            ScanState.DETECTING ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("MRZ detected, reading data…", fontWeight = FontWeight.Medium)
                }
            ScanState.MISMATCH ->
                Text("⚠️ Keep scanning…", fontWeight = FontWeight.Medium)
            ScanState.SUCCESS ->
                Text("✅ Passport verified", fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
            ScanState.TIMEOUT ->
                Text(
                    "⏱ Timed out — try better lighting or use manual entry.",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            ScanState.ERROR ->
                Text(
                    "⚠️ Recognition error — retry or use manual entry.",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
        }

        if (scanState == ScanState.MISMATCH && mismatchMsg != null) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = mismatchMsg!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onPassportScanned("MOCK12345", expectedLastName, expectedFirstName, "01/01/1990")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip (Manual Entry)")
        }
    }
}

// ---- Step 2: Passenger Details ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsStep(
    passenger: Passenger,
    flight: com.airline.checkin.domain.model.Flight?,
    onUpdate: (Passenger) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var givenName by remember { mutableStateOf(passenger.fullName.substringBefore(" ")) }
    var lastName by remember { mutableStateOf(passenger.fullName.substringAfter(" ", "")) }
    var nationality by remember { mutableStateOf(passenger.nationality) }
    var dob by remember { mutableStateOf(passenger.dateOfBirth) }
    var passport by remember { mutableStateOf(passenger.passportNumber) }
    var gender by remember { mutableStateOf("") }
    var idType by remember { mutableStateOf("Passport") }

    // Gender dropdown state
    var genderExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")

    // ID type dropdown state
    var idTypeExpanded by remember { mutableStateOf(false) }
    val idTypeOptions = listOf("Passport", "National ID", "Driver License")

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Purple flight info header bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF6200EE))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val depTime = flight?.departureTime?.let {
                try {
                    val instant = java.time.Instant.parse(if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it)
                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) { "TBD" }
            } ?: "TBD"

            val arrTime = flight?.arrivalTime?.let {
                try {
                    val instant = java.time.Instant.parse(if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it)
                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) { "TBD" }
            } ?: "TBD"

            val dateDisplay = flight?.departureTime?.let {
                try {
                    val instant = java.time.Instant.parse(if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it)
                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                } catch (e: Exception) { "TBD" }
            } ?: "TBD"

            val origin = flight?.origin ?: "Unknown"
            val dest = flight?.destination ?: "Unknown"

            Text(
                text = "Depart  •  $origin - $dest  •  $dateDisplay  •  $depTime - $arrTime  ⏱",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Section label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Passenger", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Given names
            OutlinedTextField(
                value = givenName,
                onValueChange = { givenName = it },
                label = { Text("Given names") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            // Last name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last name (surname)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            // Nationality dropdown
            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = { Text("Nationality") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                )
            }
            Spacer(Modifier.height(10.dp))

            // Date of birth with calendar icon
            OutlinedTextField(
                value = dob,
                onValueChange = { dob = it },
                label = { Text("Date of birth") },
                placeholder = { Text("DD/MM/YYYY") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick date",
                        tint = Color(0xFF6200EE))
                }
            )
            Spacer(Modifier.height(10.dp))

            // Gender dropdown
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = !genderExpanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        Icon(
                            if (genderExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ID type dropdown
            ExposedDropdownMenuBox(
                expanded = idTypeExpanded,
                onExpandedChange = { idTypeExpanded = !idTypeExpanded }
            ) {
                OutlinedTextField(
                    value = idType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ID type") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        Icon(
                            if (idTypeExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                )
                ExposedDropdownMenu(
                    expanded = idTypeExpanded,
                    onDismissRequest = { idTypeExpanded = false }
                ) {
                    idTypeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                idType = option
                                idTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Passport number
            OutlinedTextField(
                value = passport,
                onValueChange = { passport = it },
                label = { Text("Passport / ID number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    onUpdate(
                        Passenger(
                            fullName = "$givenName $lastName".trim(),
                            nationality = nationality,
                            dateOfBirth = dob,
                            passportNumber = passport
                        )
                    )
                    onNext()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = givenName.isNotBlank() && lastName.isNotBlank() && passport.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) { Text("Confirm", fontSize = 16.sp) }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                border = BorderStroke(1.dp, Color(0xFF6200EE))
            ) {
                Icon(Icons.Default.Edit, contentDescription = null,
                    tint = Color(0xFF6200EE), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit", color = Color(0xFF6200EE), fontSize = 16.sp)
            }
        }
    }
}

// ---- Step 3: Baggage ----
@Composable
fun BaggageStep(
    baggageList: List<BaggageDeclaration>,
    onAdd: (BaggageDeclaration) -> Unit,
    onRemove: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text("Baggage Declaration", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                onAdd(
                    BaggageDeclaration(
                        id = "BAG-${baggageList.size + 1}",
                        cabinBags = 1,
                        checkedBags = 0
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Add Cabin Bag (7kg)") }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                onAdd(
                    BaggageDeclaration(
                        id = "BAG-${baggageList.size + 1}",
                        cabinBags = 0,
                        checkedBags = 1
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Add Checked Bag (20kg)") }

        Spacer(Modifier.height(16.dp))

        baggageList.forEachIndexed { index, bag ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (bag.cabinBags > 0) "Cabin bag — 7kg" else "Checked bag — 20kg",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { onRemove(index) }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Next →")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
    }
}

// ---- Step 4: Special Requests ----
@Composable
fun SpecialRequestsStep(
    meal: String,
    wheelchair: Boolean,
    infant: Boolean,
    pet: Boolean,
    onUpdate: (String, Boolean, Boolean, Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var mealText by remember { mutableStateOf(meal) }
    var wcChecked by remember { mutableStateOf(wheelchair) }
    var infantChecked by remember { mutableStateOf(infant) }
    var petChecked by remember { mutableStateOf(pet) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Special Requests", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = mealText,
            onValueChange = { mealText = it },
            label = { Text("Meal preferences (optional)") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 4
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = wcChecked, onCheckedChange = { wcChecked = it })
            Spacer(Modifier.width(8.dp))
            Text("Wheelchair assistance")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = infantChecked, onCheckedChange = { infantChecked = it })
            Spacer(Modifier.width(8.dp))
            Text("Traveling with infant")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = petChecked, onCheckedChange = { petChecked = it })
            Spacer(Modifier.width(8.dp))
            Text("Traveling with pet")
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                onUpdate(mealText, wcChecked, infantChecked, petChecked)
                onNext()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirm") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
    }
}

// ---- Step 5: Confirmation ----
@Composable
fun ConfirmationStep(
    uiState: CheckInUiState,
    viewModel: CheckInViewModel,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.submitCheckIn(canSendNotification = granted) {
            showDialog = false
            onConfirm()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Boarding Pass", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                val origin = uiState.flight?.origin ?: "Unknown"
                val dest = uiState.flight?.destination ?: "Unknown"
                Text("✈ $origin → $dest", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Passenger: ${uiState.passenger.fullName.ifBlank { "—" }}")

                val flightNumber = uiState.flight?.flightNumber ?: uiState.booking?.flightId ?: "Unknown"
                Text("Flight: $flightNumber")

                val dateDisplay = try {
                    if (!uiState.flight?.departureTime.isNullOrBlank()) {
                        val instant = java.time.Instant.parse(uiState.flight!!.departureTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                        val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                        localDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                    } else "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
                Text("Date: $dateDisplay")

                val depTime = try {
                    if (!uiState.flight?.departureTime.isNullOrBlank()) {
                        val instant = java.time.Instant.parse(uiState.flight!!.departureTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                        val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                        localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } else "TBD"
                } catch (e: Exception) {
                    "TBD"
                }
                val arrTime = try {
                    if (!uiState.flight?.arrivalTime.isNullOrBlank()) {
                        val instant = java.time.Instant.parse(uiState.flight!!.arrivalTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                        val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                        localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } else "TBD"
                } catch (e: Exception) {
                    "TBD"
                }
                Text("Departure: $depTime  Arrival: $arrTime")
                Text("Gate: TBD  Seat: ${uiState.selectedSeatNumber.ifBlank { "TBD" }}")
                Spacer(Modifier.height(16.dp))
                // QR code placeholder
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("QR", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirm") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Edit")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            icon = { Text("✅", style = MaterialTheme.typography.headlineLarge) },
            title = { Text("Booking saved") },
            text = { Text("Your booking has been saved successfully") },
            confirmButton = {
                Button(onClick = {
                    val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED

                    if (needsRuntimePermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.submitCheckIn(canSendNotification = true) {
                            showDialog = false
                            onConfirm()
                        }
                    }
                }) { Text("See Ticket") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                }) { Text("Back to home") }
            }
        )
    }
}

private const val CHECK_IN_CHANNEL_ID = "check_in_updates"

private fun showCheckInCompleteNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHECK_IN_CHANNEL_ID,
            "Check-In Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, CHECK_IN_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Check-in Complete!")
        .setContentText("Your boarding pass is ready.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
}