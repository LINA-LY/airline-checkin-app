package com.airline.checkin.ui.booking

import android.app.DatePickerDialog
import android.content.Context

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.R
import com.airline.checkin.data.repository.AirportRepository
import com.airline.checkin.data.repository.BookingRepository
import com.airline.checkin.data.repository.DocumentRepository
import com.airline.checkin.data.repository.FlightRepository
import com.airline.checkin.domain.model.Airport
import com.airline.checkin.domain.model.BookingConfirmation
import com.airline.checkin.domain.model.Flight
import com.airline.checkin.domain.model.PassengerDocument
import com.airline.checkin.domain.model.cabinPrice
import com.airline.checkin.domain.model.economyPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

private const val ECO_MAX_EMISSIONS = 200

enum class CabinClass { ECONOMY, PREMIUM, BUSINESS }

// ── Search flights ──────────────────────────────────────────────────────────────

data class SearchFlightsUiState(
    val origin: String = "",
    val originLabel: String = "",
    val destination: String = "",
    val destinationLabel: String = "",
    val dateKey: String = "",
    val returnDateKey: String = "",
    val passengers: Int = 1,
    val cabinClass: CabinClass = CabinClass.ECONOMY,
    val error: String? = null
)

@HiltViewModel
class SearchFlightsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SearchFlightsUiState())
    val uiState = _uiState.asStateFlow()

    fun setOrigin(airport: Airport) {
        _uiState.value = _uiState.value.copy(
            origin = airport.code,
            originLabel = airport.city.ifBlank { airport.name },
            error = null
        )
    }

    fun setDestination(airport: Airport) {
        _uiState.value = _uiState.value.copy(
            destination = airport.code,
            destinationLabel = airport.city.ifBlank { airport.name },
            error = null
        )
    }

    fun updateOrigin(value: String) {
        _uiState.value = _uiState.value.copy(origin = value.uppercase(), originLabel = "", error = null)
    }

    fun updateDestination(value: String) {
        _uiState.value = _uiState.value.copy(destination = value.uppercase(), destinationLabel = "", error = null)
    }

    fun updateDateKey(value: String) {
        _uiState.value = _uiState.value.copy(dateKey = value, error = null)
    }

    fun updateReturnDateKey(value: String) {
        _uiState.value = _uiState.value.copy(returnDateKey = value, error = null)
    }

    fun updatePassengers(delta: Int) {
        val current = _uiState.value.passengers + delta
        if (current in 1..9) {
            _uiState.value = _uiState.value.copy(passengers = current, error = null)
        }
    }

    fun updateCabinClass(value: CabinClass) {
        _uiState.value = _uiState.value.copy(cabinClass = value, error = null)
    }

    fun swapCities() {
        val s = _uiState.value
        _uiState.value = s.copy(
            origin = s.destination,
            originLabel = s.destinationLabel,
            destination = s.origin,
            destinationLabel = s.originLabel,
            error = null
        )
    }

    fun validate(): Boolean {
        val s = _uiState.value
        if (s.origin.isBlank() || s.destination.isBlank()) {
            _uiState.value = s.copy(error = "Please select origin and destination")
            return false
        }
        if (s.dateKey.isBlank()) {
            _uiState.value = s.copy(error = "Please select a date")
            return false
        }
        return true
    }
}

