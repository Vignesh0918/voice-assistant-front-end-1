package io.livekit.android.example.voiceassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.Date;

// NOTE: This implementation uses the basic Android LocationManager.
// For production apps, the FusedLocationProviderClient (Google Play Services) is often preferred for battery efficiency.
public class LocationService extends Service implements LocationListener {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final long MIN_TIME_MS = 5000; // Update every 5 seconds
    private static final float MIN_DISTANCE_M = 10; // Update if moved 10 meters

    private LocationManager locationManager = null;

    /**
     * Required method for a Service. Not used in this basic implementation.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when the service is first created. Initializes the notification channel.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * Called when the service is started (via startService()). This is where we
     * promote the service to a foreground service and start location updates.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1. Create and start the foreground notification
        startForeground(NOTIFICATION_ID, buildNotification());

        // 2. Start listening for location updates
        startLocationUpdates();

        // Indicates that the service should continue running until it is explicitly stopped.
        return START_STICKY;
    }

    /**
     * Stops the location updates and removes the foreground notification.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.d(TAG, "LocationService stopped.");
    }

    // --- Location Logic Implementation ---

    /**
     * Requests location updates using the LocationManager.
     * Note: Requires the calling Activity to have already checked and obtained
     * ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION permissions.
     */
    private void startLocationUpdates() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        if (locationManager != null) {
            try {
                // Request updates from the best available provider (GPS or Network)
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
                // Also request from the network provider as a fallback/complement
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        this
                );
                Log.d(TAG, "Location updates requested.");
                Toast.makeText(this, "Location Tracking Started", Toast.LENGTH_SHORT).show();

            } catch (SecurityException e) {
                // This exception occurs if the required permissions (ACCESS_FINE_LOCATION)
                // have not been granted by the user.
                Log.e(TAG, "SecurityException: Location permissions not granted.", e);
                Toast.makeText(this, "Location permission denied. Cannot start tracking.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Removes the registered LocationListener from the LocationManager.
     */
    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            Log.d(TAG, "Location updates stopped.");
        }
        Toast.makeText(this, "Location Tracking Stopped", Toast.LENGTH_SHORT).show();
    }

    // --- LocationListener Callbacks ---

    @Override
    public void onLocationChanged(Location location) {
        // This method is called whenever the device detects a new location
        String timestamp = new Date().toString();
        String latitude = String.format("%.4f", location.getLatitude());
        String longitude = String.format("%.4f", location.getLongitude());

        String logMessage = "New Location: [" + timestamp + "] Lat: " + latitude + ", Lon: " + longitude + ", Provider: " + location.getProvider();
        Log.i(TAG, logMessage);

        // In a real application, you would send this location data to a server or update local storage.
        // For demonstration, we just update the notification content.
        updateNotification("Tracking Location", "Last update: Lat " + latitude + ", Lon " + longitude);
    }

    // Other required LocationListener methods (often unused in simple tracking)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, provider + " enabled.");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, provider + " disabled.");
    }

    // --- Notification Helper Methods ---

    /**
     * Creates a notification channel required for Android O (API 26) and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    /**
     * Builds the initial Notification required for the Foreground Service.
     */
    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Note: Replace android.R.drawable.ic_media_play with a proper app icon (e.g., R.drawable.ic_launcher)
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking Active")
                .setContentText("Your location is being monitored in the background.")
                .setSmallIcon(android.R.drawable.ic_media_play) // Use your app icon here
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Use a low priority to be less intrusive
                .build();
    }

    /**
     * Updates the existing notification with new content (e.g., the latest location).
     */
    private void updateNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play) // Use your app icon here
                .setContentIntent(buildPendingIntent())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Keeps the notification visible
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    /**
     * Helper to create the PendingIntent for the notification.
     */
    private PendingIntent buildPendingIntent() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
    }
}