package com.example.gaitguardian;

import android.app.Application
import com.example.gaitguardian.data.roomDatabase.GaitGuardianRoomDatabase
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessmentRepository
import androidx.datastore.preferences.preferencesDataStore
import com.example.gaitguardian.data.roomDatabase.AppPreferencesRepository
import com.example.gaitguardian.data.roomDatabase.clinician.ClinicianRepository

class GaitGuardian: Application() {
    val roomDb by lazy { GaitGuardianRoomDatabase.getDatabase(this) }
    val patientRepository by lazy { PatientRepository(roomDb.patientDao()) }
    val tugRepository by lazy { TUGAssessmentRepository(roomDb.tugDao(), roomDb.tugAnalysisDao()) }
    val clinicianRepository by lazy { ClinicianRepository(roomDb.clinicianDao()) }

    // TODO: Add other repositories here
    val dataStore by preferencesDataStore(name = "gaitGuardian_preferences")
    val appPreferencesRepository by lazy { AppPreferencesRepository(dataStore) }

}