@Composable
fun FlightSearchScreen(
    onSearch: (origin: String, destination: String, startDate: String, endDate: String, passengers: Int) -> Unit,
    viewModel: SearchFlightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pickerTarget by remember { mutableStateOf(PickerTarget.NONE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Book a flight", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AirportField(
                    label = "From",
                    value = airportLabel(uiState.origin, uiState.originLabel),
                    placeholder = "Origin",
                    onClick = { pickerTarget = PickerTarget.ORIGIN }
                )
            }
            IconButton(onClick = { viewModel.swapCities() }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap")
            }
            Box(modifier = Modifier.weight(1f)) {
                AirportField(
                    label = "To",
                    value = airportLabel(uiState.destination, uiState.destinationLabel),
                    placeholder = "Destination",
                    onClick = { pickerTarget = PickerTarget.DESTINATION }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DateField(
                    label = "From Date",
                    value = uiState.dateKey,
                    onClick = { showDatePicker(context, uiState.dateKey, viewModel::updateDateKey) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                DateField(
                    label = "To Date",
                    value = uiState.returnDateKey,
                    onClick = { showDatePicker(context, uiState.returnDateKey, viewModel::updateReturnDateKey) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Travelers", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.updatePassengers(-1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(uiState.passengers.toString())
            IconButton(onClick = { viewModel.updatePassengers(1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }

        CabinClassPicker(
            value = uiState.cabinClass,
            onChange = viewModel::updateCabinClass
        )

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (viewModel.validate()) {
                    val endDate = uiState.returnDateKey.trim().ifBlank { uiState.dateKey.trim() }
                    onSearch(
                        uiState.origin.trim(),
                        uiState.destination.trim(),
                        uiState.dateKey.trim(),
                        endDate,
                        uiState.passengers
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search flights")
        }
    }

    AirportPickerDialog(
        title = "Select origin",
        show = pickerTarget == PickerTarget.ORIGIN,
        onDismiss = { pickerTarget = PickerTarget.NONE },
        onSelect = {
            viewModel.setOrigin(it)
            pickerTarget = PickerTarget.NONE
        }
    )

    AirportPickerDialog(
        title = "Select destination",
        show = pickerTarget == PickerTarget.DESTINATION,
        onDismiss = { pickerTarget = PickerTarget.NONE },
        onSelect = {
            viewModel.setDestination(it)
            pickerTarget = PickerTarget.NONE
        }
    )
}

private enum class PickerTarget { NONE, ORIGIN, DESTINATION }

@Composable
private fun AirportField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

private fun airportLabel(code: String, label: String): String {
    if (code.isBlank()) return ""
    return if (label.isBlank()) code else "$code · $label"
}

@Composable
private fun DateField(
    label: String = "Date",
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("YYYY-MM-DD") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_date_range),
                    contentDescription = "Pick date",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CabinClassPicker(
    value: CabinClass,
    onChange: (CabinClass) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Class") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CabinClass.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class AirportPickerUiState(
    val isLoading: Boolean = false,
    val airports: List<Airport> = emptyList(),
    val filtered: List<Airport> = emptyList(),
    val query: String = "",
    val error: String? = null
)

@HiltViewModel
class AirportPickerViewModel @Inject constructor(
    private val repository: AirportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AirportPickerUiState())
    val uiState = _uiState.asStateFlow()

    fun loadAirports() {
        if (_uiState.value.airports.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val airports = repository.getAirports()
                    .sortedBy { it.code }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    airports = airports,
                    filtered = airports
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateQuery(value: String) {
        val query = value.trim()
        val filtered = if (query.isBlank()) {
            _uiState.value.airports
        } else {
            _uiState.value.airports.filter { airport ->
                airport.code.contains(query, ignoreCase = true) ||
                    airport.name.contains(query, ignoreCase = true) ||
                    airport.city.contains(query, ignoreCase = true) ||
                    airport.country.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = _uiState.value.copy(query = value, filtered = filtered)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AirportPickerDialog(
    title: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Airport) -> Unit,
    viewModel: AirportPickerViewModel = hiltViewModel()
) {
    if (!show) return
    val vmState by viewModel.uiState.collectAsState()

    // Each dialog instance keeps its own local query so From/To searches are isolated
    var localQuery by remember { mutableStateOf("") }
    val localFiltered = remember(localQuery, vmState.airports) {
        val q = localQuery.trim()
        if (q.isBlank()) vmState.airports
        else vmState.airports.filter { airport ->
            airport.code.contains(q, ignoreCase = true) ||
                airport.name.contains(q, ignoreCase = true) ||
                airport.city.contains(q, ignoreCase = true) ||
                airport.country.contains(q, ignoreCase = true)
        }
    }

    // Wrap vmState values with local overrides
    val uiState = vmState.copy(query = localQuery, filtered = localFiltered)

    LaunchedEffect(show) {
        if (show) viewModel.loadAirports()
    }

    // Full-screen overlay
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close")
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Search bar — uses local query state for isolation
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = { localQuery = it },
                    placeholder = { Text("Search by city, code, or country") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

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
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    uiState.filtered.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No airports found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        // Group by country
                        val grouped = uiState.filtered.groupBy { it.country.ifBlank { "Other" } }
                            .toSortedMap()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            grouped.forEach { (country, airports) ->
                                stickyHeader {
                                    Text(
                                        text = country,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(airports) { airport ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelect(airport) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        AirportRowHighlighted(
                                            airport = airport,
                                            query = uiState.query.trim()
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Renders an airport row with the search query highlighted in bold within the displayed text. */
@Composable
private fun AirportRowHighlighted(airport: Airport, query: String) {
    val codeAndCity = "${airport.code} · ${airport.city.ifBlank { airport.name }}"
    Text(
        text = highlightQuery(codeAndCity, query),
        fontWeight = FontWeight.SemiBold
    )
    if (airport.name.isNotBlank()) {
        Text(
            text = highlightQuery(airport.name, query),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Returns an AnnotatedString with matching substrings of [query] bolded. */
private fun highlightQuery(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val idx = lowerText.indexOf(lowerQuery, start)
        if (idx == -1) {
            builder.append(text.substring(start))
            break
        }
        builder.append(text.substring(start, idx))
        builder.pushStyle(
            androidx.compose.ui.text.SpanStyle(
                fontWeight = FontWeight.ExtraBold,
                color = androidx.compose.ui.graphics.Color.Unspecified
            )
        )
        builder.append(text.substring(idx, idx + query.length))
        builder.pop()
        start = idx + query.length
    }
    return builder.toAnnotatedString()
}


enum class StopsFilter { ANY, NONSTOP, ONE_STOP, TWO_PLUS }
enum class TimeWindow { ANY, MORNING, AFTERNOON, EVENING }
enum class SortOption { PRICE_LOW, DURATION_SHORT, DEPARTURE_EARLY, EMISSIONS_LOW }

data class FlightFilters(
    val stops: StopsFilter = StopsFilter.ANY,
    val checkedBagsOnly: Boolean = false,
    val ecoOnly: Boolean = false,
    val timeWindow: TimeWindow = TimeWindow.ANY,
    val maxPrice: Int? = null
)

data class FlightResultsUiState(
    val isLoading: Boolean = false,
    val flights: List<Flight> = emptyList(),
    val filteredFlights: List<Flight> = emptyList(),
    val filters: FlightFilters = FlightFilters(),
    val sortOption: SortOption = SortOption.PRICE_LOW,
    val priceCeiling: Int = 0,
    val error: String? = null
)

@HiltViewModel
class FlightResultsViewModel @Inject constructor(
    private val repository: FlightRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlightResultsUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFlights(origin: String, destination: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val flights = repository.searchFlights(
                    origin.uppercase(),
                    destination.uppercase(),
                    startDate.trim(),
                    endDate.trim().ifBlank { startDate.trim() }
                )
                val ceiling = flights.maxOfOrNull { it.economyPrice() } ?: 0
                val maxPrice = _uiState.value.filters.maxPrice ?: ceiling
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    flights = flights,
                    priceCeiling = ceiling,
                    filters = _uiState.value.filters.copy(maxPrice = maxPrice)
                )
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateStopsFilter(value: StopsFilter) {
        _uiState.value = _uiState.value.copy(filters = _uiState.value.filters.copy(stops = value))
        applyFiltersAndSort()
    }

    fun toggleCheckedBags() {
        val next = !_uiState.value.filters.checkedBagsOnly
        _uiState.value = _uiState.value.copy(filters = _uiState.value.filters.copy(checkedBagsOnly = next))
        applyFiltersAndSort()
    }

    fun toggleEco() {
        val next = !_uiState.value.filters.ecoOnly
        _uiState.value = _uiState.value.copy(filters = _uiState.value.filters.copy(ecoOnly = next))
        applyFiltersAndSort()
    }

    fun updateTimeWindow(value: TimeWindow) {
        _uiState.value = _uiState.value.copy(filters = _uiState.value.filters.copy(timeWindow = value))
        applyFiltersAndSort()
    }

    fun updateMaxPrice(value: Int) {
        _uiState.value = _uiState.value.copy(filters = _uiState.value.filters.copy(maxPrice = value))
        applyFiltersAndSort()
    }

    fun updateSort(value: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = value)
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var filtered = state.flights

        filtered = when (state.filters.stops) {
            StopsFilter.NONSTOP -> filtered.filter { it.stops == 0 }
            StopsFilter.ONE_STOP -> filtered.filter { it.stops == 1 }
            StopsFilter.TWO_PLUS -> filtered.filter { it.stops >= 2 }
            StopsFilter.ANY -> filtered
        }

        if (state.filters.checkedBagsOnly) {
            filtered = filtered.filter { it.checkedBagsIncluded > 0 }
        }

        if (state.filters.ecoOnly) {
            filtered = filtered.filter { it.emissionsKg in 1..ECO_MAX_EMISSIONS }
        }

        state.filters.maxPrice?.let { max ->
            if (max > 0) {
                filtered = filtered.filter { it.economyPrice() <= max }
            }
        }

        if (state.filters.timeWindow != TimeWindow.ANY) {
            filtered = filtered.filter { flight ->
                val hour = parseHour(flight.departureTime)
                hour != null && hour in timeWindowRange(state.filters.timeWindow)
            }
        }

        val sorted = when (state.sortOption) {
            SortOption.PRICE_LOW -> filtered.sortedBy { it.economyPrice() }
            SortOption.DURATION_SHORT -> filtered.sortedBy { durationMinutes(it) }
            SortOption.DEPARTURE_EARLY -> filtered.sortedBy { departureMinutes(it) }
            SortOption.EMISSIONS_LOW -> filtered.sortedBy { it.emissionsKg }
        }

        _uiState.value = state.copy(filteredFlights = sorted)
    }
}

@Composable
fun FlightResultsScreen(
    origin: String,
    destination: String,
    startDate: String,
    endDate: String,
    passengers: Int,
    onBack: () -> Unit,
    onSelectFlight: (Flight) -> Unit,
    viewModel: FlightResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(origin, destination, startDate, endDate) {
        viewModel.loadFlights(origin, destination, startDate, endDate)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text("$origin to $destination", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${startDate} to ${endDate.ifBlank { startDate }} · $passengers traveler(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FiltersBar(uiState = uiState, viewModel = viewModel)

        Spacer(Modifier.height(8.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Unknown error")
                }
            }
            uiState.filteredFlights.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No flights found. Try a different date range.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredFlights) { flight ->
                        FlightResultCard(flight = flight, onSelect = { onSelectFlight(flight) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersBar(
    uiState: FlightResultsUiState,
    viewModel: FlightResultsViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filters.stops == StopsFilter.NONSTOP,
                onClick = {
                    viewModel.updateStopsFilter(
                        if (uiState.filters.stops == StopsFilter.NONSTOP) StopsFilter.ANY else StopsFilter.NONSTOP
                    )
                },
                label = { Text("Nonstop") }
            )
            FilterChip(
                selected = uiState.filters.stops == StopsFilter.ONE_STOP,
                onClick = {
                    viewModel.updateStopsFilter(
                        if (uiState.filters.stops == StopsFilter.ONE_STOP) StopsFilter.ANY else StopsFilter.ONE_STOP
                    )
                },
                label = { Text("1 stop") }
            )
            FilterChip(
                selected = uiState.filters.stops == StopsFilter.TWO_PLUS,
                onClick = {
                    viewModel.updateStopsFilter(
                        if (uiState.filters.stops == StopsFilter.TWO_PLUS) StopsFilter.ANY else StopsFilter.TWO_PLUS
                    )
                },
                label = { Text("2+ stops") }
            )
            FilterChip(
                selected = uiState.filters.checkedBagsOnly,
                onClick = viewModel::toggleCheckedBags,
                label = { Text("Free bag") }
            )
            FilterChip(
                selected = uiState.filters.ecoOnly,
                onClick = viewModel::toggleEco,
                label = { Text("Low emissions") }
            )
        }

        if (uiState.priceCeiling > 0) {
            val maxPrice = uiState.filters.maxPrice ?: uiState.priceCeiling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Max economy price: $maxPrice")
                Slider(
                    value = maxPrice.toFloat(),
                    onValueChange = { viewModel.updateMaxPrice(it.roundToInt()) },
                    valueRange = 0f..uiState.priceCeiling.toFloat()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeWindowPicker(
                value = uiState.filters.timeWindow,
                onChange = viewModel::updateTimeWindow,
                modifier = Modifier.weight(1f)
            )
            SortPicker(
                value = uiState.sortOption,
                onChange = viewModel::updateSort,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowPicker(
    value: TimeWindow,
    onChange: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = timeWindowLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimeWindow.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(timeWindowLabel(option)) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortPicker(
    value: SortOption,
    onChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = sortLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text("Sort") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FlightResultCard(
    flight: Flight,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(flight.airline.ifBlank { "Airline" }, fontWeight = FontWeight.SemiBold)
                Text("From ${flight.currency} ${flight.economyPrice()}", fontWeight = FontWeight.SemiBold)
            }
            Text("${flight.flightNumber}  ·  ${flight.origin} -> ${flight.destination}")
            Text("${formatTime(flight.departureTime)} - ${formatTime(flight.arrivalTime)}")
            Text("${stopsLabel(flight.stops)}  ·  ${durationLabel(flight)}")
            Text(
                text = if (flight.checkedBagsIncluded > 0) "Free checked bag" else "No free checked bag",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Emissions: ${flight.emissionsKg} kg", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSelect) { Text("Details") }
            }
        }
    }
}

data class FlightDetailUiState(
    val isLoading: Boolean = false,
    val flight: Flight? = null,
    val originAirport: Airport? = null,
    val destinationAirport: Airport? = null,
    val error: String? = null
)

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val repository: FlightRepository,
    private val airportRepository: AirportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FlightDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFlight(flightId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val flight = repository.getFlight(flightId)
                // Resolve airport details
                var originAirport: Airport? = null
                var destinationAirport: Airport? = null
                try {
                    val airports = airportRepository.getAirports()
                    originAirport = airports.firstOrNull { it.code.equals(flight?.origin, ignoreCase = true) }
                    destinationAirport = airports.firstOrNull { it.code.equals(flight?.destination, ignoreCase = true) }
                } catch (_: Exception) { /* airport lookup is best-effort */ }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    flight = flight,
                    originAirport = originAirport,
                    destinationAirport = destinationAirport
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

@Composable
fun FlightDetailScreen(
    flightId: String,
    passengers: Int,
    onBack: () -> Unit,
    onSelectTicket: (CabinClass, Int) -> Unit,
    viewModel: FlightDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(flightId) {
        viewModel.loadFlight(flightId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Flight details", style = MaterialTheme.typography.titleMedium)
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Unknown error")
                }
            }
            uiState.flight == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Flight not found")
                }
            }
            else -> {
                val flight = uiState.flight!!
                val originAirport = uiState.originAirport
                val destinationAirport = uiState.destinationAirport

                // Step E — prefer pricingSummary values over multiplier fallback
                val economyFromSummary = flight.pricingSummary["ECONOMY"]?.price
                    ?: flight.pricingSummary["economy"]?.price
                val basePrice = economyFromSummary ?: flight.economyPrice()

                val options = listOf(
                    TicketOption(CabinClass.ECONOMY, "Economy", 1.0f, "Standard seat · 1 carry-on"),
                    TicketOption(CabinClass.PREMIUM, "Premium", 1.2f, "Extra comfort · Priority boarding"),
                    TicketOption(CabinClass.BUSINESS, "Business", 1.5f, "Extra space · Full recline")
                )

                // Step B — cabin selection state
                var selectedCabin by remember { mutableStateOf(CabinClass.ECONOMY) }

                // Step D — aircraft display from flight.aircraftId
                val aircraftDisplay = when (flight.aircraftId.uppercase()) {
                    "A320" -> "Airbus A320"
                    "A220" -> "Airbus A220"
                    "B737" -> "Boeing 737"
                    "E190" -> "Embraer E190"
                    "ATR72" -> "ATR 72"
                    else -> flight.aircraftId.ifBlank { "Unknown aircraft" }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Step C — redesigned header with city names
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = originAirport?.city ?: flight.origin,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${flight.origin}${originAirport?.country?.let { " · $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = destinationAirport?.city ?: flight.destination,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${flight.destination}${destinationAirport?.country?.let { " · $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(0.6f),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "✈",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "${durationLabel(flight)} · ${stopsLabel(flight.stops)} · ${formatTime(flight.departureTime)} - ${formatTime(flight.arrivalTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(flight.airline, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = if (flight.checkedBagsIncluded > 0) "${flight.checkedBagsIncluded} free checked bag(s)" else "No free checked bag",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text("Aircraft: $aircraftDisplay", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        item {
                            Spacer(Modifier.height(4.dp))
                            Text("Ticket options", style = MaterialTheme.typography.titleMedium)
                        }
                        items(options.size) { idx ->
                            val option = options[idx]
                            // Step E — prefer pricingSummary per cabin
                            val summaryPrice = flight.pricingSummary[option.cabinClass.name]?.price
                                ?: flight.pricingSummary[option.cabinClass.name.lowercase()]?.price
                            val pricePerPerson = summaryPrice?.takeIf { it > 0 }
                                ?: (basePrice * option.multiplier).roundToInt()
                            val totalPrice = pricePerPerson * passengers
                            TicketOptionCard(
                                option = option,
                                pricePerPerson = pricePerPerson,
                                totalPrice = totalPrice,
                                passengers = passengers,
                                currency = flight.currency,
                                isSelected = selectedCabin == option.cabinClass,
                                onClick = { selectedCabin = option.cabinClass }
                            )
                        }
                    }

                    Text(
                        "Seat selection is optional and can be done per passenger on the next step.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Step B — Continue button respects selected cabin
                    Button(
                        onClick = {
                            val option = options.first { it.cabinClass == selectedCabin }
                            val summaryPrice = flight.pricingSummary[option.cabinClass.name]?.price
                                ?: flight.pricingSummary[option.cabinClass.name.lowercase()]?.price
                            val price = summaryPrice?.takeIf { it > 0 }
                                ?: (basePrice * option.multiplier).roundToInt()
                            onSelectTicket(selectedCabin, price)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

data class TicketOption(
    val cabinClass: CabinClass,
    val label: String,
    val multiplier: Float,
    val note: String
)

@Composable
private fun TicketOptionCard(
    option: TicketOption,
    pricePerPerson: Int,
    totalPrice: Int,
    passengers: Int,
    currency: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant
    val borderModifier = if (isSelected)
        Modifier.fillMaxWidth()
    else
        Modifier.fillMaxWidth()

    Card(
        modifier = borderModifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(option.label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$currency $pricePerPerson",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(option.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (passengers > 1) {
                Text("$passengers travelers · Total: $currency $totalPrice", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

enum class PaymentMethod { CARD, EWALLET, BANK_TRANSFER }

data class PaymentUiState(
    val isLoading: Boolean = false,
    val isPaying: Boolean = false,
    val flight: Flight? = null,
    val method: PaymentMethod? = null,
    val demoFilled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val flights: FlightRepository,
    private val bookings: BookingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFlight(flightId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val flight = flights.getFlight(flightId)
                _uiState.value = _uiState.value.copy(isLoading = false, flight = flight)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun selectMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(method = method)
    }

    fun fillDemoCard() {
        _uiState.value = _uiState.value.copy(method = PaymentMethod.CARD, demoFilled = true)
    }

    fun pay(
        flightId: String,
        ticketsCount: Int,
        totalPrice: Int,
        currency: String,
        cabinClass: CabinClass,
        passengers: List<com.airline.checkin.domain.model.BookingPassenger>,
        onSuccess: (BookingConfirmation) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPaying = true, error = null)
            try {
                val confirmation = bookings.createBooking(
                    flightId = flightId,
                    ticketsCount = ticketsCount,
                    totalPrice = totalPrice,
                    currency = currency,
                    cabinClass = cabinClass.name,
                    passengers = passengers
                )
                _uiState.value = _uiState.value.copy(isPaying = false)
                onSuccess(confirmation)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPaying = false, error = e.message)
            }
        }
    }
}

@Composable
fun PaymentScreen(
    flightId: String,
    passengers: Int,
    cabinClass: CabinClass,
    pricePerPerson: Int,
    onBack: () -> Unit,
    onPaymentSuccess: (BookingConfirmation, Int, String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
    flowViewModel: BookingFlowViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val totalPrice = pricePerPerson * passengers

    // Payment form fields
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }
    var bankRouting by remember { mutableStateOf("") }

    LaunchedEffect(flightId) {
        viewModel.loadFlight(flightId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Payment", style = MaterialTheme.typography.titleMedium)
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Unknown error")
                }
            }
            else -> {
                val flight = uiState.flight
                val currency = flight?.currency ?: "USD"

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$currency $totalPrice",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (flight != null) {
                        Text(
                            text = "${flight.origin} -> ${flight.destination} · ${formatTime(flight.departureTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("$passengers traveler(s) · ${cabinClass.name.lowercase().replaceFirstChar { it.uppercase() }}")

                    Text("Select payment method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    PaymentMethodRow(
                        label = "Debit card",
                        selected = uiState.method == PaymentMethod.CARD,
                        onSelect = { viewModel.selectMethod(PaymentMethod.CARD) }
                    )
                    PaymentMethodRow(
                        label = "E-wallet",
                        selected = uiState.method == PaymentMethod.EWALLET,
                        onSelect = { viewModel.selectMethod(PaymentMethod.EWALLET) }
                    )
                    PaymentMethodRow(
                        label = "Bank transfer",
                        selected = uiState.method == PaymentMethod.BANK_TRANSFER,
                        onSelect = { viewModel.selectMethod(PaymentMethod.BANK_TRANSFER) }
                    )

                    // ── Payment details form (shown when a method is selected) ──
                    if (uiState.method != null) {
                        Spacer(Modifier.height(4.dp))
                        Text("Payment details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        when (uiState.method) {
                            PaymentMethod.CARD -> {
                                OutlinedTextField(
                                    value = cardNumber, onValueChange = { cardNumber = it },
                                    label = { Text("Card number") },
                                    placeholder = { Text("1234 5678 9012 3456") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = cardExpiry, onValueChange = { cardExpiry = it },
                                        label = { Text("MM/YY") },
                                        modifier = Modifier.weight(1f), singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = cardCvv, onValueChange = { cardCvv = it },
                                        label = { Text("CVV") },
                                        modifier = Modifier.weight(1f), singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = cardHolder, onValueChange = { cardHolder = it },
                                    label = { Text("Cardholder name") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        cardNumber = "4111 1111 1111 1111"
                                        cardExpiry = "12/28"
                                        cardCvv = "123"
                                        cardHolder = "DEMO USER"
                                        viewModel.fillDemoCard()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Use demo card") }
                            }
                            PaymentMethod.EWALLET -> {
                                OutlinedTextField(
                                    value = walletId, onValueChange = { walletId = it },
                                    label = { Text("Wallet ID / Phone") },
                                    placeholder = { Text("+1 234 567 890") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        walletId = "+1 555 000 1234"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Use demo wallet") }
                            }
                            PaymentMethod.BANK_TRANSFER -> {
                                OutlinedTextField(
                                    value = bankAccount, onValueChange = { bankAccount = it },
                                    label = { Text("Account number") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                OutlinedTextField(
                                    value = bankRouting, onValueChange = { bankRouting = it },
                                    label = { Text("Routing number") },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        bankAccount = "0000-1234-5678"
                                        bankRouting = "021000021"
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Use demo account") }
                            }
                            else -> {}
                        }
                    }

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.pay(
                                flightId = flightId,
                                ticketsCount = passengers,
                                totalPrice = totalPrice,
                                currency = currency,
                                cabinClass = cabinClass,
                                passengers = flowViewModel.toBookingPassengers()
                            ) { confirmation ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPaymentSuccess(confirmation, totalPrice, currency)
                            }
                        },
                        enabled = uiState.method != null && !uiState.isPaying,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isPaying) "Processing..." else "Pay now")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Text(if (selected) "Selected" else "")
        }
    }
}

@Composable
fun PaymentSuccessScreen(
    reference: String,
    amount: Int,
    currency: String,
    onSeeTicket: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Payment success", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("$currency $amount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Reference: $reference")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSeeTicket, modifier = Modifier.fillMaxWidth()) {
            Text("See ticket")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home")
        }
    }
}

private fun timeWindowLabel(value: TimeWindow): String = when (value) {
    TimeWindow.ANY -> "Any"
    TimeWindow.MORNING -> "Morning"
    TimeWindow.AFTERNOON -> "Afternoon"
    TimeWindow.EVENING -> "Evening"
}

private fun sortLabel(value: SortOption): String = when (value) {
    SortOption.PRICE_LOW -> "Price"
    SortOption.DURATION_SHORT -> "Duration"
    SortOption.DEPARTURE_EARLY -> "Departure"
    SortOption.EMISSIONS_LOW -> "Emissions"
}

private fun stopsLabel(stops: Int): String = when (stops) {
    0 -> "Nonstop"
    1 -> "1 stop"
    else -> "$stops stops"
}

private fun durationLabel(flight: Flight): String {
    val minutes = durationMinutes(flight)
    if (minutes <= 0) return "Duration: n/a"
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun durationMinutes(flight: Flight): Int {
    if (flight.durationMinutes > 0) return flight.durationMinutes
    val start = parseInstant(flight.departureTime)
    val end = parseInstant(flight.arrivalTime)
    if (start != null && end != null) {
        val diff = (end.toEpochMilli() - start.toEpochMilli()) / 60000
        return diff.toInt().coerceAtLeast(0)
    }
    return 0
}

private fun departureMinutes(flight: Flight): Int {
    val hour = parseHour(flight.departureTime) ?: return Int.MAX_VALUE
    val minute = parseMinute(flight.departureTime) ?: 0
    return hour * 60 + minute
}

private fun timeWindowRange(window: TimeWindow): IntRange = when (window) {
    TimeWindow.MORNING -> 5..11
    TimeWindow.AFTERNOON -> 12..16
    TimeWindow.EVENING -> 17..22
    TimeWindow.ANY -> 0..23
}

private fun formatTime(raw: String): String {
    parseInstant(raw)?.let {
        val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        return formatter.format(it)
    }
    return raw
}

private fun parseInstant(raw: String): Instant? {
    return runCatching { Instant.parse(raw) }.getOrNull()
}

private fun parseHour(raw: String): Int? {
    parseInstant(raw)?.let { return it.atZone(ZoneId.systemDefault()).hour }
    return raw.substringBefore(":").toIntOrNull()
}

private fun parseMinute(raw: String): Int? {
    parseInstant(raw)?.let { return it.atZone(ZoneId.systemDefault()).minute }
    val parts = raw.split(":")
    return if (parts.size > 1) parts[1].take(2).toIntOrNull() else null
}

private fun showDatePicker(
    context: Context,
    initialDateKey: String,
    onPicked: (String) -> Unit
) {
    val initialDate = runCatching { LocalDate.parse(initialDateKey) }.getOrNull()
    val calendar = Calendar.getInstance()
    val year = initialDate?.year ?: calendar.get(Calendar.YEAR)
    val month = initialDate?.monthValue?.minus(1) ?: calendar.get(Calendar.MONTH)
    val day = initialDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(
        context,
        { _, pickedYear, pickedMonth, pickedDay ->
            onPicked(formatDateKey(pickedYear, pickedMonth, pickedDay))
        },
        year,
        month,
        day
    ).show()
}

private fun formatDateKey(year: Int, monthIndex: Int, day: Int): String {
    val month = String.format("%02d", monthIndex + 1)
    val dayStr = String.format("%02d", day)
    return "$year-$month-$dayStr"
}

@HiltViewModel
class PassengerListViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val flightRepository: FlightRepository
) : ViewModel() {
    private val _docs = MutableStateFlow<List<PassengerDocument>>(emptyList())
    val docs = _docs.asStateFlow()
    private val _flight = MutableStateFlow<Flight?>(null)
    val flight = _flight.asStateFlow()

    fun loadDocs(userId: String) {
        viewModelScope.launch {
            documentRepository.observeSavedPassengers(userId).collect { _docs.value = it }
        }
    }

    fun loadFlight(flightId: String) {
        viewModelScope.launch {
            try {
                _flight.value = flightRepository.getFlight(flightId)
            } catch (_: Exception) {
                _flight.value = null
            }
        }
    }

    fun createDocument(userId: String, document: PassengerDocument) {
        viewModelScope.launch {
            if (userId.isBlank()) return@launch
            try {
                documentRepository.saveTravelerLocally(document.copy(userId = userId))
            } catch (_: Exception) {
                // keep the flow usable even if Firestore is unavailable
            }
        }
    }
}

private enum class PassengerSlotMode { PICK_SAVED, ADD_NEW, CONFIRMED }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PassengerListScreen(
    flightId: String,
    passengers: Int,
    cabin: CabinClass,
    pricePerPerson: Int,
    onBack: () -> Unit,
    onPickSeat: (index: Int, cabinClass: String) -> Unit,
    onDone: () -> Unit,
    viewModel: BookingFlowViewModel = hiltViewModel(),
    docsVm: PassengerListViewModel = hiltViewModel()
) {
    val draft by viewModel.draft.collectAsState()
    val docs by docsVm.docs.collectAsState()
    val flight by docsVm.flight.collectAsState()

    // Per-slot mode tracking
    val slotModes = remember { mutableStateListOf<PassengerSlotMode>() }

    val totalPrice = draft.passengerDrafts.sumOf { passenger ->
        if (passenger.cabinClass.isNotBlank()) (flight?.cabinPrice(passenger.cabinClass)?.takeIf { it > 0 } ?: pricePerPerson) else pricePerPerson
    }

    // Keep slotModes list in sync with the number of passengers
    LaunchedEffect(draft.passengerDrafts.size, docs.size) {
        val defaultMode = PassengerSlotMode.PICK_SAVED
        while (slotModes.size < draft.passengerDrafts.size) {
            slotModes.add(defaultMode)
        }
        while (slotModes.size > draft.passengerDrafts.size) {
            slotModes.removeAt(slotModes.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startFlow(flightId, passengers, cabin.name, pricePerPerson)
        val currentUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" } catch (_: Exception) { "" }
        if (currentUser.isNotBlank()) docsVm.loadDocs(currentUser)
        docsVm.loadFlight(flightId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Travelers", style = MaterialTheme.typography.titleMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            itemsIndexed(draft.passengerDrafts) { index, p ->
                val mode = slotModes.getOrElse(index) { PassengerSlotMode.PICK_SAVED }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row — always visible
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (index == 0) "You" else "Child $index", fontWeight = FontWeight.SemiBold)
                            if (index >= 1) {
                                TextButton(onClick = { viewModel.removePassenger(index) }) { Text("Remove child") }
                            }
                        }

                        when (mode) {
                            // ── PICK_SAVED ───────────────────────────────────────────
                            PassengerSlotMode.PICK_SAVED -> {
                                if (p.passengerName.isBlank()) {
                                    var expanded by remember(index) { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                        OutlinedTextField(
                                            value = "",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Choose a saved profile") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            docs.forEach { doc ->
                                                val displayName = listOf(doc.firstName, doc.lastName)
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" ")
                                                    .ifBlank { doc.fullName.trim() }
                                                    .ifBlank { "Unnamed Profile" }
                                                DropdownMenuItem(
                                                    text = { Text(displayName) },
                                                    onClick = {
                                                        viewModel.setPassengerDocument(index, doc)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    TextButton(onClick = {
                                        if (index < slotModes.size) slotModes[index] = PassengerSlotMode.ADD_NEW
                                    }) {
                                        Text("Enter details manually")
                                    }
                                } else {
                                    var checkedBags by remember(index) { mutableStateOf(p.hasCheckedBags) }
                                    var carryOn by remember(index) { mutableStateOf(p.hasCarryOn) }
                                    var pet by remember(index) { mutableStateOf(p.hasPet) }
                                    var wheelchair by remember(index) { mutableStateOf(p.needsWheelchair) }

                                    Text(p.passengerName, fontWeight = FontWeight.Bold)

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = carryOn, onCheckedChange = { carryOn = it })
                                        Text("Carry-on bag (7kg)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = checkedBags, onCheckedChange = { checkedBags = it })
                                        Text("Checked bag (20kg)", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = pet, onCheckedChange = { pet = it })
                                        Text("Traveling with pet", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = wheelchair, onCheckedChange = { wheelchair = it })
                                        Text("Wheelchair assistance", style = MaterialTheme.typography.bodySmall)
                                    }

                                    if (p.seatNumber.isNotBlank()) {
                                        Text("Seat: ${p.seatNumber}", style = MaterialTheme.typography.bodyMedium)
                                        TextButton(onClick = { onPickSeat(index, p.cabinClass.ifBlank { cabin.name }) }) { Text("Change seat") }
                                    } else {
                                        TextButton(onClick = { onPickSeat(index, p.cabinClass.ifBlank { cabin.name }) }) { Text("Pick seat") }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { 
                                                viewModel.setPassengerRequests(index, checkedBags, carryOn, pet, wheelchair)
                                                if (index < slotModes.size) slotModes[index] = PassengerSlotMode.CONFIRMED 
                                            },
                                            enabled = true
                                        ) { Text("Confirm") }
                                        TextButton(onClick = { viewModel.clearPassenger(index) }) { Text("Change traveler") }
                                    }
                                }
                            }

                            // ── ADD_NEW ──────────────────────────────────────────────
                            PassengerSlotMode.ADD_NEW -> {
                                var localFirstName by remember(index) { mutableStateOf(p.firstName) }
                                var localLastName by remember(index) { mutableStateOf(p.lastName) }
                                var localDocType by remember(index) { mutableStateOf("Passport") }
                                var localDocNumber by remember(index) { mutableStateOf("") }
                                var saveToAccount by remember(index) { mutableStateOf(true) }

                                OutlinedTextField(
                                    value = localFirstName,
                                    onValueChange = { localFirstName = it },
                                    label = { Text("First name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = localLastName,
                                    onValueChange = { localLastName = it },
                                    label = { Text("Last name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                DocumentTypeSelector(
                                    value = localDocType,
                                    onChange = { localDocType = it }
                                )

                                OutlinedTextField(
                                    value = localDocNumber,
                                    onValueChange = { localDocNumber = it },
                                    label = { Text("Document number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = saveToAccount,
                                        onCheckedChange = { saveToAccount = it }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Save profile for future trips",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                var checkedBags by remember(index) { mutableStateOf(p.hasCheckedBags) }
                                var carryOn by remember(index) { mutableStateOf(p.hasCarryOn) }
                                var pet by remember(index) { mutableStateOf(p.hasPet) }
                                var wheelchair by remember(index) { mutableStateOf(p.needsWheelchair) }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = carryOn, onCheckedChange = { carryOn = it })
                                    Text("Carry-on bag (7kg)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checkedBags, onCheckedChange = { checkedBags = it })
                                    Text("Checked bag (20kg)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = pet, onCheckedChange = { pet = it })
                                    Text("Traveling with pet", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = wheelchair, onCheckedChange = { wheelchair = it })
                                    Text("Wheelchair assistance", style = MaterialTheme.typography.bodySmall)
                                }

                                if (p.seatNumber.isNotBlank()) {
                                    Text("Seat: ${p.seatNumber}", style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = { 
                                        viewModel.setPassengerName(index, localFirstName.trim(), localLastName.trim())
                                        onPickSeat(index, p.cabinClass.ifBlank { cabin.name }) 
                                    }) { Text("Change seat") }
                                } else {
                                    TextButton(onClick = { 
                                        viewModel.setPassengerName(index, localFirstName.trim(), localLastName.trim())
                                        onPickSeat(index, p.cabinClass.ifBlank { cabin.name }) 
                                    }) { Text("Pick seat") }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.setPassengerName(index, localFirstName.trim(), localLastName.trim())
                                            viewModel.setPassengerRequests(index, checkedBags, carryOn, pet, wheelchair)

                                            if (saveToAccount) {
                                                val userId = try {
                                                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                                } catch (_: Exception) { "" }
                                                if (userId.isNotBlank()) {
                                                    docsVm.createDocument(
                                                        userId,
                                                        PassengerDocument(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            firstName = localFirstName.trim(),
                                                            lastName = localLastName.trim(),
                                                            fullName = listOf(localFirstName.trim(), localLastName.trim())
                                                                .filter { it.isNotBlank() }.joinToString(" "),
                                                            docType = localDocType,
                                                            docNumber = localDocNumber.trim()
                                                        )
                                                    )
                                                }
                                            }

                                            if (index < slotModes.size) {
                                                slotModes[index] = PassengerSlotMode.CONFIRMED
                                            }
                                        },
                                        enabled = localFirstName.isNotBlank() && localLastName.isNotBlank()
                                    ) { Text("Confirm") }

                                    if (docs.isNotEmpty()) {
                                        TextButton(onClick = {
                                            if (index < slotModes.size) slotModes[index] = PassengerSlotMode.PICK_SAVED
                                        }) { Text("Choose saved") }
                                    }
                                }
                            }

                            // ── CONFIRMED ────────────────────────────────────────────
                            PassengerSlotMode.CONFIRMED -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            p.passengerName.ifBlank {
                                                listOf(p.firstName, p.lastName)
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" ")
                                                    .ifBlank { if (index == 0) "You" else "Child $index" }
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                        val reqs = listOfNotNull(
                                            if (p.hasCarryOn) "Carry-on" else null,
                                            if (p.hasCheckedBags) "Bag" else null,
                                            if (p.hasPet) "Pet" else null,
                                            if (p.needsWheelchair) "Wheelchair" else null
                                        ).joinToString(", ")
                                        val reqStr = if (reqs.isNotBlank()) " · $reqs" else ""
                                        Text(
                                            "${p.cabinClass.ifBlank { cabin.name }.lowercase().replaceFirstChar { it.uppercase() }} · Seat: ${p.seatNumber.ifBlank { "No seat selected" }}$reqStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Confirmed",
                                        tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        if (index < slotModes.size) {
                                            slotModes[index] = if (docs.isNotEmpty()) PassengerSlotMode.PICK_SAVED else PassengerSlotMode.ADD_NEW
                                        }
                                    }) { Text("Edit") }

                                    TextButton(onClick = {
                                        onPickSeat(index, p.cabinClass.ifBlank { cabin.name })
                                    }) { Text("Change seat") }
                                }
                            }
                        }
                    }
                }
            }
            item {
                TextButton(
                    onClick = { viewModel.addPassenger() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add")
                    Spacer(Modifier.width(8.dp))
                    Text("Add a child")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Price summary", fontWeight = FontWeight.SemiBold)
                Text("${draft.passengerDrafts.size} traveler(s)")
                val currency = flight?.currency ?: "USD"
                draft.passengerDrafts.forEachIndexed { index, passenger ->
                    val mode = slotModes.getOrNull(index) ?: PassengerSlotMode.ADD_NEW
                    val passengerPrice = if (passenger.cabinClass.isNotBlank()) (flight?.cabinPrice(passenger.cabinClass)?.takeIf { it > 0 } ?: pricePerPerson) else pricePerPerson
                    if (mode == PassengerSlotMode.CONFIRMED) {
                        Text(
                            text = "${passenger.passengerName.ifBlank { listOf(passenger.firstName, passenger.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { if (index == 0) "You" else "Child $index" } }} · ${passenger.cabinClass.ifBlank { cabin.name }} · $currency $passengerPrice",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "${if (index == 0) "You" else "Child $index"} · Pending · $currency $passengerPrice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text("Total: $currency $totalPrice", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = "Note: If you do not choose a seat, one will be assigned by the system.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Continue to payment") }
    }
}

@Composable
private fun CabinClassSelector(
    value: String,
    onChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CabinClass.values().forEach { option ->
            val label = option.name.lowercase().replaceFirstChar { it.uppercase() }
            FilterChip(
                selected = value.equals(option.name, ignoreCase = true),
                onClick = { onChange(option.name) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTypeSelector(
    value: String,
    onChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Passport", "National ID", "Driver License")

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Document type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
