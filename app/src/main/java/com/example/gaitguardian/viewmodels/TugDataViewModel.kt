package com.example.gaitguardian.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gaitguardian.api.GaitAnalysisResponse
import com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessmentRepository
import com.example.gaitguardian.data.roomDatabase.tug.subtaskDuration
import com.example.gaitguardian.data.sharedPreferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TugDataViewModel(private val tugRepository: TUGAssessmentRepository, private val appPreferencesRepository: AppPreferencesRepository) : ViewModel() {
    init {
        viewModelScope.launch {
            tugRepository.allTUGAssessments.collect { tugList ->
                _allTUGAssessments.value = tugList
                Log.d("TugVM", "Loaded ${tugList.size} assessments: $tugList")
            }
        }
        viewModelScope.launch {
            tugRepository.allTUGAnalysis.collect { tugAnalysisList ->
                _allTUGAnalysis.value = tugAnalysisList
                Log.d("TugVM", "Loaded ${tugAnalysisList.size} assessments: $tugAnalysisList")
            }
        }
    }

    // Patient
    // Using RoomDb data to get the latest and previous timing
    private val _latestTwoDurations = MutableStateFlow<List<Float>>(emptyList())
    val latestTwoDurations: StateFlow<List<Float>> = _latestTwoDurations

    fun getLatestTwoDurations()
    {
        viewModelScope.launch(Dispatchers.IO) {
            _latestTwoDurations.value = tugRepository.getLatestTwoDuration()
        }
    }

    // Medication status
    private val _onMedication = MutableStateFlow(true)
    val onMedication: StateFlow<Boolean> = _onMedication

    fun setOnMedication(status: Boolean) {
        _onMedication.value = status
        Log.d("PatientViewModel", "Medication status set to: $status")
    }

    fun updatePostAssessmentOnMedicationStatus(medication: Boolean) {
        viewModelScope.launch {
            tugRepository.updateOnMedicationStatus(medication)
        }
    }
    // Latest TUG Assessment
    private val _latestAssessment = MutableStateFlow<TUGAssessment?>(null)
    val latestAssessment: StateFlow<TUGAssessment?> = _latestAssessment

    fun getLatestTUGAssessment()
    {
        viewModelScope.launch(Dispatchers.IO) {
            _latestAssessment.value = tugRepository.getLatestAssessment()
        }
    }
    // Insert new assessment
    fun insertNewAssessment(assessment: TUGAssessment){
        viewModelScope.launch(Dispatchers.IO) {
            tugRepository.insert(assessment)
        }
    }
    // Assessment comment
    private val _assessmentComment = MutableStateFlow("")
    val assessmentComment: StateFlow<String> = _assessmentComment

    fun setAssessmentComment(comment: String) {
        _assessmentComment.value = comment
    }

    // Remove Assessment ( if video fails, need to re-upload/record)
    fun removeLastInsertedAssessment() {
        viewModelScope.launch(Dispatchers.IO) {
            tugRepository.removeLastInserted()
        }
    }
    // END PATIENT
    fun removeAllAssessments(){
        viewModelScope.launch(Dispatchers.IO) {
            tugRepository.removeAllAssessments()
            tugRepository.removeAllTUGAnalysis()
        }
    }
    // Clinician
    private val _allTUGAssessments = MutableStateFlow<List<TUGAssessment>>(emptyList())
    val allTUGAssessments: StateFlow<List<TUGAssessment>> = _allTUGAssessments

    private val _selectedTUGAssessment = MutableStateFlow<TUGAssessment?>(null)
    val selectedTUGAssessment: StateFlow<TUGAssessment?> = _selectedTUGAssessment

    // Update the TUG Assessment (Notes, Reviewed) etc.
    // Get selected assessment by Id from RoomDB
    fun loadAssessmentById(id: String) {
        viewModelScope.launch {
            val assessment = tugRepository.getAssessmentById(id)
            _selectedTUGAssessment.value = assessment
        }
    }
    fun getDisplayNumberForId(id: String): Int {
        val list = allTUGAssessments.value
            .sortedBy { it.dateTime }

        return list.indexOfFirst { it.testId == id } + 1
    }

    // Update the TUG Assessment (Notes, Reviewed) etc.
    suspend fun updateTUGReview(id: String, watchStatus: Boolean, notes: String): Boolean {
        return try {
            tugRepository.updateClinicianReview(id, watchStatus, notes)
            true // Return true on success
        } catch (e: Exception) {
            Log.e("ClinicianViewModel", "Error updating TUG review", e)
            false // Return false on error
        }
    }
    fun markMultiAsReviewed(id: String) {
        viewModelScope.launch {
            tugRepository.multiSelectMarkAsReviewed(id, true)
        }
    }
    // AssessmentInfoScreen
    private val _selectedComments = MutableStateFlow<Set<String>>(emptySet())
    val selectedComments: StateFlow<Set<String>> = _selectedComments

    fun toggleComment(comment: String) {
        _selectedComments.value = _selectedComments.value.toMutableSet().also {
            if (it.contains(comment)) it.remove(comment) else it.add(comment)
        }
        Log.d("TugViewModel", "selected comments: ${_selectedComments.value}")

    }
    // ML Analysis
    private val _response = MutableStateFlow<GaitAnalysisResponse?>(null)
    val response: StateFlow<GaitAnalysisResponse?> = _response

    private val _allTUGAnalysis = MutableStateFlow<List<TUGAnalysis>>(emptyList())
    val allTUGAnalysis: StateFlow<List<TUGAnalysis>> = _allTUGAnalysis

    var lastInsertedId by mutableStateOf<Long?>(null)
        private set
    suspend fun insertTugAnalysis(tugAnalysis: TUGAnalysis) {
        val id = tugRepository.insertTugAnalysis(tugAnalysis)
        lastInsertedId = id
    }

    suspend fun checkTugAnalysisById(testId: String): TUGAnalysis? {
        return tugRepository.getTugAnalysisById(testId)
    }

    private val _subtaskDuration = MutableStateFlow<subtaskDuration?>(null)
    val subtaskDuration: StateFlow<subtaskDuration?> = _subtaskDuration

    fun getSubtaskById(testId: String) {
        viewModelScope.launch {
            _subtaskDuration.value = tugRepository.getSubtaskById(testId)
        }
    }

    //TODO: Replace this for Result Card , it should work , similar logic as before
    suspend fun getLatestTwoTimes(): List<Double> {
        return tugRepository.getLatestTwoTimes()
    }
    fun setResponse(response: GaitAnalysisResponse) {
        _response.value = response
    }
    suspend fun getLatestTugAnalysis(): TUGAnalysis? {
        return tugRepository.getLatestTugAnalysis()
    }
    private val _latestAnalysis = MutableStateFlow<TUGAnalysis?>(null)
    val latestAnalysis: StateFlow<TUGAnalysis?> = _latestAnalysis

    init {
        viewModelScope.launch {
            tugRepository.getLatestTugAnalysisFlow().collect {
                _latestAnalysis.value = it
                Log.d("LatestAnalysis", "Latest analysis updated: $it")
            }
        }
    }

    // For creating the VM in MainActivity
    class TugDataViewModelFactory(private val tugRepository: TUGAssessmentRepository,
        private val appPreferencesRepository: AppPreferencesRepository
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TugDataViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return TugDataViewModel(tugRepository, appPreferencesRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

