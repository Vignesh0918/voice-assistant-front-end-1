import asyncio
import json
from livekit import rtc

async def main():
    # Connect to the room
    room = rtc.Room()
    
    @room.on("data_received")
    def on_data_received(data: bytes, participant: rtc.RemoteParticipant, kind: rtc.DataPacketKind):
        try:
            # Decode the data
            decoded_data = data.decode("utf-8")
            json_data = json.loads(decoded_data)
            
            # Check if it is location data
            if "latitude" in json_data and "longitude" in json_data:
                lat = json_data["latitude"]
                lon = json_data["longitude"]
                print(f"Received location from {participant.identity}: Lat={lat}, Lon={lon}")
                
                # Process location data here (e.g., store in DB, trigger logic)
                
        except Exception as e:
            print(f"Error processing data: {e}")

    # Replace with your URL and Token
    await room.connect("wss://marvel-ydp1lqmi.livekit.cloud", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjQ5NDYyMzIsImlkZW50aXR5IjoidGVzdF91c2VyIiwiaXNzIjoiQVBJUzYzQURWNlFIa0FCIiwibmFtZSI6InRlc3RfdXNlciIsIm5iZiI6MTc2NDg1OTgzMiwic3ViIjoidGVzdF91c2VyIiwidmlkZW8iOnsicm9vbSI6InRlc3Rfcm9vbSIsInJvb21Kb2luIjp0cnVlfX0.ecelyN7C8WfiJHFOPyBZfEdYONtVLymr0oc5TzNN1UQ")
    print("Connected to room. Waiting for location updates...")
    
    # Keep the connection alive
    while True:
        await asyncio.sleep(1)

if __name__ == "__main__":
    asyncio.run(main())
