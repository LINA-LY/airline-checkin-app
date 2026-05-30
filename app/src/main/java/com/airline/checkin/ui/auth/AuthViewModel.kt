package com.airline.checkin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val requiresProfile: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName = _displayName.asStateFlow()

    init {
        loadDisplayName()
    }

    fun loadDisplayName() {
        viewModelScope.launch {
            _displayName.value = repository.getUserDisplayName()
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                repository.signIn(email, password)
                loadDisplayName()
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }

    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                repository.registerWithProfile(email, password, firstName, lastName, phone)
                loadDisplayName()
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val result = repository.signInWithGoogle(idToken)
                if (result.isSuccess) loadDisplayName()
                _uiState.value = if (result.isSuccess) {
                    AuthUiState(isSuccess = !result.requiresProfile, requiresProfile = result.requiresProfile)
                } else {
                    AuthUiState(error = "Google sign-in failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }

    fun saveProfile(firstName: String, lastName: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                repository.saveUserProfile(firstName, lastName, phone)
                loadDisplayName()
                _uiState.value = AuthUiState(isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message)
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _displayName.value = null
        _uiState.value = AuthUiState()
    }

    fun clearStatus() {
        _uiState.value = AuthUiState()
    }
}
