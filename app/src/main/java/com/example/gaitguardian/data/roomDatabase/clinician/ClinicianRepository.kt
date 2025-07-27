package com.example.gaitguardian.data.roomDatabase.clinician

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class ClinicianRepository(private val clinicianDao: ClinicianDao) {
    val getClinician: Flow<Clinician?> = clinicianDao.getClinicianById()
//    val allClinicians: Flow<List<Clinician>> = clinicianDao.getAllClinicians()

    @WorkerThread
    suspend fun insert(clinician: Clinician) {
        clinicianDao.insertNewClinician(clinician)
    }

    @WorkerThread
    suspend fun updateId(id: Int, name: String) {
        clinicianDao.updateBobID(id, name)
    }
    @WorkerThread
    suspend fun delete(clinician: Clinician) {
        clinicianDao.deleteClinician(clinician.id)
    }

    @WorkerThread
    suspend fun deleteAll(){
        clinicianDao.deleteTable()
    }

}