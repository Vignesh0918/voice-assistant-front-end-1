package io.livekit.android.example.voiceassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.example.voiceassistant.screen.VoiceAssistantRoute
import io.livekit.android.room.Room
import io.livekit.android.token.TokenSource
import io.livekit.android.token.cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Log

/**
 * This ViewModel handles holding onto the Room object, so that it is
 * maintained across configuration changes, such as rotation.
 */
class VoiceAssistantViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val room: Room = LiveKit.create(application)
    val tokenSource: TokenSource
    
    // StateFlow to hold search results for UI observation
    private val _searchResults = MutableStateFlow<String?>(null)
    val searchResults: StateFlow<String?> = _searchResults.asStateFlow()

    init {
        activeRoom = room
        val (sandboxId, url, token) = savedStateHandle.toRoute<VoiceAssistantRoute>()

        tokenSource = if (sandboxId.isNotEmpty()) {
            TokenSource.fromSandboxTokenServer(sandboxId = sandboxId).cached()
        } else {
            TokenSource.fromLiteral(url, token).cached()
        }

        // Coroutine to collect room events
        CoroutineScope(Dispatchers.IO).launch {
            room.events.collect { event ->
                if (event is RoomEvent.DataReceived) {
                    val topic = event.topic
                    Log.d("VoiceAssistant", "Data received from backend - Topic: $topic")
                    
                    if (topic == "search_results") {
                        try {
                            val dataString = String(event.data, Charsets.UTF_8)
                            Log.d("VoiceAssistant", "Received JSON data from backend: $dataString")
                            
                            val json = JSONObject(dataString)
                            Log.d("VoiceAssistant", "Successfully parsed JSON object")
                            
                            // Emit the results to StateFlow for UI observation
                            _searchResults.value = dataString
                            Log.d("VoiceAssistant", "Search results emitted to StateFlow")
                            
                            val results = json.getJSONArray("results")
                            val resultCount = results.length()
                            Log.d("VoiceAssistant", "Number of results: $resultCount")
                            
                            for (i in 0 until results.length()) {
                                val result = results.getJSONObject(i)
                                val businessName = result.getString("business_name")
                                val location = result.getJSONArray("location")
                                val lon = location.getDouble(0)
                                val lat = location.getDouble(1)
                                Log.d("VoiceAssistant", "Result $i - Business: $businessName, Lat: $lat, Lon: $lon")
                            }
                            
                            Log.d("VoiceAssistant", "All search results processed successfully")
                        } catch (e: Exception) {
                            Log.e("VoiceAssistant", "Error parsing JSON data from backend", e)
                            Log.e("VoiceAssistant", "Exception message: ${e.message}", e)
                            e.printStackTrace()
                        }
                    } else {
                        Log.d("VoiceAssistant", "Received data with different topic: $topic, ignoring")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        room.disconnect()
        room.release()
        activeRoom = null
    }

    companion object {
        var activeRoom: Room? = null
    }
}