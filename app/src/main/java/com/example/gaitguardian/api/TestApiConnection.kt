package com.example.gaitguardian.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TestApiConnection {
    private const val TAG = "TestApiConnection"
    
    fun testConnection(context: Context) {
        val client = GaitAnalysisClient(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Testing local analysis components...")
                val result = client.checkServerHealth()
                
                result.fold(
                    onSuccess = { isHealthy ->
                        Log.d(TAG, "✅ Local analysis components ready! Status: $isHealthy")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ Local analysis initialization failed: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection test error: ${e.message}")
            }
        }
    }
}