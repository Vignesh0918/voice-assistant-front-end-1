import asyncio
import json
from livekit import rtc

async def main():
    # Connect to the room
    room = rtc.Room()
    
    @room.on("data_received")
    def on_data_received(data: rtc.DataPacket):
        # Depending on the SDK version, the event might return a DataPacket object
        # or individual arguments. The error suggests a mismatch.
        # Let's try the most compatible signature based on recent SDKs.
        
        try:
            # Inspect what we received
            payload = data.data
            participant = data.participant
            
            # Decode the data
            decoded_data = payload.decode("utf-8")
            json_data = json.loads(decoded_data)
            
            # Check if it is location data
            if "latitude" in json_data and "longitude" in json_data:
                lat = json_data["latitude"]
                lon = json_data["longitude"]
                identity = participant.identity if participant else "Unknown"
                print(f"Received location from {identity}: Lat={lat}, Lon={lon}")
                
        except Exception as e:
            print(f"Error processing data: {e}")

    # Connect
    # NOTE: Ensure this token is for identity 'test_user' and room 'test_room'
    # and is DIFFERENT from the Android app's token.
    await room.connect("wss://marvel-ydp1lqmi.livekit.cloud", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjQ5NDYyMzIsImlkZW50aXR5IjoidGVzdF91c2VyIiwiaXNzIjoiQVBJUzYzQURWNlFIa0FCIiwibmFtZSI6InRlc3RfdXNlciIsIm5iZiI6MTc2NDg1OTgzMiwic3ViIjoidGVzdF91c2VyIiwidmlkZW8iOnsicm9vbSI6InRlc3Rfcm9vbSIsInJvb21Kb2luIjp0cnVlfX0.ecelyN7C8WfiJHFOPyBZfEdYONtVLymr0oc5TzNN1UQ")
    
    print("Connected to room. Waiting for location updates...")
    
    # Keep the connection alive
    while True:
        await asyncio.sleep(1)

if __name__ == "__main__":
    asyncio.run(main())
