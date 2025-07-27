package com.example.gaitguardian.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface GaitAnalysisAPI {
    @Multipart
    @POST("analyze_gait")
    suspend fun analyzeGait(
        @Part video: MultipartBody.Part
    ): Response<GaitAnalysisResponse>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
    
    @GET("models/info")
    suspend fun getModelInfo(): Response<Map<String, Any>>
}
