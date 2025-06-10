package com.example.gaitguardian.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatientViewModel(private val repository: PatientRepository) : ViewModel() {

//    val patient: StateFlow<Patient?> = repository.getPatient
//        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    init {
        viewModelScope.launch {
            repository.getPatient.collect {
                _patient.value = it
            }
        }
    }

    // function to insert patient into RoomDB,
    fun insertFirstPatient() {
        val patient = Patient( // to edit these before adding :D
            id = 1,
            name = "Sophia Tan",
            age = 45
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(patient)
        }
    }


    //TODO: Add the function to upload the TUG assessment into RoomDb

    // For creating the VM in MainActivity
    class PatientViewModelFactory(private val repository: PatientRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return PatientViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}