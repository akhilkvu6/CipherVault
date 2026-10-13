import os
import shutil
import subprocess

class AdbDevice:
    def __init__(self, serial: str, state: str, is_emulator: bool):
        self.serial = serial
        self.state = state
        self.is_emulator = is_emulator
        self.reverse_active = False

    @property
    def display_name(self) -> str:
        dev_type = "Android Emulator" if self.is_emulator else "Physical Device"
        return f"{self.serial} ({dev_type})"


class AdbService:
    """Manages Android Debug Bridge detection and port forwarding."""

    _cached_adb_path = None

    @classmethod
    def find_adb(cls) -> str | None:
        if cls._cached_adb_path and os.path.isfile(cls._cached_adb_path):
            return cls._cached_adb_path

        # 1. Check system PATH
        path_in_env = shutil.which("adb")
        if path_in_env:
            cls._cached_adb_path = path_in_env
            return path_in_env

        # 2. Check Android SDK default locations
        local_app_data = os.environ.get("LOCALAPPDATA", "")
        candidates = [
            os.path.join(local_app_data, "Android", "Sdk", "platform-tools", "adb.exe"),
            os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb.exe"),
            os.path.join(os.environ.get("ANDROID_SDK_ROOT", ""), "platform-tools", "adb.exe"),
        ]

        for cand in candidates:
            if cand and os.path.isfile(cand):
                cls._cached_adb_path = cand
                return cand

        return None

    @classmethod
    def is_available(cls) -> bool:
        return cls.find_adb() is not None

    @classmethod
    def _run_cmd(cls, args: list[str], timeout: float = 6.0) -> tuple[bool, str]:
        adb = cls.find_adb()
        if not adb:
            return False, "ADB binary not found"

        full_cmd = [adb] + args
        try:
            res = subprocess.run(
                full_cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                creationflags=subprocess.CREATE_NO_WINDOW
            )
            return True, res.stdout + res.stderr
        except subprocess.TimeoutExpired:
            return False, "ADB command timed out"
        except Exception as e:
            return False, str(e)

    @classmethod
    def get_devices(cls) -> list[AdbDevice]:
        ok, out = cls._run_cmd(["devices"])
        if not ok:
            return []

        devices = []
        lines = out.strip().split("\n")[1:]  # Skip "List of devices attached"
        for line in lines:
            line = line.strip().replace("\r", "")
            if line and "\t" in line:
                serial, state = line.split("\t", 1)
                serial = serial.strip()
                state = state.strip()
                is_emu = serial.startswith("emulator-")
                dev = AdbDevice(serial, state, is_emu)
                devices.append(dev)

        # Check reverse status for connected devices
        for dev in devices:
            if dev.state == "device":
                dev.reverse_active = cls.is_reverse_active(dev.serial)

        return devices

    @classmethod
    def is_reverse_active(cls, serial: str | None = None) -> bool:
        args = ["-s", serial, "reverse", "--list"] if serial else ["reverse", "--list"]
        ok, out = cls._run_cmd(args)
        if not ok:
            return False
        return "tcp:8080 tcp:8080" in out

    @classmethod
    def enable_reverse(cls, serial: str | None = None) -> tuple[bool, str]:
        args = ["-s", serial, "reverse", "tcp:8080", "tcp:8080"] if serial else ["reverse", "tcp:8080", "tcp:8080"]
        ok, out = cls._run_cmd(args)
        if not ok:
            return False, out

        # Verify
        if cls.is_reverse_active(serial):
            return True, "Reverse port forwarding active (tcp:8080 -> tcp:8080)"
        return False, "ADB command completed but reverse mapping was not confirmed"

    @classmethod
    def disable_reverse(cls, serial: str | None = None) -> tuple[bool, str]:
        args = ["-s", serial, "reverse", "--remove", "tcp:8080"] if serial else ["reverse", "--remove", "tcp:8080"]
        ok, out = cls._run_cmd(args)
        if not ok:
            return False, out
        return True, "Reverse port forwarding removed"
