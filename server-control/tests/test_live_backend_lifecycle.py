"""Live Backend Lifecycle Integration Test.

Tests Start -> Port Listening -> Health Check -> RUNNING -> Stop -> STOPPED
using the real Maven wrapper, Spring Boot application, and Server Control GUI window.
"""

import os
import sys
import time

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
SERVER_CONTROL_DIR = os.path.dirname(CURRENT_DIR)
if SERVER_CONTROL_DIR not in sys.path:
    sys.path.insert(0, SERVER_CONTROL_DIR)

from PySide6.QtWidgets import QApplication
from app.services.backend_state import BackendState
from app.window import MainWindow

app = QApplication.instance()
if app is None:
    app = QApplication(sys.argv)

def run_lifecycle_test():
    print("=== LIVE BACKEND LIFECYCLE TEST ===")
    win = MainWindow()
    win.show()

    # Step 1: Initial state
    print(f"1. Initial State: {win._backend_state}")
    assert win._backend_state == BackendState.STOPPED, f"Expected STOPPED, got {win._backend_state}"

    # Step 2: Start backend
    print("2. Starting backend via win._start_server()...")
    win._start_server()
    app.processEvents()

    # Step 3: Wait for backend to reach RUNNING state (timeout 45s)
    print("3. Waiting for Spring Boot to start and reach RUNNING state...")
    max_wait = 45
    start_time = time.time()
    became_running = False

    while time.time() - start_time < max_wait:
        app.processEvents()
        time.sleep(0.5)
        if win._backend_state in (BackendState.RUNNING, BackendState.ONLINE):
            became_running = True
            break

    elapsed = time.time() - start_time
    if became_running:
        print(f"SUCCESS: Backend became RUNNING in {elapsed:.1f}s!")
        print(f"   Status Pill: {win._status_pill.text()}")
        print(f"   Start Btn Enabled: {win._btn_start.isEnabled()}")
        print(f"   Stop Btn Enabled: {win._btn_stop.isEnabled()}")
        print(f"   Restart Btn Enabled: {win._btn_restart.isEnabled()}")
    else:
        print(f"FAILED: Backend did not reach RUNNING within {max_wait}s. Current state: {win._backend_state}")

    # Step 4: Stop backend
    print("4. Stopping backend via win._stop_server()...")
    win._stop_server()
    app.processEvents()

    # Step 5: Wait for backend to reach STOPPED state (timeout 15s)
    print("5. Waiting for backend process to terminate...")
    stop_wait = 15
    stop_start = time.time()
    became_stopped = False

    while time.time() - stop_start < stop_wait:
        app.processEvents()
        time.sleep(0.5)
        if win._backend_state in (BackendState.STOPPED, BackendState.OFFLINE):
            became_stopped = True
            break

    stop_elapsed = time.time() - stop_start
    if became_stopped:
        print(f"SUCCESS: Backend returned to STOPPED in {stop_elapsed:.1f}s!")
        print(f"   Status Pill: {win._status_pill.text()}")
        print(f"   Start Btn Enabled: {win._btn_start.isEnabled()}")
        print(f"   Stop Btn Enabled: {win._btn_stop.isEnabled()}")
    else:
        print(f"FAILED: Backend did not return to STOPPED within {stop_wait}s. Current state: {win._backend_state}")

    win.close()
    app.quit()

    if became_running and became_stopped:
        print("=== LIFECYCLE TEST COMPLETED SUCCESSFULLY! ===")
        return 0
    else:
        print("=== LIFECYCLE TEST ENCOUNTERED DEFECTS! ===")
        return 1

if __name__ == "__main__":
    sys.exit(run_lifecycle_test())
