package com.example.gaitguardian.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class GaitAnalysisClient {
    companion object {
        private const val TAG = "GaitAnalysisClient"
        
        // REPLACE THIS IP WITH YOUR COMPUTER'S IP ADDRESS
        private const val BASE_URL = "http://192.168.10.142:5001/"  // Change this IP!
        
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 300L
        private const val WRITE_TIMEOUT = 300L
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(GaitAnalysisAPI::class.java)
    
    suspend fun analyzeVideo(videoFile: File): Result<GaitAnalysisResponse> {
        return try {
            Log.d(TAG, "Starting video analysis for file: ${videoFile.name} (${videoFile.length()} bytes)")
            
            val requestFile = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
            val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, requestFile)
            
            val response = api.analyzeGait(videoPart)
            
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (body.success) {
                        Log.d(TAG, "Analysis successful. Severity: ${body.severity}")
                        Result.success(body)
                    } else {
                        Log.e(TAG, "Analysis failed: ${body.error}")
                        Result.failure(Exception(body.error ?: "Unknown server error"))
                    }
                } ?: Result.failure(Exception("Empty response from server"))
            } else {
                val errorMsg = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during video analysis", e)
            Result.failure(e)
        }
    }
    
    suspend fun checkServerHealth(): Result<Boolean> {
        return try {
            val response = api.healthCheck()
            if (response.isSuccessful) {
                val healthData = response.body()
                val isHealthy = healthData?.get("status") == "healthy" && 
                               healthData["models_loaded"] == true
                Log.d(TAG, "Server health check: $isHealthy")
                Result.success(isHealthy)
            } else {
                Result.failure(Exception("Health check failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check network error", e)
            Result.failure(e)
        }
    }
}