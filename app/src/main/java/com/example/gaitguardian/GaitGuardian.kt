package com.example.gaitguardian;

import android.app.Application
import com.example.gaitguardian.data.roomDatabase.GaitGuardianRoomDatabase
import com.example.gaitguardian.data.roomDatabase.patient.PatientRepository

class GaitGuardian: Application() {
    val roomDb by lazy { GaitGuardianRoomDatabase.getDatabase(this) }
    val patientRepository by lazy { PatientRepository(roomDb.patientDao()) }
    // TODO: Add other repositories here
}
