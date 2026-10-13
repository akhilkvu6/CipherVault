import json
import os

SETTINGS_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "settings.json")

DEFAULT_SETTINGS = {
    "theme": "system",
    "backend_port": 8080,
    "health_interval": 10,
    "auto_start_backend": True,
    "auto_enable_adb_reverse": False,
    "show_wifi": True,
    "show_ethernet": True,
    "show_adb": True,
    "show_emulator": True,
    "preferred_connection": "USB / ADB"
}


class SettingsManager:
    """Loads, saves, and provides application configuration."""

    def __init__(self):
        self._data = dict(DEFAULT_SETTINGS)
        self.load()

    def load(self):
        if os.path.exists(SETTINGS_FILE):
            try:
                with open(SETTINGS_FILE, "r", encoding="utf-8") as f:
                    loaded = json.load(f)
                    for k, v in loaded.items():
                        if k in DEFAULT_SETTINGS:
                            self._data[k] = v
            except Exception:
                pass

    def save(self):
        try:
            with open(SETTINGS_FILE, "w", encoding="utf-8") as f:
                json.dump(self._data, f, indent=2)
        except Exception:
            pass

    def get(self, key, default=None):
        return self._data.get(key, default)

    def set(self, key, value):
        self._data[key] = value
        self.save()

    @property
    def theme(self) -> str:
        return self._data.get("theme", "system")

    @property
    def backend_port(self) -> int:
        return self._data.get("backend_port", 8080)

    @property
    def health_interval(self) -> int:
        return self._data.get("health_interval", 10)

    @property
    def auto_start_backend(self) -> bool:
        return self._data.get("auto_start_backend", True)

    @property
    def auto_enable_adb_reverse(self) -> bool:
        return self._data.get("auto_enable_adb_reverse", False)

    @property
    def show_wifi(self) -> bool:
        return self._data.get("show_wifi", True)

    @property
    def show_ethernet(self) -> bool:
        return self._data.get("show_ethernet", True)

    @property
    def show_adb(self) -> bool:
        return self._data.get("show_adb", True)

    @property
    def show_emulator(self) -> bool:
        return self._data.get("show_emulator", True)

    @property
    def preferred_connection(self) -> str:
        return self._data.get("preferred_connection", "USB / ADB")


settings = SettingsManager()
