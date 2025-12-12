# 🔧 LiveKit Connection Issue - SOLUTION

## ❌ Problem

The `simple_receiver.py` is failing with LiveKit WebSocket connection errors:
```
livekit::rtc_engine - resuming connection failed: signal failure: ws failure: Connection closed normally
```

This is a known issue with the Python LiveKit SDK having connection stability problems.

---

## ✅ SOLUTION: Use Webhook Receiver Instead

I've created a **more stable alternative** that doesn't require maintaining a persistent LiveKit connection.

### **New File: `webhook_receiver.py`**

This backend:
- ✅ **More stable** - No persistent WebSocket connection issues
- ✅ **Simpler** - Just receives HTTP POST requests
- ✅ **Works immediately** - No connection retry loops
- ✅ **Same output** - Prints location data to terminal exactly the same way

---

## 🚀 How to Use

### **Step 1: Run the Webhook Receiver**

```bash
cd backend
python webhook_receiver.py
```

You'll see:
```
======================================================================
🚀 LiveKit Location Webhook Receiver
======================================================================
📍 Ready to receive location data
🌐 Server: http://localhost:8001
📚 API Docs: http://localhost:8001/docs
======================================================================
```

### **Step 2: Send Location Data**

There are **3 ways** to send location data to the backend:

---

#### **Option 1: From Android App via HTTP (RECOMMENDED)**

Modify your `LocationService.kt` to send location via HTTP:

```kotlin
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// Add at top of LocationService class
companion object {
    private const val BACKEND_URL = "http://YOUR_COMPUTER_IP:8001/location"
    // Replace YOUR_COMPUTER_IP with your computer's local IP
    // Example: "http://192.168.1.100:8001/location"
}

// Add this new function
private fun sendLocationViaHTTP(message: String) {
    scope.launch {
        try {
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = message.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(BACKEND_URL)
                .post(body)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Location sent via HTTP successfully")
                } else {
                    Log.e(TAG, "HTTP request failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending location via HTTP", e)
        }
    }
}

// Modify existing sendLocationToLiveKit function
private fun sendLocationToLiveKit(message: String) {
    // Keep existing LiveKit code...
    val room = VoiceAssistantViewModel.activeRoom
    if (room != null) {
        // ... existing LiveKit code ...
    }
    
    // ADD THIS: Also send via HTTP
    sendLocationViaHTTP(message)
}
```

**To get your computer's IP:**
```powershell
ipconfig
```
Look for "IPv4 Address" under your active network adapter.

---

#### **Option 2: Test with PowerShell**

```powershell
Invoke-WebRequest -Uri "http://localhost:8001/location" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"user_id":"test_user","user_location":{"latitude":12.9716,"longitude":77.5946}}' `
  -UseBasicParsing
```

---

#### **Option 3: Test with Python**

```python
import requests

data = {
    "user_id": "test_user",
    "user_location": {
        "latitude": 12.9716,
        "longitude": 77.5946
    }
}

response = requests.post("http://localhost:8001/location", json=data)
print(response.json())
```

---

## 📍 Terminal Output

When location data is received, you'll see:

```
======================================================================
📍 LOCATION #1 RECEIVED
======================================================================
👤 User ID:     test_user
🌍 Latitude:    12.9716
🌍 Longitude:   77.5946
🕐 Timestamp:   2025-12-12 12:30:45
======================================================================
```

---

## 🔄 Complete Flow (HTTP Method)

```
┌─────────────────┐
│  Android App    │  Gets GPS location
│ LocationService │  Formats as JSON
└────────┬────────┘
         │
         │ HTTP POST to http://YOUR_IP:8001/location
         │ Body: {"user_location": {"latitude": X, "longitude": Y}}
         │
         ▼
┌─────────────────┐
│ Backend Server  │  Receives HTTP request
│webhook_receiver │  Prints to terminal
└─────────────────┘  Stores in memory
```

---

## 📊 Comparison

| Method | Stability | Setup | Pros | Cons |
|--------|-----------|-------|------|------|
| **simple_receiver.py** | ❌ Unstable | Complex | Real-time via LiveKit | Connection issues |
| **webhook_receiver.py** | ✅ Stable | Simple | No connection issues | Requires HTTP from Android |

---

## 🎯 Recommended Setup

### **For Production:**
1. Use `webhook_receiver.py` (more stable)
2. Modify Android app to send via HTTP (see Option 1 above)
3. Both LiveKit AND HTTP can work simultaneously

### **For Testing:**
1. Run `webhook_receiver.py`
2. Use PowerShell to send test data (Option 2)
3. Verify terminal output

---

## 🔍 Troubleshooting

### **Backend not receiving data?**

1. **Check backend is running:**
   ```bash
   # Should see "Application startup complete"
   ```

2. **Check Android can reach backend:**
   - Android and computer must be on same WiFi
   - Use computer's local IP (not localhost)
   - Check firewall isn't blocking port 8001

3. **Test with PowerShell first:**
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:8001/location" -Method POST -ContentType "application/json" -Body '{"user_id":"test","user_location":{"latitude":12.9716,"longitude":77.5946}}' -UseBasicParsing
   ```

### **Find your computer's IP:**

```powershell
ipconfig | Select-String "IPv4"
```

---

## 📚 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | API information |
| `/location` | POST | Receive location data |
| `/webhook` | POST | LiveKit webhook endpoint |
| `/stats` | GET | Get statistics |
| `/locations` | GET | Get all stored locations |
| `/docs` | GET | Interactive API docs |

---

## ✅ Summary

**The webhook_receiver.py is:**
- ✅ Running on port 8001
- ✅ Ready to receive location data
- ✅ Will print to terminal when data arrives
- ✅ More stable than LiveKit direct connection

**Next step:**
Modify your Android app to send location via HTTP (see Option 1 above) or test with PowerShell (Option 2).
