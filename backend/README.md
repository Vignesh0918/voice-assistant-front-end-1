# LiveKit Location Backend

A Python FastAPI backend server that integrates with LiveKit to receive and store user location data from connected participants.

## Features

✅ **LiveKit Integration** - Connects to LiveKit rooms and receives real-time location data  
✅ **REST API** - Provides endpoints for location submission and retrieval  
✅ **In-Memory Storage** - Stores location data with user tracking  
✅ **Terminal Logging** - Prints received locations to console in formatted output  
✅ **Error Handling** - Comprehensive validation and error handling  
✅ **API Documentation** - Auto-generated Swagger/OpenAPI docs  

## Installation

### 1. Install Python Dependencies

```bash
cd backend
pip install -r requirements.txt
```

### 2. Configure Environment Variables

Copy the example environment file and update with your LiveKit credentials:

```bash
cp .env.example .env
```

Edit `.env` and add your LiveKit credentials:

```env
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your_api_key_here
LIVEKIT_API_SECRET=your_api_secret_here
```

You can get these credentials from your [LiveKit Cloud Dashboard](https://cloud.livekit.io/).

## Usage

### Start the Server

```bash
python app.py
```

The server will start on `http://localhost:8000`

### API Documentation

Once running, visit:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API Endpoints

### 1. Submit Location (Manual)

```bash
POST /location
Content-Type: application/json

{
  "user_id": "user123",
  "user_location": {
    "latitude": 12.9716,
    "longitude": 77.5946
  },
  "session_id": "optional_session_id"
}
```

### 2. Connect to LiveKit Room

```bash
POST /livekit/connect?room_name=my-room
```

This connects the backend to a LiveKit room to automatically receive location data from participants.

### 3. Get User Locations

```bash
GET /location/{user_id}
```

### 4. Get Latest Location

```bash
GET /location/{user_id}/latest
```

### 5. Get All Locations

```bash
GET /locations
```

### 6. Get Statistics

```bash
GET /stats
```

## How It Works

### With LiveKit Integration

1. **Android App** → Sends location data to LiveKit room (via `LocationService.kt`)
2. **LiveKit Room** → Broadcasts data to all participants
3. **Backend Server** → Receives data as a participant and stores it
4. **Terminal Output** → Displays formatted location information

### Terminal Output Example

```
============================================================
📍 NEW LOCATION RECEIVED
============================================================
User ID:    android_user_123
Latitude:   12.9716
Longitude:  77.5946
Timestamp:  2025-12-12 11:47:30
Session:    PA_abc123xyz
============================================================
```

### Manual API Submission

You can also submit location data directly via the REST API without LiveKit:

```bash
curl -X POST "http://localhost:8000/location" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user",
    "user_location": {
      "latitude": 12.9716,
      "longitude": 77.5946
    }
  }'
```

## Architecture

```
┌─────────────────┐
│  Android App    │
│ (LocationService)│
└────────┬────────┘
         │ Sends location via LiveKit
         ▼
┌─────────────────┐
│  LiveKit Room   │
│  (Data Channel) │
└────────┬────────┘
         │ Broadcasts to participants
         ▼
┌─────────────────┐
│ Backend Server  │
│  (app.py)       │
├─────────────────┤
│ • Receives data │
│ • Stores in RAM │
│ • Logs to term  │
│ • Serves API    │
└─────────────────┘
```

## Data Models

### UserLocation
```python
{
  "latitude": float,   # -90 to 90
  "longitude": float   # -180 to 180
}
```

### LocationEntry
```python
{
  "user_id": str,
  "user_location": UserLocation,
  "timestamp": datetime,
  "session_id": str (optional)
}
```

## Error Handling

- ✅ Validates latitude (-90 to 90) and longitude (-180 to 180)
- ✅ Handles invalid JSON data
- ✅ Manages LiveKit connection errors
- ✅ Returns appropriate HTTP status codes
- ✅ Logs errors to console

## Development

### Run with Auto-Reload

```bash
uvicorn app:app --reload --host 0.0.0.0 --port 8000
```

### Test the API

```bash
# Check server status
curl http://localhost:8000/

# Submit test location
curl -X POST "http://localhost:8000/location" \
  -H "Content-Type: application/json" \
  -d '{"user_id":"test","user_location":{"latitude":12.9716,"longitude":77.5946}}'

# Get statistics
curl http://localhost:8000/stats
```

## Production Considerations

For production deployment, consider:

1. **Database Storage** - Replace in-memory storage with PostgreSQL/MongoDB
2. **Authentication** - Add API key or JWT authentication
3. **Rate Limiting** - Implement rate limiting for API endpoints
4. **Logging** - Use structured logging (e.g., with `loguru`)
5. **Monitoring** - Add health checks and metrics
6. **HTTPS** - Deploy behind a reverse proxy (nginx/Caddy)

## Troubleshooting

### LiveKit Connection Issues

If you can't connect to LiveKit:
1. Verify your credentials in `.env`
2. Check your LiveKit Cloud project is active
3. Ensure the room name matches your Android app

### No Data Received

If the backend doesn't receive location data:
1. Check Android app permissions (location access)
2. Verify `LocationService` is running on Android
3. Confirm both Android and backend are in the same LiveKit room
4. Check Android logs for data sending confirmation

## License

This project is open source and available under the MIT License.

## Support

For issues or questions:
- Check the [LiveKit Documentation](https://docs.livekit.io/)
- Join the [LiveKit Community Slack](https://livekit.io/join-slack)
