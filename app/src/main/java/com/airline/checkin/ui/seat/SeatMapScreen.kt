package com.airline.checkin.ui.seat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SeatMapScreen(
    flightId: String,
    onSeatPicked: () -> Unit
) {
    // TODO Member 3: implement interactive seat map
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Seat Map", style = MaterialTheme.typography.headlineMedium)
        Text("Flight: $flightId")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSeatPicked) { Text("Pick Seat (placeholder)") }
    }
}
