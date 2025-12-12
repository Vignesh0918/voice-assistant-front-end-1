"""
HTTP Location Sender for Android
Add this code to your Android app to send location directly to backend via HTTP
This is a backup method if LiveKit connection has issues
"""

# In your Android app, add this Kotlin code to LocationService.kt:

"""
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// Add this constant at the top of LocationService class
companion object {
    private const val BACKEND_URL = "http://YOUR_BACKEND_IP:8000/location"
    // Replace YOUR_BACKEND_IP with your computer's IP address
    // Example: "http://192.168.1.100:8000/location"
}

// Add this function to send via HTTP as backup
private fun sendLocationViaHTTP(message: String) {
    scope.launch {
        try {
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = message.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(BACKEND_URL)
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent via HTTP: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "HTTP request failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending location via HTTP", e)
        }
    }
}

// Modify sendLocationToLiveKit to also send via HTTP:
private fun sendLocationToLiveKit(message: String) {
    val room = VoiceAssistantViewModel.activeRoom
    if (room != null) {
        scope.launch {
            try {
                val data = message.toByteArray(StandardCharsets.UTF_8)
                room.localParticipant.publishData(data, DataPublishReliability.RELIABLE, TOPIC_LOCATION_UPDATE)
                Log.d(TAG, "Location sent to LiveKit: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending location to LiveKit", e)
            }
        }
    } else {
        Log.w(TAG, "LiveKit Room is null, cannot send location")
    }
    
    // ALSO send via HTTP as backup
    sendLocationViaHTTP(message)
}
"""

print(__doc__)
