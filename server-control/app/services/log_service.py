import os
import threading
from datetime import datetime

LOGS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "logs")
LOG_FILE = os.path.join(LOGS_DIR, "server-manager.log")

_lock = threading.Lock()


class LogService:
    """Manages application logging to file and in-memory buffer."""

    def __init__(self):
        os.makedirs(LOGS_DIR, exist_ok=True)
        self._memory_logs: list[str] = []
        self._max_memory_logs = 1000

    def log(self, category: str, message: str):
        # Scrub any accidental secrets
        scrubbed = self._sanitize(message)
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        entry = f"[{timestamp}] [{category.upper()}] {scrubbed}"

        with _lock:
            self._memory_logs.append(entry)
            if len(self._memory_logs) > self._max_memory_logs:
                self._memory_logs.pop(0)

            try:
                with open(LOG_FILE, "a", encoding="utf-8") as f:
                    f.write(entry + "\n")
            except Exception:
                pass

    def get_recent_logs(self, limit: int = 100) -> list[str]:
        with _lock:
            return list(self._memory_logs[-limit:])

    @staticmethod
    def _sanitize(text: str) -> str:
        # Avoid logging tokens, keys, passwords
        sensitive_keywords = ["password", "secret", "bearer", "token", "master_key", "salt"]
        lowered = text.lower()
        for kw in sensitive_keywords:
            if kw in lowered and ("=" in text or ":" in text):
                # Don't suppress normal log statements like "Password encoder : BCrypt"
                if "bcrypt" not in lowered and "initialized" not in lowered and "fallback" not in lowered:
                    return "[FILTERED SENSITIVE LOG ENTRY]"
        return text


app_logger = LogService()
