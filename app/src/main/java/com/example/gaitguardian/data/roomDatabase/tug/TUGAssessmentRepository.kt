package com.example.gaitguardian.data.roomDatabase.tug

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class TUGAssessmentRepository(private val tugDao: TugDao) {
    val allTUGAssessments: Flow<List<TUGAssessment>> = tugDao.getAllTUGVideos()

    @WorkerThread
    suspend fun insert(tugAssessment: TUGAssessment) {
        tugDao.insertNewTUGAssessment(tugAssessment)
    }

    @WorkerThread
    suspend fun getAssessmentById(id: Int): TUGAssessment? {
        return tugDao.getAssessmentById(id)
    }

    @WorkerThread
    suspend fun updateClinicianReview(id: Int, watchStatus: Boolean, notes: String) {
        tugDao.updateClinicianReview(id, watchStatus, notes)
    }

    @WorkerThread
    suspend fun multiSelectMarkAsReviewed(id: Int, watchStatus: Boolean) {
        tugDao.multiSelectMarkAsReviewed(id, watchStatus)
    }

    @WorkerThread
    suspend fun getLatestTwoDuration(): List<Float> {
        return tugDao.getLatestTwoDurations()
    }
}