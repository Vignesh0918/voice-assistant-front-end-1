import asyncio
import logging
from livekit.agents import JobContext, WorkerOptions, cli
from livekit import rtc
from dotenv import load_dotenv
import os

# Load environment variables from .env
load_dotenv()

async def entrypoint(ctx: JobContext):
    # Connect to the room
    await ctx.connect()
    print(f"Agent connected to room: {ctx.room.name}")

    # Register a listener for incoming data packets
    @ctx.room.on("data_received")
    def on_data_received(dp: rtc.DataPacket):
        # dp.data is bytes, decode it to string
        try:
            payload = dp.data.decode('utf-8')
            topic = dp.topic
            
            if topic == "location_update":
                print(f"📍 Location Received: {payload}")
            else:
                print(f"Received Data ({topic}): {payload}")
        except Exception as e:
            print(f"Error decoding data: {e}")

    # Keep the agent alive to listen for events
    try:
        await asyncio.Future()
    except asyncio.CancelledError:
        print("Agent shutting down...")

if __name__ == "__main__":
    # This starts the worker which listens for jobs from LiveKit
    cli.run_app(WorkerOptions(entrypoint_fnc=entrypoint))
