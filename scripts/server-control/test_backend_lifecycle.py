"""Test Backend Lifecycle: Start Spring Boot -> Health Check -> Stop Spring Boot"""

import os
import sys
import time
import requests

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ['QT_QPA_PLATFORM'] = 'offscreen'

from PySide6.QtWidgets import QApplication
from app.services.backend_service import BackendService
from app.services.network_service import NetworkService

app = QApplication(sys.argv)
service = BackendService()
service.log_line.connect(lambda l: print(f"    [mvn] {l}"))

print("1. Starting backend...")
service.start_backend()

# Wait for backend to come up (up to 60 seconds)
started = False
for i in range(60):
    time.sleep(1)
    app.processEvents()
    success, code, latency, msg = NetworkService.test_endpoint("http://127.0.0.1:8080/api/health", timeout=1.0)
    if success:
        print(f"Backend is UP in {i+1}s! Health: {msg} (Latency: {latency:.1f}ms)")
        started = True
        break
    else:
        print(f"Waiting for backend... ({i+1}s)")

assert started, "Backend failed to start within 60 seconds"

# Test Wi-Fi IP health check as well
ip, port, desc, _ = NetworkService.get_preferred_wifi_connection(port=8080)
wifi_url = f"http://{ip}:{port}/api/health"
print(f"2. Testing Wi-Fi IP endpoint: {wifi_url}")
success, code, latency, msg = NetworkService.test_endpoint(wifi_url, timeout=2.0)
print(f"Wi-Fi Health Check Result: success={success}, code={code}, latency={latency:.1f}ms, msg={msg}")
assert success, f"Failed to reach backend via Wi-Fi IP {wifi_url}"

print("3. Stopping backend...")
service.stop_backend()
time.sleep(3)

# Verify port is released
binding, _ = NetworkService.check_backend_binding(port=8080)
print(f"Port 8080 binding after stop: {binding}")
assert binding == "none", f"Port 8080 should be free, but was {binding}"

print("=== BACKEND LIFECYCLE TEST PASSED ===")
