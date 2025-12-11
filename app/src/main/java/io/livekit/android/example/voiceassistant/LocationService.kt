package io.livekit.android.example.voiceassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Service for retrieving location updates and sending them to a LiveKit server.
 * This service runs in the foreground to ensure continuous location tracking.
 */
class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "LocationServiceChannel"
        private const val NOTIFICATION_ID = 12345
        private const val TOPIC_LOCATION_UPDATE = "location_update"
        
        // Location update intervals
        private const val UPDATE_INTERVAL_MS = 5000L
        private const val MIN_UPDATE_INTERVAL_MS = 2000L
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Ensure the service runs in the foreground with a notification
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationService started")
        startLocationUpdates()
        return START_STICKY
    }

    /**
     * persistent notification to keep the service running in the foreground.
     */
    private fun startForegroundService() {
        createNotificationChannel()
        
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Sending location to LiveKit...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Initializes location requests and sets up the callback.
     */
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    
                    Log.d(TAG, "Location received: $latitude, $longitude")
                    
                    // Format appropriately for LiveKit using JSON
                    val jsonObject = JSONObject()
                    jsonObject.put("lat", latitude)
                    jsonObject.put("lng", longitude)
                    
                    sendLocationToLiveKit(jsonObject.toString())
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
        }
    }

    /**
     * Publishes the location data to the LiveKit room.
     */
    private fun sendLocationToLiveKit(message: String) {
        val room = VoiceAssistantViewModel.activeRoom
        if (room != null) {
            scope.launch {
                try {
                    val data = message.toByteArray(StandardCharsets.UTF_8)
                    // reliably publish data to the room
                    room.localParticipant.publishData(data, DataPublishReliability.RELIABLE, TOPIC_LOCATION_UPDATE)
                    
                    // Optional: Show confirmation on main thread (debug only, can be spammy in production)
                     Handler(Looper.getMainLooper()).post {
                         // Toast.makeText(applicationContext, "Location Sent", Toast.LENGTH_SHORT).show()
                     }
                     Log.d(TAG, "Location sent to LiveKit: $message")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending location to LiveKit", e)
                }
            }
        } else {
            Log.w(TAG, "LiveKit Room is null, cannot send location")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationService destroyed")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}