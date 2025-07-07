package com.example.gaitguardian.data.roomDatabase.tug

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class TUGAssessmentRepository(private val tugDao: TugDao) {
    val allTUGAssessments: Flow<List<TUGAssessment>> = tugDao.getAllTUGVideos()

    @WorkerThread
    suspend fun insert(tugAssessment: TUGAssessment) {
        tugDao.insertNewTUGAssessment(tugAssessment)
    }
}