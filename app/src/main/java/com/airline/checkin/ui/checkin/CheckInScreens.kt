package com.airline.checkin.ui.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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

// ---- ViewModel ----

data class CheckInUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val error: String? = null,
    val currentStep: Int = 1,
    val passenger: Passenger = Passenger(),
    val baggageList: List<BaggageDeclaration> = emptyList(),
    val mealPreference: String = "",
    val needsWheelchair: Boolean = false,
    val travelingWithInfant: Boolean = false,
    val travelingWithPet: Boolean = false
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

    fun updatePassenger(passenger: Passenger) {
        _uiState.value = _uiState.value.copy(passenger = passenger)
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

    fun nextStep() {
        _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep + 1)
    }

    fun prevStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.value = _uiState.value.copy(currentStep = _uiState.value.currentStep - 1)
        }
    }
}

// ---- Booking Lookup Screen ----

@Composable
fun BookingLookupScreen(
    onBookingFound: (bookingId: String) -> Unit,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bookingRef by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = "Find my bookings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        // Booking ID field
        OutlinedTextField(
            value = bookingRef,
            onValueChange = { bookingRef = it },
            label = { Text("Booking reference") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // Last name field
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // Find button
        Button(
            onClick = { viewModel.lookupBooking(bookingRef, lastName) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text("Find my booking")
        }

        Spacer(Modifier.height(24.dp))

        // State 1: Loading
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // State 2: Error
        uiState.error?.let { errorMsg ->
            Text(
                text = " $errorMsg",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // State 3: Booking found → show flight card
        uiState.booking?.let { booking ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Booking ID: ${booking.reference}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Flight: ${booking.flightId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (booking.checkInStatus) " Checked In" else " Not checked in",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onBookingFound(booking.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start checking")
                    }
                }
            }
        }
    }
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

    when (uiState.currentStep) {
        1 -> PassportScanStep(onNext = { viewModel.nextStep() })
        2 -> PassengerDetailsStep(
            passenger = uiState.passenger,
            onUpdate = { viewModel.updatePassenger(it) },
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.prevStep() }
        )
        3 -> BaggageStep(
            baggageList = uiState.baggageList,
            onAdd = { viewModel.addBaggage(it) },
            onRemove = { viewModel.removeBaggage(it) },
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.prevStep() }
        )
        4 -> SpecialRequestsStep(
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
        5 -> ConfirmationStep(
            uiState = uiState,
            onConfirm = { onDone() },
            onBack = { viewModel.prevStep() }
        )
    }
}

// ---- Step 1: Passport Scan ----
@Composable
fun PassportScanStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Scan Passport", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        // Camera placeholder box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📷 Camera Preview", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Passport Scanned → Next")
        }
    }
}

// ---- Step 2: Passenger Details ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsStep(
    passenger: Passenger,
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
            Text(
                text = "Depart  •  Jakarta - Bali  •  Mon, May 7  •  16:55 - 20:30  ⏱",
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
                expanded = false, // nationality is free text, keep simple
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
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Boarding Pass", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("✈ CGK → DPS", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Passenger: ${uiState.passenger.fullName.ifBlank { "—" }}")
                Text("Flight: IDN16821")
                Text("Date: Mon, May 7")
                Text("Departure: 16:55  Arrival: 20:30")
                Text("Gate: 12  Seat: 5B")
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
                    showDialog = false
                    onConfirm()
                }) { Text("See Ticket") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onConfirm()
                }) { Text("Back to home") }
            }
        )
    }
}