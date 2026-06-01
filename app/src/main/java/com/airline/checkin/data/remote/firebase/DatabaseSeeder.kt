package com.airline.checkin.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun seedIfNeeded() {
        val flightRef = firestore.collection("flights").document("FL001")
        val batch = firestore.batch()

        batch.set(
            flightRef,
            mapOf(
                "flightNumber" to "IDN16821",
                "departure" to "CGK",
                "destination" to "DPS"
            )
        )

        batch.commit().await()
    }
}
