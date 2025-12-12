"""
Quick Start Guide for LiveKit Location Backend
"""

print("""
╔════════════════════════════════════════════════════════════╗
║     LiveKit Location Backend - Quick Start Guide          ║
╚════════════════════════════════════════════════════════════╝

📋 SETUP INSTRUCTIONS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1: Run Setup
─────────────────
   Double-click: setup.bat
   
   This will:
   ✓ Create a Python virtual environment
   ✓ Install all required dependencies
   ✓ Prepare the backend for running

Step 2: Configure LiveKit Credentials
──────────────────────────────────────
   1. Copy .env.example to .env
   2. Edit .env with your LiveKit credentials:
   
      LIVEKIT_URL=wss://your-project.livekit.cloud
      LIVEKIT_API_KEY=your_api_key
      LIVEKIT_API_SECRET=your_api_secret
   
   Get credentials from: https://cloud.livekit.io/

Step 3: Start the Server
─────────────────────────
   Double-click: start.bat
   
   Server will start at: http://localhost:8000
   API Docs available at: http://localhost:8000/docs

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔄 HOW IT WORKS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   ┌─────────────────┐
   │  Android App    │  Sends location via LocationService.kt
   │  (Your Phone)   │  Format: {"user_location": {"latitude": X, "longitude": Y}}
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │  LiveKit Room   │  Broadcasts data to all participants
   │  (Cloud)        │  Topic: "location_update"
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │ Backend Server  │  Receives, stores, and logs location
   │  (This App)     │  • Stores in memory
   └─────────────────┘  • Prints to terminal
                        • Provides REST API

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📡 CONNECTING TO LIVEKIT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Once the server is running, connect to a LiveKit room:

   POST http://localhost:8000/livekit/connect?room_name=YOUR_ROOM_NAME

Or use curl:
   curl -X POST "http://localhost:8000/livekit/connect?room_name=my-room"

The backend will join the room and start receiving location data!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📍 TERMINAL OUTPUT EXAMPLE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

When location data is received, you'll see:

   ============================================================
   📍 NEW LOCATION RECEIVED
   ============================================================
   User ID:    android_user_123
   Latitude:   12.9716
   Longitude:  77.5946
   Timestamp:  2025-12-12 11:47:30
   Session:    PA_abc123xyz
   ============================================================

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🧪 TESTING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Test the API manually:

   python test_api.py

Or submit a test location:

   curl -X POST "http://localhost:8000/location" \\
     -H "Content-Type: application/json" \\
     -d '{"user_id":"test","user_location":{"latitude":12.9716,"longitude":77.5946}}'

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 API ENDPOINTS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

   GET  /                          → API information
   POST /location                  → Submit location manually
   GET  /location/{user_id}        → Get all user locations
   GET  /location/{user_id}/latest → Get latest location
   GET  /locations                 → Get all locations
   GET  /stats                     → Get statistics
   POST /livekit/connect           → Connect to LiveKit room
   GET  /livekit/status            → Check LiveKit status
   GET  /docs                      → Interactive API docs

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

❓ TROUBLESHOOTING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Problem: "Module not found" errors
Solution: Run setup.bat to install dependencies

Problem: No location data received
Solution: 
   1. Check Android app has location permissions
   2. Verify LocationService is running
   3. Ensure both Android and backend are in same room
   4. Check .env credentials are correct

Problem: Can't connect to LiveKit
Solution:
   1. Verify LIVEKIT_URL, API_KEY, and API_SECRET in .env
   2. Check your LiveKit Cloud project is active
   3. Ensure room name matches your Android app

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📖 For more information, see README.md

╔════════════════════════════════════════════════════════════╗
║  Ready to start? Run setup.bat, then start.bat!           ║
╚════════════════════════════════════════════════════════════╝
""")
