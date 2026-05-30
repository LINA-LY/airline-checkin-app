package com.airline.checkin.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.data.local.dao.SavedPassengerDao
import com.airline.checkin.data.repository.DocumentRepository
import com.airline.checkin.domain.model.PassengerDocument
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val savedPassengerDao: SavedPassengerDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    val userId: String
        get() = auth.currentUser?.uid ?: ""

    private val _syncStatus = MutableStateFlow("idle")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    val unsyncedCount: StateFlow<Int> = if (userId.isNotEmpty()) {
        savedPassengerDao.getAll(userId).map { list ->
            list.count { !it.syncedToRemote }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    } else {
        MutableStateFlow(0)
    }

    val savedPassengers: StateFlow<List<PassengerDocument>> = 
        if (userId.isNotEmpty()) {
            documentRepository.observeSavedPassengers(userId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    fun deleteTraveler(id: String) {
        viewModelScope.launch {
            savedPassengerDao.deleteById(id)
        }
    }

    fun triggerSync() {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            _syncStatus.value = "syncing"
            try {
                documentRepository.syncUnsyncedToFirestore(userId)
                _syncStatus.value = "done"
            } catch (e: Exception) {
                _syncStatus.value = "error"
            }
        }
    }

    fun saveTravelerLocally(doc: PassengerDocument) {
        viewModelScope.launch {
            documentRepository.saveTravelerLocally(doc)
        }
    }
}
