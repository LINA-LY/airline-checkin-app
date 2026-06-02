// app/src/main/java/com/airline/checkin/ui/profile/ProfileViewModel.kt
package com.airline.checkin.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.local.DocumentPreferences
import com.airline.checkin.data.repository.AuthRepository
import com.airline.checkin.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val docPrefs: DocumentPreferences
) : ViewModel() {

    val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()

    val savedPassport = docPrefs.savedPassport.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfile.value = authRepository.getUserProfile()
        }
    }

    fun deleteSavedPassport() {
        viewModelScope.launch {
            docPrefs.clear()
        }
    }
}