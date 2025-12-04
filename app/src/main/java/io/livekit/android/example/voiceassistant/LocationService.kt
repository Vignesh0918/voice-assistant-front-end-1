package io.livekit.android.example.voiceassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class LocationService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d(TAG, "LocationService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationService started")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val room = VoiceAssistantViewModel.room
                    if (room != null) {
                        val locationData = mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude
                        )
                        val locationJson = Gson().toJson(locationData)
                        scope.launch {
                            try {
                                room.localParticipant.publishData(
                                    data = locationJson.toByteArray(StandardCharsets.UTF_8),
                                    topic = "location",
                                    reliability = DataPublishReliability.RELIABLE
                                )
                                Log.d(TAG, "Published location: ${location.latitude}, ${location.longitude}")
                                
                                // Show Toast on Main Thread
                                mainHandler.post {
                                    Toast.makeText(
                                        applicationContext, 
                                        "Location sent to the backend successfully", 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error publishing location", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Room is null, cannot publish location")
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
            Log.d(TAG, "Requesting location updates")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for location updates", e)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        job.cancel()
        Log.d(TAG, "LocationService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing Location")
            .setContentText("Your location is being shared with the voice assistant.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "LocationServiceChannel"
        const val TAG = "LocationService"
    }
}
