package com.example.gaitguardian.data.roomDatabase.patient

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class PatientRepository(private val patientDao: PatientDao) {
//    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()
    val getPatient: Flow<Patient?> = patientDao.getPatientById()
    @WorkerThread
    suspend fun insert(patient: Patient) {
        patientDao.insertNewPatient(patient)
    }

    @WorkerThread
    suspend fun delete(patient: Patient) {
        patientDao.deletePatient(patient.id)
    }

    //TODO: DataStore Preferences methods can go here if needed
}