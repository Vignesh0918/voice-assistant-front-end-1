"""
LiveKit Location Backend Server
================================
This backend server receives user location data from LiveKit participants
and stores it in memory while logging to the terminal.

Author: Backend Developer
Date: 2025-12-12
"""

from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, validator
from typing import Dict, List, Optional
from datetime import datetime
import uvicorn
import json
import asyncio
from livekit import api, rtc
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Initialize FastAPI app
app = FastAPI(
    title="LiveKit Location Backend",
    description="Backend service for receiving and storing user location data from LiveKit",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================================
# MODELS
# ============================================================================

class UserLocation(BaseModel):
    """Model for user location data"""
    latitude: float = Field(..., ge=-90, le=90, description="Latitude coordinate")
    longitude: float = Field(..., ge=-180, le=180, description="Longitude coordinate")
    
    @validator('latitude', 'longitude')
    def validate_coordinates(cls, v):
        if not isinstance(v, (int, float)):
            raise ValueError('Coordinate must be a number')
        return v


class LocationEntry(BaseModel):
    """Model for stored location entry"""
    user_id: str
    user_location: UserLocation
    timestamp: datetime
    session_id: Optional[str] = None


class LocationRequest(BaseModel):
    """Model for incoming location requests"""
    user_location: UserLocation
    user_id: Optional[str] = "anonymous"
    session_id: Optional[str] = None


# ============================================================================
# IN-MEMORY STORAGE
# ============================================================================

class LocationStorage:
    """In-memory storage for user locations"""
    
    def __init__(self):
        self.locations: Dict[str, List[LocationEntry]] = {}
        self.total_entries = 0
    
    def add_location(self, user_id: str, location: UserLocation, session_id: Optional[str] = None) -> LocationEntry:
        """Add a new location entry"""
        entry = LocationEntry(
            user_id=user_id,
            user_location=location,
            timestamp=datetime.now(),
            session_id=session_id
        )
        
        if user_id not in self.locations:
            self.locations[user_id] = []
        
        self.locations[user_id].append(entry)
        self.total_entries += 1
        
        # Print to terminal
        self._print_location(entry)
        
        return entry
    
    def _print_location(self, entry: LocationEntry):
        """Print location to terminal with formatting"""
        print("\n" + "="*60)
        print(f"📍 NEW LOCATION RECEIVED")
        print("="*60)
        print(f"User ID:    {entry.user_id}")
        print(f"Latitude:   {entry.user_location.latitude}")
        print(f"Longitude:  {entry.user_location.longitude}")
        print(f"Timestamp:  {entry.timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
        if entry.session_id:
            print(f"Session:    {entry.session_id}")
        print("="*60 + "\n")
    
    def get_user_locations(self, user_id: str) -> List[LocationEntry]:
        """Get all locations for a specific user"""
        return self.locations.get(user_id, [])
    
    def get_latest_location(self, user_id: str) -> Optional[LocationEntry]:
        """Get the most recent location for a user"""
        user_locations = self.locations.get(user_id, [])
        return user_locations[-1] if user_locations else None
    
    def get_all_locations(self) -> Dict[str, List[LocationEntry]]:
        """Get all stored locations"""
        return self.locations
    
    def get_stats(self) -> Dict:
        """Get storage statistics"""
        return {
            "total_users": len(self.locations),
            "total_entries": self.total_entries,
            "users": list(self.locations.keys())
        }


# Initialize storage
storage = LocationStorage()


# ============================================================================
# LIVEKIT INTEGRATION
# ============================================================================

class LiveKitLocationReceiver:
    """Handles LiveKit room connection and location data reception"""
    
    def __init__(self, url: str, api_key: str, api_secret: str):
        self.url = url
        self.api_key = api_key
        self.api_secret = api_secret
        self.room = None
        self.is_connected = False
    
    async def connect_to_room(self, room_name: str):
        """Connect to a LiveKit room and listen for location data"""
        try:
            # Generate token for the backend participant
            token = api.AccessToken(self.api_key, self.api_secret) \
                .with_identity("location-backend") \
                .with_name("Location Backend Service") \
                .with_grants(api.VideoGrants(
                    room_join=True,
                    room=room_name,
                )).to_jwt()
            
            # Create room instance
            self.room = rtc.Room()
            
            # Set up event handlers
            @self.room.on("data_received")
            def on_data_received(data: rtc.DataPacket):
                self._handle_data_packet(data)
            
            @self.room.on("participant_connected")
            def on_participant_connected(participant: rtc.RemoteParticipant):
                print(f"✅ Participant connected: {participant.identity}")
            
            @self.room.on("participant_disconnected")
            def on_participant_disconnected(participant: rtc.RemoteParticipant):
                print(f"❌ Participant disconnected: {participant.identity}")
            
            # Connect to the room
            await self.room.connect(self.url, token)
            self.is_connected = True
            print(f"🎉 Connected to LiveKit room: {room_name}")
            
        except Exception as e:
            print(f"❌ Error connecting to LiveKit room: {e}")
            raise
    
    def _handle_data_packet(self, packet: rtc.DataPacket):
        """Handle incoming data packets from LiveKit"""
        try:
            # Decode the data
            data_str = packet.data.decode('utf-8')
            data_json = json.loads(data_str)
            
            # Check if it's location data
            if "user_location" in data_json:
                location_data = data_json["user_location"]
                user_location = UserLocation(
                    latitude=location_data["latitude"],
                    longitude=location_data["longitude"]
                )
                
                # Store the location
                user_id = packet.participant.identity if packet.participant else "unknown"
                storage.add_location(
                    user_id=user_id,
                    location=user_location,
                    session_id=packet.participant.sid if packet.participant else None
                )
                
        except json.JSONDecodeError:
            print(f"⚠️  Received non-JSON data: {packet.data}")
        except Exception as e:
            print(f"❌ Error handling data packet: {e}")
    
    async def disconnect(self):
        """Disconnect from the LiveKit room"""
        if self.room:
            await self.room.disconnect()
            self.is_connected = False
            print("👋 Disconnected from LiveKit room")


# Global LiveKit receiver instance
livekit_receiver: Optional[LiveKitLocationReceiver] = None


# ============================================================================
# API ENDPOINTS
# ============================================================================

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "LiveKit Location Backend API",
        "version": "1.0.0",
        "endpoints": {
            "POST /location": "Submit new location data",
            "GET /location/{user_id}": "Get locations for a user",
            "GET /location/{user_id}/latest": "Get latest location for a user",
            "GET /locations": "Get all stored locations",
            "GET /stats": "Get storage statistics",
            "POST /livekit/connect": "Connect to LiveKit room"
        }
    }


@app.post("/location", status_code=201)
async def submit_location(request: LocationRequest):
    """
    Submit new location data
    
    This endpoint accepts location data and stores it in memory.
    """
    try:
        entry = storage.add_location(
            user_id=request.user_id,
            location=request.user_location,
            session_id=request.session_id
        )
        
        return {
            "status": "success",
            "message": "Location stored successfully",
            "data": {
                "user_id": entry.user_id,
                "latitude": entry.user_location.latitude,
                "longitude": entry.user_location.longitude,
                "timestamp": entry.timestamp.isoformat()
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error storing location: {str(e)}")


@app.get("/location/{user_id}")
async def get_user_locations(user_id: str):
    """Get all locations for a specific user"""
    locations = storage.get_user_locations(user_id)
    
    if not locations:
        raise HTTPException(status_code=404, detail=f"No locations found for user: {user_id}")
    
    return {
        "user_id": user_id,
        "total_locations": len(locations),
        "locations": [
            {
                "latitude": loc.user_location.latitude,
                "longitude": loc.user_location.longitude,
                "timestamp": loc.timestamp.isoformat(),
                "session_id": loc.session_id
            }
            for loc in locations
        ]
    }


@app.get("/location/{user_id}/latest")
async def get_latest_location(user_id: str):
    """Get the most recent location for a user"""
    location = storage.get_latest_location(user_id)
    
    if not location:
        raise HTTPException(status_code=404, detail=f"No locations found for user: {user_id}")
    
    return {
        "user_id": user_id,
        "latitude": location.user_location.latitude,
        "longitude": location.user_location.longitude,
        "timestamp": location.timestamp.isoformat(),
        "session_id": location.session_id
    }


@app.get("/locations")
async def get_all_locations():
    """Get all stored locations"""
    all_locations = storage.get_all_locations()
    
    return {
        "total_users": len(all_locations),
        "total_entries": storage.total_entries,
        "data": {
            user_id: [
                {
                    "latitude": loc.user_location.latitude,
                    "longitude": loc.user_location.longitude,
                    "timestamp": loc.timestamp.isoformat(),
                    "session_id": loc.session_id
                }
                for loc in locations
            ]
            for user_id, locations in all_locations.items()
        }
    }


@app.get("/stats")
async def get_stats():
    """Get storage statistics"""
    return storage.get_stats()


@app.post("/livekit/connect")
async def connect_livekit(room_name: str):
    """
    Connect to a LiveKit room to receive location data
    
    Requires environment variables:
    - LIVEKIT_URL
    - LIVEKIT_API_KEY
    - LIVEKIT_API_SECRET
    """
    global livekit_receiver
    
    # Get LiveKit credentials from environment
    livekit_url = os.getenv("LIVEKIT_URL")
    livekit_api_key = os.getenv("LIVEKIT_API_KEY")
    livekit_api_secret = os.getenv("LIVEKIT_API_SECRET")
    
    if not all([livekit_url, livekit_api_key, livekit_api_secret]):
        raise HTTPException(
            status_code=500,
            detail="LiveKit credentials not configured. Set LIVEKIT_URL, LIVEKIT_API_KEY, and LIVEKIT_API_SECRET"
        )
    
    try:
        livekit_receiver = LiveKitLocationReceiver(livekit_url, livekit_api_key, livekit_api_secret)
        await livekit_receiver.connect_to_room(room_name)
        
        return {
            "status": "success",
            "message": f"Connected to LiveKit room: {room_name}",
            "room_name": room_name
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to connect to LiveKit: {str(e)}")


@app.get("/livekit/status")
async def livekit_status():
    """Check LiveKit connection status"""
    if livekit_receiver and livekit_receiver.is_connected:
        return {
            "connected": True,
            "message": "Connected to LiveKit room"
        }
    else:
        return {
            "connected": False,
            "message": "Not connected to LiveKit room"
        }


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    global livekit_receiver
    if livekit_receiver:
        await livekit_receiver.disconnect()


# ============================================================================
# MAIN
# ============================================================================

if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 Starting LiveKit Location Backend Server")
    print("="*60)
    print("📍 Ready to receive location data from LiveKit participants")
    print("🌐 API Documentation: http://localhost:8000/docs")
    print("="*60 + "\n")
    
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )
