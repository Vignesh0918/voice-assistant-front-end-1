package io.livekit.android.example.voiceassistant.screen

import android.app.Activity
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainScope
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.layoutId
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.livekit.android.annotations.Beta
import io.livekit.android.compose.local.SessionScope
import io.livekit.android.compose.local.requireRoom
import io.livekit.android.compose.state.SessionOptions
import io.livekit.android.compose.state.rememberAgent
import io.livekit.android.compose.state.rememberLocalMedia
import io.livekit.android.compose.state.rememberSession
import io.livekit.android.compose.state.rememberSessionMessages
import io.livekit.android.compose.ui.VideoTrackView
import io.livekit.android.example.voiceassistant.LocationService
import io.livekit.android.example.voiceassistant.rememberCanAccessLocation
import io.livekit.android.example.voiceassistant.rememberCanEnableCamera
import io.livekit.android.example.voiceassistant.rememberCanEnableMic
import io.livekit.android.example.voiceassistant.requirePermissions
import io.livekit.android.example.voiceassistant.ui.AgentVisualization
import io.livekit.android.example.voiceassistant.ui.ChatBar
import io.livekit.android.example.voiceassistant.ui.ChatLog
import io.livekit.android.example.voiceassistant.ui.ConnectionStatusIndicator
import io.livekit.android.example.voiceassistant.ui.ConnectionStatus
import io.livekit.android.example.voiceassistant.ui.ControlBar
import io.livekit.android.example.voiceassistant.viewmodel.VoiceAssistantViewModel
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.json.JSONObject
import android.util.Log

@Serializable
data class VoiceAssistantRoute(
    val sandboxId: String,
    val hardcodedUrl: String,
    val hardcodedToken: String
)

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    onEndCall: () -> Unit,
) {
    VoiceAssistant(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
        onEndCall = onEndCall
    )
}

