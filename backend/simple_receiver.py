"""
Simple LiveKit Location Receiver
Connects to LiveKit and prints user locations to terminal
"""

import asyncio
import json
import os
from datetime import datetime
from dotenv import load_dotenv
from livekit import rtc, api

# Load environment variables
load_dotenv()

class LocationReceiver:
    def __init__(self):
        self.room = None
        self.location_count = 0
        
    def print_location(self, user_id: str, latitude: float, longitude: float, session_id: str = None):
        """Print location to terminal with nice formatting"""
        self.location_count += 1
        
        print("\n" + "="*70)
        print(f"📍 LOCATION #{self.location_count} RECEIVED")
        print("="*70)
        print(f"👤 User ID:     {user_id}")
        print(f"🌍 Latitude:    {latitude}")
        print(f"🌍 Longitude:   {longitude}")
        print(f"🕐 Timestamp:   {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        if session_id:
            print(f"🔑 Session ID:  {session_id}")
        print("="*70 + "\n")
    
    async def connect_and_listen(self, room_name: str):
        """Connect to LiveKit room and listen for location data"""
        
        # Get credentials from environment
        livekit_url = os.getenv("LIVEKIT_URL")
        livekit_api_key = os.getenv("LIVEKIT_API_KEY")
        livekit_api_secret = os.getenv("LIVEKIT_API_SECRET")
        
        if not all([livekit_url, livekit_api_key, livekit_api_secret]):
            print("❌ ERROR: Missing LiveKit credentials!")
            print("Please set LIVEKIT_URL, LIVEKIT_API_KEY, and LIVEKIT_API_SECRET in .env file")
            return
        
        try:
            # Generate access token
            print("🔐 Generating access token...")
            token = api.AccessToken(livekit_api_key, livekit_api_secret) \
                .with_identity("location-backend") \
                .with_name("Location Backend") \
                .with_grants(api.VideoGrants(
                    room_join=True,
                    room=room_name,
                )).to_jwt()
            
            # Create room
            print("🏗️  Creating room instance...")
            self.room = rtc.Room()
            
            # Set up event handlers
            @self.room.on("data_received")
            def on_data_received(data_packet: rtc.DataPacket):
                try:
                    # Decode the data
                    data_str = data_packet.data.decode('utf-8')
                    print(f"📦 Raw data received: {data_str}")
                    
                    # Parse JSON
                    data_json = json.loads(data_str)
                    
                    # Check if it's location data
                    if "user_location" in data_json:
                        location = data_json["user_location"]
                        user_id = data_packet.participant.identity if data_packet.participant else "unknown"
                        session_id = data_packet.participant.sid if data_packet.participant else None
                        
                        # Print to terminal
                        self.print_location(
                            user_id=user_id,
                            latitude=location.get("latitude"),
                            longitude=location.get("longitude"),
                            session_id=session_id
                        )
                    else:
                        print(f"⚠️  Received data without user_location: {data_json}")
                        
                except json.JSONDecodeError as e:
                    print(f"⚠️  Received non-JSON data: {data_packet.data}")
                except Exception as e:
                    print(f"❌ Error processing data: {e}")
            
            @self.room.on("participant_connected")
            def on_participant_connected(participant: rtc.RemoteParticipant):
                print(f"✅ Participant joined: {participant.identity} (SID: {participant.sid})")
            
            @self.room.on("participant_disconnected")
            def on_participant_disconnected(participant: rtc.RemoteParticipant):
                print(f"👋 Participant left: {participant.identity}")
            
            @self.room.on("connected")
            def on_connected():
                print(f"🎉 Successfully connected to room!")
                print(f"📡 Listening for location data on topic: 'location_update'")
                print(f"⏳ Waiting for location data from participants...\n")
            
            @self.room.on("disconnected")
            def on_disconnected():
                print("❌ Disconnected from room")
            
            # Connect to room
            print(f"🔌 Connecting to LiveKit room: {room_name}")
            print(f"🌐 Server: {livekit_url}")
            await self.room.connect(livekit_url, token)
            
            # Keep the connection alive
            print("✨ Backend is now listening for location updates...")
            print("💡 Press Ctrl+C to stop\n")
            
            # Wait indefinitely
            while True:
                await asyncio.sleep(1)
                
        except KeyboardInterrupt:
            print("\n\n👋 Shutting down...")
        except Exception as e:
            print(f"❌ Error: {e}")
            import traceback
            traceback.print_exc()
        finally:
            if self.room:
                await self.room.disconnect()
                print("✅ Disconnected from LiveKit")


async def main():
    """Main entry point"""
    print("\n" + "="*70)
    print("🚀 LiveKit Location Receiver")
    print("="*70)
    print("This backend connects to LiveKit and displays user locations")
    print("="*70 + "\n")
    
    # Get room name from environment or use default
    room_name = os.getenv("LIVEKIT_ROOM_NAME", "voice-assistant-room")
    
    print(f"📋 Configuration:")
    print(f"   Room Name: {room_name}")
    print(f"   URL: {os.getenv('LIVEKIT_URL', 'Not set')}")
    print()
    
    # Create receiver and connect
    receiver = LocationReceiver()
    await receiver.connect_and_listen(room_name)


if __name__ == "__main__":
    asyncio.run(main())
