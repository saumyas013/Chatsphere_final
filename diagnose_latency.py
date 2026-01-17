
import time
import requests
import base64
import json

API_URL = "http://127.0.0.1:5000/predict"

def test_latency(description, payload):
    print(f"\n--- Testing: {description} ---")
    start_time = time.time()
    try:
        response = requests.post(API_URL, json=payload, timeout=300) # 5 min timeout
        end_time = time.time()
        
        latency = end_time - start_time
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            reply = data.get('response', '')[:50] + "..." # Truncate for display
            print(f"Response: {reply}")
            print(f"Latency: {latency:.2f} seconds")
            return latency
        else:
            print(f"Error: {response.text}")
            return None
    except Exception as e:
        print(f"Connection Error: {e}")
        return None

if __name__ == "__main__":
    print("Starting Diagnostic Benchmark...")
    
    # 1. Warm-up / Simple Text
    text_payload = {
        "message": "Hello, are you ready?",
        "history": []
    }
    test_latency("Simple Text Query (Warm-up)", text_payload)
    
    # 2. RAG Query (Simulated)
    # We ask a question likely to trigger context retrieval if docs exist
    rag_payload = {
        "message": "What is Spring Boot?",
        "history": []
    }
    test_latency("RAG Context Query", rag_payload)

    print("\nDiagnostic Complete.")
