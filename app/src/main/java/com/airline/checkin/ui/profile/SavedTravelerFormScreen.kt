package com.airline.checkin.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airline.checkin.domain.model.PassengerDocument
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedTravelerFormScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("Passport") }
    var docNumber by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val docTypes = listOf("Passport", "National ID", "Driver's License")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Text("Add Saved Traveler", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = docType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Document type") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    docTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                docType = type
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = docNumber,
                onValueChange = { docNumber = it },
                label = { Text("Document number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nationality,
                onValueChange = { nationality = it },
                label = { Text("Nationality") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Date of Birth") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val doc = PassengerDocument(
                        id = UUID.randomUUID().toString(),
                        userId = viewModel.userId,
                        firstName = firstName,
                        lastName = lastName,
                        fullName = "$firstName $lastName".trim(),
                        docType = docType,
                        docNumber = docNumber,
                        nationality = nationality,
                        dateOfBirth = dateOfBirth
                    )
                    viewModel.saveTravelerLocally(doc)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}
