package com.example.gaitguardian.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.data.sharedPreferences.AppPreferencesRepository
import com.example.gaitguardian.data.roomDatabase.clinician.Clinician
import com.example.gaitguardian.data.roomDatabase.clinician.ClinicianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClinicianViewModel(private val clinicianRepository: ClinicianRepository, private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {
    private val _clinician = MutableStateFlow<Clinician?>(null)
    val clinician: StateFlow<Clinician?> = _clinician


    init {
        Log.d("ClinicianViewModel", "ClinicianVM init called")
        viewModelScope.launch {
            clinicianRepository.getClinician.collect {
                _clinician.value = it
                Log.d("ClinicianVM", "your value is ${_clinician.value}")
            }

        }
    }

    // Function to insert Clinician into RoomDB (used in StartScreen.kt)
    fun insertClinician(clinician: Clinician) {
        viewModelScope.launch(Dispatchers.IO) {
            clinicianRepository.insert(clinician)
        }
    }

    //Datastore Preferences
    // Getter Setter to retrieve Current User
    fun saveCurrentUserView(currentUser: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveCurrentUserView(currentUser)
        }
    }

    val getCurrentUserView: StateFlow<String?> = appPreferencesRepository.getCurrentUserView()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")


    // For creating the VM in MainActivity
    class ClinicianViewModelFactory(private val clinicianRepository: ClinicianRepository,
                                    private val appPreferencesRepository: AppPreferencesRepository
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClinicianViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return ClinicianViewModel(clinicianRepository, appPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}