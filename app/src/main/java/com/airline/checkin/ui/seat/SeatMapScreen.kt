package com.airline.checkin.ui.seat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.rememberScrollState
import com.airline.checkin.data.repository.FlightRepository
import com.airline.checkin.data.repository.SeatRepository
import com.airline.checkin.domain.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SeatMapUiState(
    val isLoading: Boolean = false,
    val flight: Flight? = null,
    val occupiedSeatNumbers: Set<String> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class SeatMapViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    private val seatRepository: SeatRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SeatMapUiState())
    val uiState = _uiState.asStateFlow()

    fun loadFlightAndSeats(flightId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val flight = flightRepository.getFlight(flightId)
                val seats = seatRepository.getSeats(flightId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    flight = flight,
                    occupiedSeatNumbers = seats.filter { it.isOccupied }
                        .mapNotNull { it.seatNumber.takeIf(String::isNotBlank) }.toSet()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

private data class SeatMapTemplate(
    val aircraftId: String,
    val businessRows: Int,
    val premiumRows: Int,
    val economyRows: Int,
    val seatLetters: List<String>,
    val cabinBands: List<CabinBand>
) {
    val totalRows: Int = businessRows + premiumRows + economyRows
}

private data class CabinBand(
    val label: String,
    val classKey: String,
    val rowRange: IntRange,
    val accent: Color
)

private val seatMapTemplates = listOf(
    SeatMapTemplate(
        aircraftId = "A320",
        businessRows = 1,
        premiumRows = 1,
        economyRows = 8,
        seatLetters = listOf("A", "B", "C", "D"),
        cabinBands = listOf(
            CabinBand("Business", "BUSINESS", 1..1, Color(0xFFE3F2FD)),
            CabinBand("Premium Economy", "PREMIUM", 2..2, Color(0xFFF3E5F5)),
            CabinBand("Economy", "ECONOMY", 3..10, Color(0xFFE8F5E9))
        )
    ),
    SeatMapTemplate(
        aircraftId = "B737",
        businessRows = 1,
        premiumRows = 1,
        economyRows = 7,
        seatLetters = listOf("A", "B", "C", "D"),
        cabinBands = listOf(
            CabinBand("Business", "BUSINESS", 1..1, Color(0xFFE3F2FD)),
            CabinBand("Premium Economy", "PREMIUM", 2..2, Color(0xFFF3E5F5)),
            CabinBand("Economy", "ECONOMY", 3..9, Color(0xFFE8F5E9))
        )
    ),
    SeatMapTemplate(
        aircraftId = "A220",
        businessRows = 1,
        premiumRows = 1,
        economyRows = 6,
        seatLetters = listOf("A", "B", "C", "D"),
        cabinBands = listOf(
            CabinBand("Business", "BUSINESS", 1..1, Color(0xFFE3F2FD)),
            CabinBand("Premium Economy", "PREMIUM", 2..2, Color(0xFFF3E5F5)),
            CabinBand("Economy", "ECONOMY", 3..8, Color(0xFFE8F5E9))
        )
    ),
    SeatMapTemplate(
        aircraftId = "E190",
        businessRows = 1,
        premiumRows = 1,
        economyRows = 6,
        seatLetters = listOf("A", "B", "C", "D"),
        cabinBands = listOf(
            CabinBand("Business", "BUSINESS", 1..1, Color(0xFFE3F2FD)),
            CabinBand("Premium Economy", "PREMIUM", 2..2, Color(0xFFF3E5F5)),
            CabinBand("Economy", "ECONOMY", 3..8, Color(0xFFE8F5E9))
        )
    ),
    SeatMapTemplate(
        aircraftId = "ATR72",
        businessRows = 1,
        premiumRows = 1,
        economyRows = 5,
        seatLetters = listOf("A", "B", "C", "D"),
        cabinBands = listOf(
            CabinBand("Business", "BUSINESS", 1..1, Color(0xFFE3F2FD)),
            CabinBand("Premium Economy", "PREMIUM", 2..2, Color(0xFFF3E5F5)),
            CabinBand("Economy", "ECONOMY", 3..7, Color(0xFFE8F5E9))
        )
    )
)

@Composable
fun SeatMapScreen(
    flightId: String,
    passengerIndex: Int,
    cabinClass: String,
    onSeatPicked: (seatId: String, seatNumber: String) -> Unit,
    viewModel: SeatMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCabin by remember { mutableStateOf("ALL") }
    var pendingSeat by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(flightId) {
        viewModel.loadFlightAndSeats(flightId)
    }

    val flight = uiState.flight
    val template = remember(flight?.aircraftId) {
        templateForAircraft(flight?.aircraftId.orEmpty())
    }
    val occupied = uiState.occupiedSeatNumbers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Select seat", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("Passenger ${passengerIndex + 1}", style = MaterialTheme.typography.bodySmall)
        }

        if (flight != null) {
            Text(
                text = "${flight.origin} -> ${flight.destination}  ·  ${flight.aircraftId.ifBlank { template.aircraftId }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Economy from ${flight.currency} ${displayCabinPrice("ECONOMY")}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCabin == "ALL",
                onClick = { selectedCabin = "ALL" },
                label = { Text("All") }
            )
            template.cabinBands.forEach { band ->
                FilterChip(
                    selected = selectedCabin == band.classKey,
                    onClick = { selectedCabin = band.classKey },
                    label = { Text(band.label) }
                )
            }
        }

        LegendRow()

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Unable to load seats")
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items((1..template.totalRows).toList()) { rowNumber ->
                        val cabinBand = cabinBandForRow(template, rowNumber)
                        if (selectedCabin != "ALL" && cabinBand?.classKey != selectedCabin) {
                            return@items
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cabinBand?.accent ?: MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Row $rowNumber",
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Text(
                                        text = cabinBand?.label ?: "Unknown",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                SeatRow(
                                    flightId = flightId,
                                    rowNumber = rowNumber,
                                    seatLetters = template.seatLetters,
                                    occupiedSeatNumbers = occupied,
                                    selectedCabin = selectedCabin,
                                    cabinBand = cabinBand,
                                    pendingSeatNumber = pendingSeat?.second,
                                    onSeatSelected = { id, num -> pendingSeat = id to num }
                                )
                            }
                        }
                    }
                }

                // ── Confirmation bottom bar ──
                AnimatedVisibility(visible = pendingSeat != null) {
                    val seatNum = pendingSeat?.second.orEmpty()
                    val rowNum = seatNum.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                    val seatCabinBand = cabinBandForRow(template, rowNum)
                    val cabinLabel = seatCabinBand?.label ?: "Economy"
                    val cabinKey = seatCabinBand?.classKey ?: "ECONOMY"
                    val price = displayCabinPrice(cabinKey)
                    val currency = flight?.currency ?: "USD"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Seat $seatNum selected",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$cabinLabel · $currency $price",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = {
                                pendingSeat?.let { (id, num) -> onSeatPicked(id, num) }
                            }) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun displayCabinPrice(cabinKey: String): Int {
    return when (cabinKey.uppercase()) {
        "BUSINESS" -> 300
        "PREMIUM" -> 168
        else -> 120
    }
}

@Composable
private fun SeatRow(
    flightId: String,
    rowNumber: Int,
    seatLetters: List<String>,
    occupiedSeatNumbers: Set<String>,
    selectedCabin: String,
    cabinBand: CabinBand?,
    pendingSeatNumber: String?,
    onSeatSelected: (seatId: String, seatNumber: String) -> Unit
) {
    val splitIndex = (seatLetters.size / 2).coerceAtLeast(1)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(52.dp))
        seatLetters.forEachIndexed { index, letter ->
            if (index == splitIndex) {
                Spacer(modifier = Modifier.width(14.dp))
            }
            val seatNumber = "$rowNumber$letter"
            val occupied = occupiedSeatNumbers.contains(seatNumber)
            val cabinName = cabinBand?.classKey.orEmpty()
            val enabled = !occupied && (selectedCabin == "ALL" || cabinName.isBlank() || selectedCabin == cabinName)
            val isSelected = seatNumber == pendingSeatNumber
            val seatColor = when {
                occupied -> Color(0xFFD1D5DB)
                isSelected -> Color(0xFF34D399)
                cabinName == "BUSINESS" -> Color(0xFFD9F99D)
                cabinName == "PREMIUM" -> Color(0xFFDDD6FE)
                else -> Color(0xFFBAE6FD)
            }
            Card(
                modifier = Modifier
                    .size(width = 42.dp, height = 44.dp)
                    .clickable(enabled = enabled) {
                        onSeatSelected("${flightId}_$seatNumber", seatNumber)
                    },
                colors = CardDefaults.cardColors(containerColor = seatColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = seatNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (occupied) Color(0xFF6B7280) else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendChip(color = Color(0xFFD1D5DB), text = "Occupied")
        LegendChip(color = Color(0xFFBAE6FD), text = "Available")
        LegendChip(color = Color(0xFF34D399), text = "Selected")
    }
}

@Composable
private fun LegendChip(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun templateForAircraft(aircraftId: String): SeatMapTemplate {
    return seatMapTemplates.firstOrNull { it.aircraftId.equals(aircraftId, ignoreCase = true) }
        ?: seatMapTemplates.first()
}

private fun cabinBandForRow(template: SeatMapTemplate, rowNumber: Int): CabinBand? {
    return template.cabinBands.firstOrNull { rowNumber in it.rowRange }
}

private fun normalizeCabinFilter(cabinClass: String): String {
    return when (cabinClass.uppercase()) {
        "FIRST" -> "BUSINESS"
        "PREMIUM", "PREMIUM ECONOMY" -> "PREMIUM"
        "BUSINESS" -> "BUSINESS"
        "ECONOMY" -> "ECONOMY"
        else -> "ALL"
    }
}
