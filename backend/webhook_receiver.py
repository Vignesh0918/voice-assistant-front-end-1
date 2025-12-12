"""
Alternative Backend - Using LiveKit Webhooks
This receives location data via webhooks instead of direct room connection
More stable and reliable for production use
"""

from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
import json
import uvicorn
import os
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="LiveKit Location Webhook Receiver")

# Add CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Storage
location_data = []
location_count = 0


class UserLocation(BaseModel):
    latitude: float
    longitude: float


class LocationData(BaseModel):
    user_location: UserLocation
    user_id: Optional[str] = "unknown"


def print_location(user_id: str, latitude: float, longitude: float, session_id: str = None):
    """Print location to terminal with nice formatting"""
    global location_count
    location_count += 1
    
    print("\n" + "="*70)
    print(f"📍 LOCATION #{location_count} RECEIVED")
    print("="*70)
    print(f"👤 User ID:     {user_id}")
    print(f"🌍 Latitude:    {latitude}")
    print(f"🌍 Longitude:   {longitude}")
    print(f"🕐 Timestamp:   {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    if session_id:
        print(f"🔑 Session ID:  {session_id}")
    print("="*70 + "\n")


@app.get("/")
async def root():
    return {
        "message": "LiveKit Location Webhook Receiver",
        "status": "running",
        "total_locations": location_count,
        "endpoints": {
            "POST /location": "Receive location data",
            "POST /webhook": "LiveKit webhook endpoint",
            "GET /stats": "Get statistics"
        }
    }


@app.post("/location")
async def receive_location(data: LocationData):
    """Direct endpoint to receive location data"""
    try:
        # Store
        location_entry = {
            "user_id": data.user_id,
            "latitude": data.user_location.latitude,
            "longitude": data.user_location.longitude,
            "timestamp": datetime.now().isoformat()
        }
        location_data.append(location_entry)
        
        # Print to terminal
        print_location(
            user_id=data.user_id,
            latitude=data.user_location.latitude,
            longitude=data.user_location.longitude
        )
        
        return {"status": "success", "message": "Location received"}
    except Exception as e:
        print(f"❌ Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/webhook")
async def livekit_webhook(request: Request):
    """Receive webhooks from LiveKit"""
    try:
        body = await request.json()
        event = body.get("event")
        
        print(f"📨 Webhook received: {event}")
        print(f"📦 Data: {json.dumps(body, indent=2)}")
        
        # Handle data_received events
        if event == "data_received":
            data_str = body.get("data", {}).get("payload", "")
            if data_str:
                try:
                    data_json = json.loads(data_str)
                    if "user_location" in data_json:
                        location = data_json["user_location"]
                        user_id = body.get("participant", {}).get("identity", "unknown")
                        session_id = body.get("participant", {}).get("sid")
                        
                        print_location(
                            user_id=user_id,
                            latitude=location.get("latitude"),
                            longitude=location.get("longitude"),
                            session_id=session_id
                        )
                except json.JSONDecodeError:
                    print(f"⚠️  Could not parse data as JSON")
        
        return {"status": "ok"}
    except Exception as e:
        print(f"❌ Webhook error: {e}")
        return {"status": "error", "message": str(e)}


@app.get("/stats")
async def get_stats():
    """Get statistics"""
    return {
        "total_locations": location_count,
        "stored_locations": len(location_data),
        "recent_locations": location_data[-10:] if location_data else []
    }


@app.get("/locations")
async def get_all_locations():
    """Get all locations"""
    return {
        "total": len(location_data),
        "locations": location_data
    }


if __name__ == "__main__":
    print("\n" + "="*70)
    print("🚀 LiveKit Location Webhook Receiver")
    print("="*70)
    print("📍 Ready to receive location data")
    print("🌐 Server: http://localhost:8001")
    print("📚 API Docs: http://localhost:8001/docs")
    print("="*70 + "\n")
    print("💡 TIP: You can send location data directly to:")
    print("   POST http://localhost:8001/location")
    print("   Body: {\"user_location\": {\"latitude\": 12.9716, \"longitude\": 77.5946}}")
    print("="*70 + "\n")
    
    uvicorn.run(app, host="0.0.0.0", port=8001, log_level="info")
