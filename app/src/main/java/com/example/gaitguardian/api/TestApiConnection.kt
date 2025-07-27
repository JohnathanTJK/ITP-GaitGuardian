package com.example.gaitguardian.api

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TestApiConnection {
    private const val TAG = "TestApiConnection"
    
    fun testConnection() {
        val client = GaitAnalysisClient()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Testing API connection...")
                val result = client.checkServerHealth()
                
                result.fold(
                    onSuccess = { isHealthy ->
                        Log.d(TAG, "✅ API Connection successful! Server healthy: $isHealthy")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ API Connection failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection test error: ${e.message}")
            }
        }
    }
}