package com.example.gaitguardian.data.roomDatabase.patient

import androidx.annotation.WorkerThread
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

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


}