package com.example.gaitguardian.data.roomDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.gaitguardian.data.roomDatabase.patient.Patient
import com.example.gaitguardian.data.roomDatabase.patient.PatientDao

// TODO: entities = [Patient::class, TUGAssessment::class, TUGVideo::class] etc. if necessary
@Database(entities = [Patient::class], version = 1)
abstract class GaitGuardianRoomDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}