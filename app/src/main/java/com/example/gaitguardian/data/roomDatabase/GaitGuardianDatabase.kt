package com.example.gaitguardian.data.roomDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gaitguardian.data.roomDatabase.clinician.Clinician
import com.example.gaitguardian.data.roomDatabase.clinician.ClinicianDao
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.patient.PatientDao
import com.example.gaitguardian.data.roomDatabase.tug.TUGAnalysis
import com.example.gaitguardian.data.roomDatabase.tug.TUGAssessment
import com.example.gaitguardian.data.roomDatabase.tug.TugAnalysisDao
import com.example.gaitguardian.data.roomDatabase.tug.TugDao

// TODO: entities = [Patient::class, TUGAssessment::class, TUGVideo::class] etc. if necessary
//  version number need to update if schema changes
@Database(entities = [Patient::class, TUGAssessment::class, Clinician::class, TUGAnalysis::class], version = 7)
abstract class GaitGuardianRoomDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun tugDao(): TugDao
    abstract fun clinicianDao(): ClinicianDao
    abstract fun tugAnalysisDao(): TugAnalysisDao
    // abstract fun //TODO: Add other DAOs here

    companion object {
        @Volatile
        private var INSTANCE: GaitGuardianRoomDatabase? = null

        fun getDatabase(context: Context): GaitGuardianRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GaitGuardianRoomDatabase::class.java,
                    "GaitGuardian_database"
//                ).fallbackToDestructiveMigration(true).build()
                    // TODO: Don't use fallbackToDestructiveMigration after finalising schema / final submission !
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}