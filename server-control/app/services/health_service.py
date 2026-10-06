import time
import threading
import requests
from datetime import datetime
from PySide6.QtCore import QThread, Signal

class BackendState:
    STOPPED = "STOPPED"
    STARTING = "STARTING"
    ONLINE = "ONLINE"
    OFFLINE = "OFFLINE"
    ERROR = "ERROR"
    FOREIGN_SERVICE = "FOREIGN_SERVICE"


class HealthPoller(QThread):
    """Polls backend health in a background QThread and emits signals."""

    status_changed = Signal(str, str, dict)
    # (state: BackendState, reason: str, data: dict)

    def __init__(self, parent=None, interval: int = 10):
        super().__init__(parent)
        self._running = False
        self._current_state = BackendState.STOPPED
        self._interval = interval

    def set_interval(self, secs: int):
        self._interval = max(2, secs)

    def set_state(self, state: str, reason: str = ""):
        self._current_state = state
        self.status_changed.emit(state, reason, {})

    def run(self):
        self._running = True
        while self._running:
            self._poll()
            for _ in range(self._interval * 2):
                if not self._running:
                    break
                time.sleep(0.5)

    def poll_now(self):
        t = threading.Thread(target=self._poll, daemon=True)
        t.start()

    def _poll(self):
        # Don't override foreign service state unless port freed
        if self._current_state == BackendState.FOREIGN_SERVICE:
            return

        urls = [
            "http://localhost:8080/api/health",
            "http://127.0.0.1:8080/api/health"
        ]
        last_err = None

        for url in urls:
            try:
                t0 = time.monotonic()
                resp = requests.get(url, timeout=3.5)
                latency_ms = int((time.monotonic() - t0) * 1000)

                if resp.status_code == 200:
                    try:
                        data = resp.json()
                        # Verify this is actually CipherVault
                        svc = data.get("service", "")
                        status = data.get("status", "")
                        if "CipherVault" in svc or status == "UP":
                            data["_latency_ms"] = latency_ms
                            data["_http_status"] = 200
                            data["_checked_at"] = datetime.now().strftime("%H:%M:%S")
                            self._current_state = BackendState.ONLINE
                            self.status_changed.emit(BackendState.ONLINE, "200 OK", data)
                            return
                        else:
                            # 200 OK but doesn't look like CipherVault
                            self._current_state = BackendState.FOREIGN_SERVICE
                            self.status_changed.emit(
                                BackendState.FOREIGN_SERVICE,
                                f"Service '{svc}' is not CipherVault",
                                {}
                            )
                            return
                    except ValueError:
                        self.status_changed.emit(
                            BackendState.ERROR, "HTTP 200 but invalid JSON response", {}
                        )
                        return
                else:
                    self._current_state = BackendState.ERROR
                    self.status_changed.emit(
                        BackendState.ERROR, f"HTTP {resp.status_code}", {}
                    )
                    return

            except requests.exceptions.ConnectionError:
                last_err = "Connection refused"
            except requests.exceptions.Timeout:
                last_err = "Request timed out"
            except Exception as e:
                last_err = str(e)

        # If we reach here, connection failed
        if self._current_state != BackendState.STARTING:
            self._current_state = BackendState.OFFLINE
            self.status_changed.emit(BackendState.OFFLINE, last_err or "Connection refused", {})

    def stop(self):
        self._running = False
        self.wait(3000)
