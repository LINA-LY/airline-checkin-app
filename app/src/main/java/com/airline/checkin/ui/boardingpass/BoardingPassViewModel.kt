package com.airline.checkin.ui.boardingpass

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.BoardingPassRepository
import com.airline.checkin.data.repository.MockDataStore
import com.airline.checkin.domain.model.BoardingPass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardingPassUiState(
    val isLoading: Boolean = false,
    val boardingPass: BoardingPass? = null,
    val qrCodeBitmap: Bitmap? = null,
    val error: String? = null
)

@HiltViewModel
class BoardingPassViewModel @Inject constructor(
    private val boardingPassRepository: BoardingPassRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardingPassUiState())
    val uiState = _uiState.asStateFlow()

    fun loadBoardingPass(bookingId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Using MockDataStore to bypass Firebase crash.
                val boardingPass = MockDataStore.getMockBoardingPass(bookingId)
                // IMPORTANT
                // Fetching real data from Firebase/Room
                // val boardingPass = boardingPassRepository.getBoardingPass(bookingId)

                if (boardingPass != null) {
                    _uiState.value = _uiState.value.copy(boardingPass = boardingPass)

                    val qrBitmap = QrCodeGenerator.generateQrCodeBitmap(boardingPass.qrCode)
                    _uiState.value = _uiState.value.copy(qrCodeBitmap = qrBitmap)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Boarding pass not found for booking: $bookingId"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "An unexpected error occurred"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}