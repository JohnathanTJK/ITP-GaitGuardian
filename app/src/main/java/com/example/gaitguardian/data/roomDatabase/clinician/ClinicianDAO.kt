package com.example.gaitguardian.data.roomDatabase.clinician

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicianDao {
// DAO will store all the SQL queries, referenced from Chen Kan jetpackArchTest repo
//    @Query("SELECT * FROM patients_table ORDER BY id ASC")
//    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM clinician_table WHERE id = :clinicianId LIMIT 1")
    fun getClinicianById(clinicianId: Int = 1): Flow<Clinician?> // Room does not support StateFlow

    @Query("SELECT * FROM clinician_table ORDER BY id ASC")
    fun getAllClinicians(): Flow<List<Clinician>>

    @Query("UPDATE clinician_table SET id = :id WHERE name = :name")
    suspend fun updateBobID(id: Int, name: String)

    @Insert
    suspend fun insertNewClinician(clinician: Clinician)

    @Query("DELETE FROM clinician_table WHERE id = :id")
    suspend fun deleteClinician(id: Int)

    @Query("DELETE FROM clinician_table")
    suspend fun deleteTable()
}