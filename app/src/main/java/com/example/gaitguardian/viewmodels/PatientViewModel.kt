package com.example.gaitguardian.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.data.sharedPreferences.AppPreferencesRepository
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientViewModel(private val patientRepository: PatientRepository, private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {

//    val patient: StateFlow<Patient?> = repository.getPatient
//        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    init {
        viewModelScope.launch {
            patientRepository.getPatient.collect {
                _patient.value = it
            }
        }
    }

    // function to insert patient into RoomDB,
    fun insertPatient(patient: Patient) {
        viewModelScope.launch(Dispatchers.IO) {
            patientRepository.insert(patient)
        }
    }

    // Store Current User
    fun saveCurrentUserView(currentUser: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveCurrentUserView(currentUser)
        }
    }

    // Track First Time Privacy
    val firstPrivacyCheck: StateFlow<Boolean> = appPreferencesRepository.getFirstPrivacyCheck()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setFirstPrivacyCheck(shouldSave: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setFirstPrivacyCheck(shouldSave)
        }
    }

    // Save whether to save videos or not
    val saveVideos: StateFlow<Boolean> = appPreferencesRepository.getSaveVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setSaveVideos(shouldSave: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setSaveVideos(shouldSave)
        }
    }

    val currentUserView: StateFlow<String?> = appPreferencesRepository.getCurrentUserView()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // For creating the VM in MainActivity
    class PatientViewModelFactory(private val patientRepository: PatientRepository,
                                  private val appPreferencesRepository: AppPreferencesRepository
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return PatientViewModel(patientRepository, appPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}