package com.example.gaitguardian;

import android.app.Application
import com.example.gaitguardian.data.roomDatabase.GaitGuardianRoomDatabase
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessmentRepository

class GaitGuardian: Application() {
    val roomDb by lazy { GaitGuardianRoomDatabase.getDatabase(this) }
    val patientRepository by lazy { PatientRepository(roomDb.patientDao()) }
    val tugRepository by lazy { TUGAssessmentRepository(roomDb.tugDao()) }
    // TODO: Add other repositories here
}
