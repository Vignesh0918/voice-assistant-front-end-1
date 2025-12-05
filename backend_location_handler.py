import asyncio
import json
from livekit import rtc

# Configuration
LIVEKIT_URL = "wss://marvel-ydp1lqmi.livekit.cloud"
# Token for identity: backend_user, room: test_room
LIVEKIT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjQ5OTYzNzcsImlkZW50aXR5IjoiYmFja2VuZF91c2VyIiwiaXNzIjoiQVBJUzYzQURWNlFIa0FCIiwibmFtZSI6InRlc3RfdXNlciIsIm5iZiI6MTc2NDkwOTk3Nywic3ViIjoiYmFja2VuZF91c2VyIiwidmlkZW8iOnsicm9vbSI6InRlc3Rfcm9vbSIsInJvb21Kb2luIjp0cnVlfX0._EGvXqE7dLTZS6pI1eDRbEJkX9XmHTZrso-Y8yIBebg"

async def main():
    room = rtc.Room()

    @room.on("participant_connected")
    def on_participant_connected(participant: rtc.RemoteParticipant):
        print(f"👋 Participant connected: {participant.identity}")

    @room.on("participant_disconnected")
    def on_participant_disconnected(participant: rtc.RemoteParticipant):
        print(f"❌ Participant disconnected: {participant.identity}")

    @room.on("data_received")
    def on_data_received(data: rtc.DataPacket):
        print(f"📦 Data received on topic '{data.topic}' from {data.participant.identity if data.participant else 'Server'}")
        try:
            payload = data.data.decode("utf-8")
            print(f"   Raw payload: {payload}")
            json_data = json.loads(payload)
            if "latitude" in json_data and "longitude" in json_data:
                print(f"   📍 COORDINATES: Lat={json_data['latitude']}, Lon={json_data['longitude']}")
        except Exception as e:
            print(f"   ⚠️ Failed to decode data: {e}")

    print(f"Connecting to {LIVEKIT_URL}...")
    try:
        await room.connect(LIVEKIT_URL, LIVEKIT_TOKEN)
        print(f"✅ Connected to room: {room.name}")
        print(f"My Identity: {room.local_participant.identity}")
        
        print("👥 Current participants:")
        for p in room.remote_participants.values():
            print(f"   - {p.identity}")
        
        if not room.remote_participants:
            print("   (No other users found yet. Is the Android app running?)")

        print("\nWaiting for location updates...")
        
        # Keep running
        while True:
            await asyncio.sleep(1)
            
    except Exception as e:
        print(f"❌ Failed to connect: {e}")

if __name__ == "__main__":
    asyncio.run(main())
