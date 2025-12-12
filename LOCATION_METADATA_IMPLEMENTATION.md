# LiveKit Location Metadata Implementation

## Overview (கண்ணோட்டம்)

இந்த implementation LiveKit server-க்கு user-ன் location (latitude & longitude) ஐ **room.connect செய்யும் போதே** (connection ஆரம்பத்திலேயே) metadata-வாக அனுப்புகிறது.

## Changes Made (செய்யப்பட்ட மாற்றங்கள்)

### 1. LocationHelper.kt (புதிய file)
**Path:** `app/src/main/java/io/livekit/android/example/voiceassistant/LocationHelper.kt`

இந்த helper class இரண்டு முக்கிய functions செய்கிறது:

#### `getCurrentLocationMetadata(context: Context): String?`
- User-ன் current location-ஐ FusedLocationProviderClient பயன்படுத்தி எடுக்கிறது
- முதலில் last known location-ஐ try செய்கிறது (faster)
- அது கிடைக்கவில்லை என்றால், fresh location request செய்கிறது
- 5 second timeout உள்ளது
- JSON format-ல் return செய்கிறது:
```json
{
  "user_location": {
    "latitude": 12.9716,
    "longitude": 77.5946
  }
}
```

#### `formatLocationAsMetadata(latitude: Double, longitude: Double): String`
- Latitude & longitude-ஐ LiveKit metadata format-ல் JSON string-ஆக convert செய்கிறது

### 2. VoiceAssistantScreen.kt (மாற்றங்கள்)

#### Import Added:
```kotlin
import io.livekit.android.example.voiceassistant.LocationHelper
```

#### Logic Modified (Line 167-192):
Connection successful ஆன பிறகு:
1. `LocationHelper.getCurrentLocationMetadata()` call செய்து location எடுக்கிறது
2. Location கிடைத்தால், `session.room.localParticipant?.setMetadata()` பயன்படுத்தி metadata-வாக set செய்கிறது
3. Success/failure logs print செய்கிறது

## How It Works (எப்படி வேலை செய்கிறது)

### Flow:

1. **User opens VoiceAssistantScreen**
   - Location permission already granted (MainActivity-ல்)

2. **Session starts** (`session.start()`)
   - LiveKit server-க்கு connection establish ஆகிறது

3. **Connection successful ஆனவுடன்:**
   ```kotlin
   // Get current location
   val locationMetadata = LocationHelper.getCurrentLocationMetadata(context)
   
   // Set as participant metadata
   session.room.localParticipant?.setMetadata(locationMetadata)
   ```

4. **Location metadata LiveKit server-க்கு அனுப்பப்படுகிறது**
   - Server-side agent இந்த metadata-ஐ படிக்க முடியும்
   - Participant metadata-வாக store ஆகிறது

### Metadata Format:
```json
{
  "user_location": {
    "latitude": 12.9716,
    "longitude": 77.5946
  }
}
```

## Backend-ல் எப்படி படிப்பது?

### Python (LiveKit Agent):
```python
from livekit import rtc

@agent.on("participant_connected")
async def on_participant_connected(participant: rtc.RemoteParticipant):
    metadata = participant.metadata
    if metadata:
        import json
        data = json.loads(metadata)
        user_location = data.get("user_location", {})
        latitude = user_location.get("latitude")
        longitude = user_location.get("longitude")
        print(f"User location: Lat={latitude}, Lon={longitude}")
```

### Node.js (LiveKit Agent):
```javascript
room.on('participantConnected', (participant) => {
  const metadata = participant.metadata;
  if (metadata) {
    const data = JSON.parse(metadata);
    const { latitude, longitude } = data.user_location || {};
    console.log(`User location: Lat=${latitude}, Lon=${longitude}`);
  }
});
```

## Testing (டெஸ்டிங்)

### Android App:
1. Build and run the app
2. Grant location permissions
3. Connect to LiveKit room
4. Check Logcat for:
   ```
   D/VoiceAssistant: Location metadata set: {"user_location":{"latitude":12.9716,"longitude":77.5946}}
   ```

### Backend:
1. Python/Node.js agent-ல் participant metadata-ஐ log செய்யுங்கள்
2. Connection ஆனவுடன் location print ஆகும்

## Important Notes (முக்கிய குறிப்புகள்)

1. **Location Permission**: MainActivity-ல் already request செய்யப்பட்டுள்ளது
2. **Timeout**: Location fetch-க்கு 5 seconds timeout உள்ளது
3. **Fallback**: Location கிடைக்கவில்லை என்றால், metadata set ஆகாது (error log மட்டும் print ஆகும்)
4. **Timing**: Connection successful ஆன **உடனேயே** metadata set ஆகிறது
5. **Continuous Updates**: LocationService தொடர்ந்து location updates-ஐ data messages-ஆக அனுப்பும் (existing functionality)

## Difference from LocationService

### LocationService (Existing):
- Connection-க்கு **பிறகு** தொடர்ந்து location updates அனுப்புகிறது
- `room.localParticipant.publishData()` பயன்படுத்துகிறது
- 5 seconds-க்கு ஒரு முறை update
- Data messages-ஆக அனுப்பப்படுகிறது

### New Implementation (LocationHelper):
- Connection **ஆரம்பத்திலேயே** ஒரு முறை அனுப்புகிறது
- `setMetadata()` பயன்படுத்துகிறது
- Participant metadata-வாக store ஆகிறது
- Server-side agent உடனே access செய்ய முடியும்

## Files Modified:
1. ✅ `LocationHelper.kt` - Created new
2. ✅ `VoiceAssistantScreen.kt` - Modified to set metadata on connect

இப்போது உங்கள் app room.connect செய்யும் போதே location-ஐ metadata-வாக அனுப்பும்! 🎉
