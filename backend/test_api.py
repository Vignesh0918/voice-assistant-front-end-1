"""
Test script for the LiveKit Location Backend
Demonstrates how to submit location data to the backend
"""

import requests
import json
from datetime import datetime

# Backend URL
BASE_URL = "http://localhost:8000"

def test_submit_location():
    """Test submitting a location"""
    print("\n" + "="*60)
    print("Testing Location Submission")
    print("="*60)
    
    location_data = {
        "user_id": "test_user_001",
        "user_location": {
            "latitude": 12.9716,
            "longitude": 77.5946
        },
        "session_id": "test_session_123"
    }
    
    response = requests.post(f"{BASE_URL}/location", json=location_data)
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    return response.status_code == 201


def test_get_user_locations():
    """Test retrieving user locations"""
    print("\n" + "="*60)
    print("Testing Get User Locations")
    print("="*60)
    
    user_id = "test_user_001"
    response = requests.get(f"{BASE_URL}/location/{user_id}")
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    return response.status_code == 200


def test_get_latest_location():
    """Test retrieving latest location"""
    print("\n" + "="*60)
    print("Testing Get Latest Location")
    print("="*60)
    
    user_id = "test_user_001"
    response = requests.get(f"{BASE_URL}/location/{user_id}/latest")
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    return response.status_code == 200


def test_get_stats():
    """Test retrieving statistics"""
    print("\n" + "="*60)
    print("Testing Get Statistics")
    print("="*60)
    
    response = requests.get(f"{BASE_URL}/stats")
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    return response.status_code == 200


def test_invalid_coordinates():
    """Test submitting invalid coordinates"""
    print("\n" + "="*60)
    print("Testing Invalid Coordinates (Should Fail)")
    print("="*60)
    
    invalid_data = {
        "user_id": "test_user_002",
        "user_location": {
            "latitude": 999,  # Invalid latitude
            "longitude": 77.5946
        }
    }
    
    response = requests.post(f"{BASE_URL}/location", json=invalid_data)
    
    print(f"Status Code: {response.status_code}")
    print(f"Response: {json.dumps(response.json(), indent=2)}")
    
    return response.status_code == 422  # Validation error


def run_all_tests():
    """Run all tests"""
    print("\n" + "🧪 "*30)
    print("STARTING BACKEND API TESTS")
    print("🧪 "*30)
    
    tests = [
        ("Submit Location", test_submit_location),
        ("Get User Locations", test_get_user_locations),
        ("Get Latest Location", test_get_latest_location),
        ("Get Statistics", test_get_stats),
        ("Invalid Coordinates", test_invalid_coordinates)
    ]
    
    results = []
    
    for test_name, test_func in tests:
        try:
            result = test_func()
            results.append((test_name, "✅ PASSED" if result else "❌ FAILED"))
        except Exception as e:
            results.append((test_name, f"❌ ERROR: {str(e)}"))
    
    # Print summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    for test_name, result in results:
        print(f"{test_name:.<40} {result}")
    print("="*60 + "\n")


if __name__ == "__main__":
    print("Make sure the backend server is running on http://localhost:8000")
    print("Start it with: python app.py")
    input("Press Enter to continue with tests...")
    
    run_all_tests()
