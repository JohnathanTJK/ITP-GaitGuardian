package com.example.gaitguardian.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gaitguardian.data.roomDatabase.AppPreferencesRepository
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientViewModel(private val patientRepository: PatientRepository,private val tugRepository: TUGAssessmentRepository,  private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {

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
    val previousDuration: StateFlow<Int> = appPreferencesRepository.getPreviousDuration()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestDuration: StateFlow<Int> = appPreferencesRepository.getLatestDuration()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // function to insert patient into RoomDB,
    fun insertFirstPatient() {
        val patient = Patient( // to edit these before adding :D
            id = 1,
            name = "Sophia Tan",
            age = 45
        )
        viewModelScope.launch(Dispatchers.IO) {
            patientRepository.insert(patient)
        }
    }

    //TODO: Add the function to upload the TUG assessment into RoomDb
    fun insertNewAssessment(assessment: TUGAssessment){
        viewModelScope.launch(Dispatchers.IO) {
            tugRepository.insert(assessment)
        }
    }

    // Store Current User
    fun saveCurrentUserView(currentUser: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveCurrentUserView(currentUser)
        }
    }

//    fun getCurrentUserView(): StateFlow<String> {
//        return appPreferencesRepository.getCurrentUserView()
//            .stateIn(viewModelScope, SharingStarted.Lazily, "")
//    }

    // Medication status
    private val _medicationStatus = MutableStateFlow("OFF")
    val medicationStatus: StateFlow<String> = _medicationStatus

    fun setMedicationStatus(status: String) {
        _medicationStatus.value = status
    }

    // Assessment comment
    private val _assessmentComment = MutableStateFlow("")
    val assessmentComment: StateFlow<String> = _assessmentComment

    fun setAssessmentComment(comment: String) {
        _assessmentComment.value = comment
    }

    fun addRecording(duration: Int) {
        viewModelScope.launch {
            appPreferencesRepository.updateDurations(duration)
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

    val currentUserView: StateFlow<String> = appPreferencesRepository.getCurrentUserView()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // For creating the VM in MainActivity
    class PatientViewModelFactory(private val patientRepository: PatientRepository,
                                  private val tugRepository: TUGAssessmentRepository,
                                  private val appPreferencesRepository: AppPreferencesRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return PatientViewModel(patientRepository, tugRepository, appPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}