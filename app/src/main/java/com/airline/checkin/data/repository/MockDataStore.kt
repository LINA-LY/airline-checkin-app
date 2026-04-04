package com.airline.checkin.data.repository

import com.airline.checkin.domain.model.BoardingPass
import kotlinx.coroutines.delay

object MockDataStore {
    // This simulates fetching data from an external database
    suspend fun getMockBoardingPass(bookingId: String): BoardingPass {
        delay(1000) // Simulates a 1-second network loading time
        
        return BoardingPass(
            id = "mock_bp_001",
            bookingId = bookingId,
            passengerId = "Yanouche Sari",
            flightNumber = "IDN16821",
            seatNumber = "5B",
            gate = "12",
            boardingTime = "16:55",
            qrCode = "PNR: $bookingId | Flight: IDN16821 | Seat: 5B | Name: Yanouche Sari",
            isDownloaded = false
        )
    }
}