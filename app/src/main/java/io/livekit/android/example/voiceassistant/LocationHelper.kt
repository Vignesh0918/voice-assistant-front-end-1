package io.livekit.android.example.voiceassistant

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Helper object to get current location for LiveKit metadata
 */
object LocationHelper {
    private const val TAG = "LocationHelper"
    private const val LOCATION_TIMEOUT_MS = 5000L
    
    /**
     * Gets the current location and formats it as JSON metadata for LiveKit
     * Returns null if location cannot be obtained within timeout
     */
    suspend fun getCurrentLocationMetadata(context: Context): String? {
        return try {
            val location = getCurrentLocation(context)
            if (location != null) {
                formatLocationAsMetadata(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "Could not get location")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location metadata", e)
            null
        }
    }
    
    /**
     * Gets the current location using FusedLocationProviderClient
     */
    private suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        
        try {
            // First try to get last known location (faster)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && !continuation.isCompleted) {
                    Log.d(TAG, "Got last known location: ${location.latitude}, ${location.longitude}")
                    continuation.resume(location)
                    return@addOnSuccessListener
                }
                
                // If no last known location, request a fresh one
                requestFreshLocation(fusedLocationClient, continuation)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last location", e)
                if (!continuation.isCompleted) {
                    requestFreshLocation(fusedLocationClient, continuation)
                }
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            if (!continuation.isCompleted) {
                continuation.resume(null)
            }
        }
        
        continuation.invokeOnCancellation {
            Log.d(TAG, "Location request cancelled")
        }
    }
    
    /**
     * Requests a fresh location update
     */
    private fun requestFreshLocation(
        fusedLocationClient: FusedLocationProviderClient,
        continuation: kotlinx.coroutines.CancellableContinuation<Location?>
    ) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_TIMEOUT_MS)
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null && !continuation.isCompleted) {
                    Log.d(TAG, "Got fresh location: ${location.latitude}, ${location.longitude}")
                    continuation.resume(location)
                } else if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Set a timeout
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!continuation.isCompleted) {
                    Log.w(TAG, "Location request timed out")
                    continuation.resume(null)
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }
            }, LOCATION_TIMEOUT_MS)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
            if (!continuation.isCompleted) {
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Formats latitude and longitude as JSON metadata string
     */
    fun formatLocationAsMetadata(latitude: Double, longitude: Double): String {
        val userLocation = JSONObject()
        userLocation.put("latitude", latitude)
        userLocation.put("longitude", longitude)
        
        val jsonObject = JSONObject()
        jsonObject.put("user_location", userLocation)
        
        return jsonObject.toString()
    }
}
