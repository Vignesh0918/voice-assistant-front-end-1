
/**
 * Interface definition for the user location structure.
 */
interface UserLocation {
    user_location: {
        latitude: number;
        longitude: number;
    };
}

/**
 * Retrieves the user's current geolocation.
 * Returns a Promise that resolves to the formatted JSON string.
 */
export async function getUserLocation(): Promise<string> {
    return new Promise((resolve, reject) => {
        // Check if Geolocation is supported
        if (!navigator.geolocation) {
            reject(new Error("Geolocation is not supported by this browser."));
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                // Construct the location object structure
                const locationData: UserLocation = {
                    user_location: {
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                    },
                };

                // Convert to JSON string
                const jsonString = JSON.stringify(locationData);
                resolve(jsonString);
            },
            (error) => {
                // Handle explicit errors
                let errorMessage = "Unknown error occurred fetching location.";
                switch (error.code) {
                    case error.PERMISSION_DENIED:
                        errorMessage = "User denied the request for Geolocation.";
                        break;
                    case error.POSITION_UNAVAILABLE:
                        errorMessage = "Location information is unavailable.";
                        break;
                    case error.TIMEOUT:
                        errorMessage = "The request to get user location timed out.";
                        break;
                }
                reject(new Error(errorMessage));
            },
            {
                enableHighAccuracy: true, // Request best possible results
                timeout: 10000,           // Wait max 10s
                maximumAge: 0             // Do not use cached position
            }
        );
    });
}

// --- usage example with livekit-client ---

// import { Room, RoomEvent } from 'livekit-client';

/**
 * Demonstrates how to connect to a LiveKit room and pass the location metadata.
 * Note: In LiveKit, initial metadata is typically part of the Access Token.
 * From the client side, we can update the metadata immediately after connection
 * using `room.localParticipant.setMetadata()`.
 */
export async function joinRoomWithLocation(url: string, token: string) {
    try {
        console.log("Requesting user location...");
        const locationMetadata = await getUserLocation();
        console.log("Location obtained:", locationMetadata);

        // Assuming you have the 'livekit-client' package installed
        // const room = new Room();

        // await room.connect(url, token);
        // console.log("Connected to room:", room.name);

        // Publish the location as metadata for the local participant
        // await room.localParticipant.setMetadata(locationMetadata);
        // console.log("Metadata updated successfully");

    } catch (error) {
        console.error("Error joining room with location:", error);
    }
}
