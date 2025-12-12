# LiveKit Location Backend - Simple Receiver

## Quick Start

This is a simplified version that automatically connects to LiveKit and prints user locations to the terminal.

### 1. Install Dependencies

```bash
pip install fastapi uvicorn pydantic python-dotenv livekit livekit-api
```

### 2. Configure Room Name

Edit `.env` file and set your room name:

```bash
LIVEKIT_ROOM_NAME=voice-assistant-room
```

Make sure this matches the room name your Android app connects to!

### 3. Run the Receiver

```bash
python simple_receiver.py
```

## What You'll See

When the backend receives location data from your Android app, it will print:

```
======================================================================
📍 LOCATION #1 RECEIVED
======================================================================
👤 User ID:     android_user_123
🌍 Latitude:    12.9716
🌍 Longitude:   77.5946
🕐 Timestamp:   2025-12-12 12:15:30
🔑 Session ID:  PA_abc123xyz
======================================================================
```

## How It Works

1. **Backend connects** to LiveKit room as a participant
2. **Android app** sends location data via `LocationService.kt`
3. **LiveKit** broadcasts the data to all participants
4. **Backend receives** the data and prints it to terminal

## Troubleshooting

### No location data appearing?

1. **Check room name** - Make sure `.env` has the correct `LIVEKIT_ROOM_NAME`
2. **Check Android app** - Ensure LocationService is running and has permissions
3. **Check credentials** - Verify `LIVEKIT_URL`, `LIVEKIT_API_KEY`, and `LIVEKIT_API_SECRET` are correct
4. **Check Android logs** - Look for "Location sent to LiveKit" messages

### Connection errors?

1. Verify your LiveKit credentials in `.env`
2. Make sure your LiveKit Cloud project is active
3. Check your internet connection

## Next Steps

Once you see location data in the terminal, you can:
- Use the full `app.py` for REST API endpoints
- Store locations in a database
- Build a web dashboard to visualize locations
