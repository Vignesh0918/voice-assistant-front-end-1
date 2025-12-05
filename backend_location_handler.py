import asyncio
import json
from livekit import rtc

# Replace with your actual LiveKit URL
LIVEKIT_URL = "wss://marvel-ydp1lqmi.livekit.cloud"

# Paste the BACKEND USER Token here (Identity: backend_user, Room: test_room)
LIVEKIT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjQ5OTYzNzcsImlkZW50aXR5IjoiYmFja2VuZF91c2VyIiwiaXNzIjoiQVBJUzYzQURWNlFIa0FCIiwibmFtZSI6ImJhY2tlbmRfdXNlciIsIm5iZiI6MTc2NDkwOTk3Nywic3ViIjoiYmFja2VuZF91c2VyIiwidmlkZW8iOnsicm9vbSI6InRlc3Rfcm9vbSIsInJvb21Kb2luIjp0cnVlfX0._EGvXqE7dLTZS6pI1eDRbEJkX9XmHTZrso-Y8yIBebg"

async def main():
    print("Connecting to LiveKit room...")
    room = rtc.Room()

    @room.on("data_received")
    def on_data_received(data: rtc.DataPacket):
        try:
            # Decode the payload
            payload = data.data.decode("utf-8")
            json_data = json.loads(payload)

            if "latitude" in json_data and "longitude" in json_data:
                lat = json_data["latitude"]
                lon = json_data["longitude"]
                identity = data.participant.identity if data.participant else "Unknown"

                print(f"📍 Location received from {identity}: Latitude={lat}, Longitude={lon}")
        except Exception as e:
            print(f"Error processing data: {e}")

    try:
        await room.connect(LIVEKIT_URL, LIVEKIT_TOKEN)
        print("✅ Connected to room. Waiting for location updates from Android...")

        # Keep the script running to listen for events
        while True:
            await asyncio.sleep(1)

    except Exception as e:
        print(f"Failed to connect: {e}")

if __name__ == "__main__":
    asyncio.run(main())