@OptIn(Beta::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceAssistant(
    viewModel: VoiceAssistantViewModel,
    modifier: Modifier = Modifier,
    onEndCall: () -> Unit
) {
    var requestedAudio by remember { mutableStateOf(true) } // Turn on audio by default.
    var requestedVideo by remember { mutableStateOf(false) }

    requirePermissions(requestedAudio, requestedVideo, location = true)

    val canEnableMic by rememberCanEnableMic()
    val canEnableVideo by rememberCanEnableCamera()
    val canAccessLocation by rememberCanAccessLocation()

    val session = rememberSession(
        tokenSource = viewModel.tokenSource,
        options = SessionOptions(
            room = viewModel.room
        )
    )

    val context = LocalContext.current

    // Start LocationService when permission is granted
    LaunchedEffect(canAccessLocation) {
        if (canAccessLocation) {
            val intent = Intent(context, LocationService::class.java)
            context.startService(intent)
        }
    }

    // Fetch and Display Real-time Location
    var locationText by remember { mutableStateOf("Waiting for location...") }
    
    DisposableEffect(canAccessLocation) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    locationText = "Lat: ${location.latitude}\nLon: ${location.longitude}"
                }
            }
        }

        if (canAccessLocation) {
            try {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMinUpdateIntervalMillis(2000)
                    .build()
                
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                locationText = "Permission denied"
            }
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    SessionScope(session = session) { session ->

        // Track connection status
        var connectionStatus by remember { mutableStateOf(ConnectionStatus.CONNECTING) }
        var isSessionConnected by remember { mutableStateOf(false) }
        
        // Start the session when we have at least microphone permissions.
        // Permission removals kill the app, so this is a one-way transition.
        LaunchedEffect(canEnableMic) {
            if (!canEnableMic) {
                connectionStatus = ConnectionStatus.DISCONNECTED
                return@LaunchedEffect
            }

            connectionStatus = ConnectionStatus.CONNECTING
            Log.d("VoiceAssistantScreen", "Starting voice assistant session...")
            
            val result = session.start()

            // Handle if the session fails to connect.
            if (result.isFailure) {
                Log.e("VoiceAssistantScreen", "Failed to connect to session", result.exceptionOrNull())
                connectionStatus = ConnectionStatus.DISCONNECTED
                Toast.makeText(context, "Error connecting to the session.", Toast.LENGTH_SHORT).show()
                onEndCall()
            } else {
                Log.d("VoiceAssistantScreen", "Session started successfully")
            }
        }
        
        // Monitor session connection state - optimized to reduce lag
        LaunchedEffect(session) {
            try {
                session.waitUntilConnected()
                isSessionConnected = true
                connectionStatus = if (isMicEnabled) ConnectionStatus.LISTENING else ConnectionStatus.CONNECTED
                Log.d("VoiceAssistantScreen", "Session connected to voice assistant - Status: $connectionStatus")
            } catch (e: Exception) {
                Log.e("VoiceAssistantScreen", "Error waiting for connection", e)
                connectionStatus = ConnectionStatus.DISCONNECTED
            }
        }

        // End the session when leaving the screen.
        DisposableEffect(Unit) {
            onDispose {
                session.end()
                val intent = Intent(context, LocationService::class.java)
                context.stopService(intent)
            }
        }

        val room = requireRoom()
        var chatVisible by remember { mutableStateOf(false) }
        
        // Observe search results from ViewModel
        val searchResultsJson by viewModel.searchResults.collectAsState()
        var showResultsDialog by remember { mutableStateOf(false) }
        var dialogMessage by remember { mutableStateOf("") }
        
        // Show dialog when results are received
        LaunchedEffect(searchResultsJson) {
            if (searchResultsJson != null) {
                Log.d("VoiceAssistantScreen", "Search results received in UI - showing dialog")
                try {
                    val json = JSONObject(searchResultsJson)
                    val results = json.getJSONArray("results")
                    val resultCount = results.length()
                    
                    val messageBuilder = StringBuilder()
                    messageBuilder.append("Received $resultCount result(s) from backend:\n\n")
                    
                    for (i in 0 until results.length()) {
                        val result = results.getJSONObject(i)
                        val businessName = result.optString("business_name", "N/A")
                        val location = result.optJSONArray("location")
                        
                        messageBuilder.append("${i + 1}. $businessName")
                        if (location != null && location.length() >= 2) {
                            val lat = location.getDouble(1)
                            val lon = location.getDouble(0)
                            messageBuilder.append("\n   Location: Lat=$lat, Lon=$lon")
                        }
                        messageBuilder.append("\n\n")
                    }
                    
                    dialogMessage = messageBuilder.toString()
                    showResultsDialog = true
                    Log.d("VoiceAssistantScreen", "Dialog message prepared and dialog set to show")
                } catch (e: Exception) {
                    Log.e("VoiceAssistantScreen", "Error formatting dialog message", e)
                    dialogMessage = "Received results from backend:\n\n$searchResultsJson"
                    showResultsDialog = true
                }
            }
        }

        // LocalMedia provides state information about the user's local devices
        val localMedia = rememberLocalMedia()
        val isMicEnabled by localMedia::isMicrophoneEnabled
        val isCameraEnabled by localMedia::isCameraEnabled
        val isScreenShareEnabled by localMedia::isScreenShareEnabled

        LaunchedEffect(canEnableMic, requestedAudio) {
            session.waitUntilConnected()
            isSessionConnected = true
            localMedia.setMicrophoneEnabled(canEnableMic && requestedAudio)
            if (canEnableMic && requestedAudio) {
                connectionStatus = ConnectionStatus.LISTENING
                Log.d("VoiceAssistantScreen", "Microphone enabled - Voice assistant is listening")
            } else {
                connectionStatus = ConnectionStatus.CONNECTED
                Log.d("VoiceAssistantScreen", "Microphone disabled")
            }
        }
        
        // Update connection status based on mic state
        LaunchedEffect(isMicEnabled, isSessionConnected) {
            if (isSessionConnected) {
                if (isMicEnabled) {
                    connectionStatus = ConnectionStatus.LISTENING
                } else {
                    connectionStatus = ConnectionStatus.CONNECTED
                }
            }
        }

        LaunchedEffect(canEnableVideo, requestedVideo) {
            session.waitUntilConnected()
            localMedia.setCameraEnabled(canEnableVideo && requestedVideo)
        }

        // SessionMessages handles all transcriptions and chat messages
        val sessionMessages = rememberSessionMessages()

        // Agent provides state information about the agent participant.
        val agent = rememberAgent()

        val constraints = getConstraints(chatVisible, isCameraEnabled, isScreenShareEnabled)
        ConstraintLayout(
            constraintSet = constraints,
            modifier = modifier,
            animateChangesSpec = spring()
        ) {
            val coroutineScope = rememberCoroutineScope { Dispatchers.IO }

            ChatLog(
                room = room,
                messages = sessionMessages.messages,
                modifier = Modifier.layoutId(LAYOUT_ID_CHAT_LOG)
            )

            var message by rememberSaveable {
                mutableStateOf("")
            }
            ChatBar(
                value = message,
                onValueChange = { message = it },
                onChatSend = { msg ->
                    coroutineScope.launch {
                        sessionMessages.send(msg)
                    }
                    message = ""
                },
                modifier = Modifier.layoutId(LAYOUT_ID_CHAT_BAR)
            )

            // Amplitude visualization of the Assistant's voice track.
            val agentBorderAlpha by animateFloatAsState(if (chatVisible) 1f else 0f, label = "agentBorderAlpha")
            AgentVisualization(
                agent = agent,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_AGENT)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = agentBorderAlpha), RoundedCornerShape(8.dp))
            )

            val context = LocalContext.current
            val screenSharePermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val resultCode = result.resultCode
                    val data = result.data
                    if (resultCode != Activity.RESULT_OK || data == null) {
                        return@rememberLauncherForActivityResult
                    }
                    coroutineScope.launch {
                        // Agents only support one video stream at a time.
                        requestedVideo = false
                        localMedia.setScreenShareEnabled(true, ScreenCaptureParams(data))
                    }
                }

            ControlBar(
                isMicEnabled = isMicEnabled,
                onMicClick = { requestedAudio = !requestedAudio },
                localAudioTrack = localMedia.microphoneTrack,
                isCameraEnabled = isCameraEnabled,
                onCameraClick = {
                    requestedVideo = !requestedVideo
                    if (requestedVideo) {
                        // Agents only support one video stream at a time.
                        coroutineScope.launch { localMedia.setScreenShareEnabled(false) }
                    }
                },
                isScreenShareEnabled = isScreenShareEnabled,
                onScreenShareClick = {
                    if (!isScreenShareEnabled) {
                        // Screenshare permission needs to be requested each time.
                        val mediaProjectionManager = context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        screenSharePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                    } else {
                        coroutineScope.launch { localMedia.setScreenShareEnabled(false) }
                    }
                },
                isChatEnabled = chatVisible,
                onChatClick = { chatVisible = !chatVisible },
                onExitClick = onEndCall,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_CONTROL_BAR)
            )

            val cameraAlpha by animateFloatAsState(targetValue = if (isCameraEnabled) 1f else 0f, label = "Camera Alpha")
            Box(
                modifier = Modifier
                    .layoutId(LAYOUT_ID_CAMERA)
                    .clickable { localMedia.switchCamera() }
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(cameraAlpha)
            ) {
                VideoTrackView(
                    trackReference = localMedia.cameraTrack,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .fillMaxWidth(.35f)
                        .aspectRatio(1f)
                ) {
                    Icon(
                        Icons.Default.Cameraswitch,
                        tint = Color.White.copy(alpha = 0.7f),
                        contentDescription = "Flip Camera",
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                }
            }

            val screenShareAlpha by animateFloatAsState(targetValue = if (isScreenShareEnabled) 1f else 0f, label = "Screen Share Alpha")
            VideoTrackView(
                trackReference = localMedia.screenShareTrack,
                modifier = Modifier
                    .layoutId(LAYOUT_ID_SCREENSHARE)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(screenShareAlpha)
            )

            // Display Location
            Box(
                modifier = Modifier
                    .layoutId(LAYOUT_ID_LOCATION)
                    .padding(top = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = locationText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Connection Status Indicator
            ConnectionStatusIndicator(
                status = connectionStatus,
                modifier = Modifier.layoutId(LAYOUT_ID_CONNECTION_STATUS)
            )
        }
        
        // AlertDialog to show search results
        if (showResultsDialog) {
            AlertDialog(
                onDismissRequest = {
                    Log.d("VoiceAssistantScreen", "Results dialog dismissed")
                    showResultsDialog = false
                },
                title = {
                    Text("Search Results Received")
                },
                text = {
                    Text(dialogMessage)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Log.d("VoiceAssistantScreen", "OK button clicked in results dialog")
                            showResultsDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}


private const val LAYOUT_ID_AGENT = "agentVisualizer"
private const val LAYOUT_ID_CHAT_LOG = "chatLog"
private const val LAYOUT_ID_CONTROL_BAR = "controlBar"
private const val LAYOUT_ID_CHAT_BAR = "chatBar"
private const val LAYOUT_ID_CAMERA = "camera"
private const val LAYOUT_ID_SCREENSHARE = "screenshare"
private const val LAYOUT_ID_LOCATION = "location"
private const val LAYOUT_ID_CONNECTION_STATUS = "connectionStatus"

private fun getConstraints(chatVisible: Boolean, cameraVisible: Boolean, screenShareVisible: Boolean) = ConstraintSet {
    val (agentVisualizer, chatLog, controlBar, chatBar, camera, screenShare, locationBox, connectionStatus) = createRefsFor(
        LAYOUT_ID_AGENT,
        LAYOUT_ID_CHAT_LOG,
        LAYOUT_ID_CONTROL_BAR,
        LAYOUT_ID_CHAT_BAR,
        LAYOUT_ID_CAMERA,
        LAYOUT_ID_SCREENSHARE,
        LAYOUT_ID_LOCATION,
        LAYOUT_ID_CONNECTION_STATUS
    )
    val chatTopGuideline = createGuidelineFromTop(0.2f)

    constrain(locationBox) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }
    
    constrain(connectionStatus) {
        top.linkTo(locationBox.bottom, 8.dp)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
    }

    constrain(chatLog) {
        top.linkTo(chatTopGuideline)
        bottom.linkTo(chatBar.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(chatBar) {
        bottom.linkTo(controlBar.top, 16.dp)
        start.linkTo(parent.start, 16.dp)
        end.linkTo(parent.end, 16.dp)
        width = Dimension.fillToConstraints
        height = Dimension.wrapContent
    }

    constrain(controlBar) {
        bottom.linkTo(parent.bottom, 10.dp)
        start.linkTo(parent.start, 16.dp)
        end.linkTo(parent.end, 16.dp)

        width = Dimension.fillToConstraints
        height = Dimension.value(60.dp)
    }

    if (chatVisible) {
        val chain = createHorizontalChain(agentVisualizer, screenShare, camera, chainStyle = ChainStyle.Spread)

        constrain(chain) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }

        fun ConstrainScope.itemConstraints(visible: Boolean = true) {
            top.linkTo(parent.top)
            bottom.linkTo(chatTopGuideline)
            width = Dimension.percent(0.3f)
            height = Dimension.fillToConstraints
            visibility = if (visible) Visibility.Visible else Visibility.Gone
        }
        constrain(agentVisualizer) {
            itemConstraints()
        }
        constrain(camera) {
            itemConstraints(cameraVisible)
        }
        constrain(screenShare) {
            itemConstraints(screenShareVisible)
        }
    } else {
        constrain(agentVisualizer) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            height = Dimension.fillToConstraints
            width = Dimension.fillToConstraints
        }
        constrain(camera) {
            end.linkTo(parent.end, 16.dp)
            bottom.linkTo(controlBar.top, 16.dp)
            width = Dimension.percent(0.25f)
            height = Dimension.percent(0.2f)
            visibility = if (cameraVisible) Visibility.Visible else Visibility.Gone
        }
        constrain(screenShare) {
            if (cameraVisible) {
                end.linkTo(camera.start, 16.dp)
            } else {
                end.linkTo(parent.end, 16.dp)
            }
            bottom.linkTo(controlBar.top, 16.dp)
            width = Dimension.percent(0.25f)
            height = Dimension.percent(0.2f)
            visibility = if (screenShareVisible) Visibility.Visible else Visibility.Gone
        }
    }
}
