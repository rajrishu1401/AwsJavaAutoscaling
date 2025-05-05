import requests
import threading
import time

url = "http://crimealert-lb-ec2-with-asg-1225296361.ap-south-1.elb.amazonaws.com/health"

# Configuration
requests_per_thread = 50
thread_count = 50
repeat_load_cycles = 50  # Adjust this to send multiple waves
delay_between_cycles = 10  # seconds

def send_requests(thread_id):
    for i in range(requests_per_thread):
        try:
            response = requests.get(url)
            print(f"[Thread-{thread_id}] Request {i+1}: {response.status_code} ({response.elapsed.total_seconds():.3f}s)")
        except Exception as e:
            print(f"[Thread-{thread_id}] Request {i+1} failed: {e}")

for cycle in range(repeat_load_cycles):
    print(f"\n--- Load Cycle {cycle + 1} ---")
    threads = []

    for i in range(thread_count):
        t = threading.Thread(target=send_requests, args=(i,))
        threads.append(t)
        t.start()

    for t in threads:
        t.join()

    if cycle < repeat_load_cycles - 1:
        print(f"Waiting {delay_between_cycles} seconds before next cycle...\n")
        time.sleep(delay_between_cycles)

print("\n✅ Load test completed.")
