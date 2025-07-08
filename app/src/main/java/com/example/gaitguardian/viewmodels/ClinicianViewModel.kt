package com.example.gaitguardian.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.data.roomDatabase.AppPreferencesRepository
import com.example.gaitguardian.data.roomDatabase.clinician.Clinician
import com.example.gaitguardian.data.roomDatabase.clinician.ClinicianDao
import com.example.gaitguardian.data.roomDatabase.clinician.ClinicianRepository
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClinicianViewModel(private val clinicianRepository: ClinicianRepository, private val tugRepository: TUGAssessmentRepository, private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {
    private val _clinician = MutableStateFlow<Clinician?>(null)
    val clinician: StateFlow<Clinician?> = _clinician

    // In ClinicianViewModel.kt
    private val _allClinicians = MutableStateFlow<List<Clinician>>(emptyList())
    val allClinicians: StateFlow<List<Clinician>> = _allClinicians

    // TUG
    private val _allTUGAssessments = MutableStateFlow<List<TUGAssessment>>(emptyList())
    val allTUGAssessments: StateFlow<List<TUGAssessment>> = _allTUGAssessments

    private val _selectedTUGAssessment = MutableStateFlow<TUGAssessment?>(null)
    val selectedTUGAssessment: StateFlow<TUGAssessment?> = _selectedTUGAssessment

    init {
        Log.d("ClinicianViewModel", "ClinicianVM init called")

        viewModelScope.launch {
            clinicianRepository.getClinician.collect(){
                _clinician.value = it
                Log.d("ClinicianVM", "your value is ${_clinician.value}")
            }

        }
        viewModelScope.launch {
            clinicianRepository.allClinicians.collect { clinicians ->
                _allClinicians.value = clinicians
                Log.d("ClinicianVM", "Loaded ${clinicians.size} clinicians: $clinicians")
            }
        }

        viewModelScope.launch {
            tugRepository.allTUGAssessments.collect { tugList ->
                _allTUGAssessments.value = tugList
                Log.d("ClinicianVM", "Loaded ${tugList.size} assessments: $tugList")
            }
        }
    }

    // function to insert clinician into RoomDB,
    fun insertFirstClinician() {
        val clinician = Clinician( // to edit these before adding :D
//            id = 2,
            name = "Bob Bobby",
        )
        viewModelScope.launch(Dispatchers.IO) {
            clinicianRepository.insert(clinician)
        }
    }
    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            clinicianRepository.deleteAll()
        }
    }

    fun updateId(id: Int, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            clinicianRepository.updateId(id, name)
        }
    }
    // Update the TUG Assessment (Notes, Reviewed) etc.
    // Get selected assessment by Id from RoomDB
    fun loadAssessmentById(id: Int) {
        viewModelScope.launch {
            val assessment = tugRepository.getAssessmentById(id)
            _selectedTUGAssessment.value = assessment
        }
    }
    // Update the TUG Assessment (Notes, Reviewed) etc.
    fun updateTUGReview(id: Int, watchStatus: Boolean, notes: String) {
        viewModelScope.launch {
            tugRepository.updateClinicianReview(id, watchStatus, notes)
        }
    }
    fun markMultiAsReviewed(id: Int) {
        viewModelScope.launch {
            tugRepository.multiSelectMarkAsReviewed(id, true)
        }
    }

    //Datastore Preferences
    // Store Current User
    fun saveCurrentUserView(currentUser: String) {
        viewModelScope.launch {
            appPreferencesRepository.saveCurrentUserView(currentUser)
        }
    }

    val getCurrentUserView: StateFlow<String> = appPreferencesRepository.getCurrentUserView()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

//    fun getCurrentUserView(): StateFlow<String> {
//        return appPreferencesRepository.getCurrentUserView()
//            .stateIn(viewModelScope, SharingStarted.Lazily, "")
//    }

    // For creating the VM in MainActivity
    class ClinicianViewModelFactory(private val clinicianRepository: ClinicianRepository,
                                    private val tugRepository: TUGAssessmentRepository,
                                    private val appPreferencesRepository: AppPreferencesRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClinicianViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return ClinicianViewModel(clinicianRepository,tugRepository, appPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}