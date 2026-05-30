package com.airline.checkin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_passengers")
data class SavedPassengerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val docType: String,
    val docNumber: String,
    val nationality: String,
    val dateOfBirth: String,
    val syncedToRemote: Boolean = false
)
